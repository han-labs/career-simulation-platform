package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class StudentAgentUpgradeIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:0.8.2-pg17");

    Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    @Test
    void populatedV21UpgradePreservesAllRowsAndChecksumsWithoutInventingReportsConsentsOrPlans() {
        flyway("21").migrate();
        var jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        long admin =
                jdbc.queryForObject(
                        "INSERT INTO app_users(email,password_hash,display_name,role) VALUES ('synthetic-upgrade-admin@example.test','synthetic','Reviewer','ADMINISTRATOR') RETURNING id",
                        Long.class);
        long student =
                jdbc.queryForObject(
                        "INSERT INTO app_users(email,password_hash,display_name,role) VALUES ('synthetic-upgrade-student@example.test','synthetic','Student','STUDENT') RETURNING id",
                        Long.class);
        jdbc.update("INSERT INTO student_profiles(user_id) VALUES (?)", student);
        UUID agent = UUID.randomUUID(),
                config = UUID.randomUUID(),
                preview = UUID.randomUUID(),
                session = UUID.randomUUID(),
                message = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO agent(id,name,purpose,created_by_admin_id) VALUES (?,'Historical synthetic Agent','Fixture',?)",
                agent,
                admin);
        jdbc.update(
                "INSERT INTO agent_configuration(id,agent_id,version_number,model_key,prompt_template,prompt_template_ref,prompt_hash,tool_policy,memory_policy,output_policy,created_by_admin_id,content_hash,sealed) VALUES (?,?,1,'disabled/synthetic','Original prompt','historical-template',repeat('a',64),'{}','{}','{}',?,repeat('b',64),true)",
                config,
                agent,
                admin);
        jdbc.update(
                "INSERT INTO agent_execution(id,execution_mode,task_type,initiated_by_user_id,configuration_id,input_snapshot,request_id,request_hash,status,configuration_hash,privacy_version,output_snapshot,finished_at) VALUES (?,'ADMIN_PREVIEW','CHAT',?,?,'{}',?,repeat('c',64),'SUCCEEDED',repeat('b',64),0,'{\"text\":\"Synthetic historical preview\"}',CURRENT_TIMESTAMP)",
                preview,
                admin,
                config,
                UUID.randomUUID());
        jdbc.update(
                "UPDATE agent_configuration SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP WHERE id=?",
                config);
        jdbc.update(
                "UPDATE agent SET status='ACTIVE',published_configuration_id=? WHERE id=?",
                config,
                agent);
        jdbc.update(
                "INSERT INTO agent_assignment(id,agent_id,student_user_id,assigned_by_admin_user_id) VALUES (?,?,?,?)",
                UUID.randomUUID(),
                agent,
                student,
                admin);
        jdbc.update(
                "INSERT INTO agent_session(id,owner_user_id,agent_id,configuration_id,title,next_sequence) VALUES (?,?,?,?,'Historical conversation',2)",
                session,
                student,
                agent,
                config);
        jdbc.update(
                "INSERT INTO interaction_record(id,session_id,owner_user_id,sequence_number,actor_type,content) VALUES (?,?,?,1,'USER','{\"text\":\"Original synthetic narrative\"}')",
                message,
                session,
                student);
        jdbc.update(
                "INSERT INTO exploration_plan(student_id,title,steps,confirmed_at) VALUES (?,'Original manually confirmed plan','[\"Synthetic confirmed step\"]',CURRENT_TIMESTAMP)",
                student);
        jdbc.update(
                "INSERT INTO consent_grant(user_id,purpose,recipient_key,policy_version,confirmed_at,confirmation_method,revoked_at) VALUES (?,'EXTERNAL_AI','provider:gemini','careersim-purpose-v1',CURRENT_TIMESTAMP,'EXPLICIT_UI',CURRENT_TIMESTAMP)",
                student);
        var tables =
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history' ORDER BY table_name",
                        String.class);
        var before = fingerprints(jdbc, tables);
        var checksums =
                Arrays.stream(flyway("21").info().applied())
                        .map(m -> List.of(m.getVersion(), m.getScript(), m.getChecksum()))
                        .toList();
        assertThat(flyway("22").migrate().migrationsExecuted).isEqualTo(1);
        flyway("22").validate();
        assertThat(flyway("22").migrate().migrationsExecuted).isZero();
        assertThat(fingerprints(jdbc, tables)).isEqualTo(before);
        assertThat(
                        Arrays.stream(flyway("22").info().applied())
                                .limit(21)
                                .map(m -> List.of(m.getVersion(), m.getScript(), m.getChecksum()))
                                .toList())
                .isEqualTo(checksums);
        for (String table :
                List.of(
                        "insight_report",
                        "agent_execution_context_source",
                        "agent_execution_event"))
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class))
                    .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT source_execution_id IS NULL AND confirmation_hash IS NULL AND confirmation_request_id IS NULL FROM exploration_plan WHERE student_id=?",
                                Boolean.class,
                                student))
                .isTrue();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE interaction_record SET content='{}' WHERE id=?",
                                        message))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM agent_session WHERE id=?", session))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    Map<String, String> fingerprints(JdbcTemplate jdbc, List<String> tables) {
        var values = new TreeMap<String, String>();
        for (String table : tables) {
            String excluded =
                    switch (table) {
                        case "agent_session" -> "-'creation_request_id'";
                        case "exploration_plan" ->
                                "-'source_execution_id'-'confirmation_request_id'-'confirmation_hash'";
                        default -> "";
                    };
            values.put(
                    table,
                    jdbc.queryForObject(
                            "SELECT md5(COALESCE(string_agg(data,E'\\n' ORDER BY data),'')) FROM (SELECT (to_jsonb(t)"
                                    + excluded
                                    + ")::text AS data FROM public."
                                    + table
                                    + " t) q",
                            String.class));
        }
        return values;
    }
}
