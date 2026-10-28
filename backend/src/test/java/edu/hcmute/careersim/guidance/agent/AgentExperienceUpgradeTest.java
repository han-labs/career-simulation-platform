package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class AgentExperienceUpgradeTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("pgvector/pgvector:0.8.2-pg17");

    @Test
    void additiveUpgradeKeepsSealedV1BytesHashesAndPublicationProof() {
        Flyway.configure()
                .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                .target("28")
                .load()
                .migrate();
        var db =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                pg.getJdbcUrl(), pg.getUsername(), pg.getPassword()));
        long actor =
                db.queryForObject(
                        "INSERT INTO app_users(email,display_name,role,status) VALUES ('upgrade@example.test','Synthetic reviewer','ADMINISTRATOR','ACTIVE') RETURNING id",
                        Long.class);
        UUID agent = UUID.randomUUID(), config = UUID.randomUUID(), execution = UUID.randomUUID();
        String hash = "a".repeat(64);
        db.update(
                "INSERT INTO agent(id,name,purpose,created_by_admin_id) VALUES (?,'Legacy guide','Legacy purpose',?)",
                agent,
                actor);
        db.update(
                "INSERT INTO agent_configuration(id,agent_id,version_number,model_key,prompt_template,prompt_template_ref,prompt_hash,tool_policy,memory_policy,output_policy,created_by_admin_id,content_hash) VALUES (?,?,1,'gemini/synthetic','Literal legacy {{name}}','legacy-v1',?,'{}','{}','{}',?,?)",
                config,
                agent,
                hash,
                actor,
                hash);
        db.update("UPDATE agent_configuration SET sealed=true WHERE id=?", config);
        db.update(
                "INSERT INTO agent_execution(id,execution_mode,task_type,initiated_by_user_id,configuration_id,input_snapshot,request_id,request_hash,status,model_key,prompt_hash,configuration_hash,privacy_version) VALUES (?,'ADMIN_PREVIEW','CHAT',?,?,'{}',?,?,'ACCEPTED','gemini/synthetic',?,?,0)",
                execution,
                actor,
                config,
                UUID.randomUUID(),
                hash,
                hash,
                hash);
        db.update(
                "UPDATE agent_execution SET status='SUCCEEDED',output_snapshot='{}',finished_at=CURRENT_TIMESTAMP WHERE id=?",
                execution);
        db.update(
                "UPDATE agent_configuration SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP WHERE id=?",
                config);
        db.update(
                "UPDATE agent SET published_configuration_id=?,status='ACTIVE' WHERE id=?",
                config,
                agent);
        long oldStudent =
                db.queryForObject(
                        "INSERT INTO app_users(email,display_name,role,status) VALUES ('upgrade-student@example.test','Synthetic Student','STUDENT','ACTIVE') RETURNING id",
                        Long.class);
        db.update("INSERT INTO student_profiles(user_id) VALUES (?)", oldStudent);
        UUID oldAssignment = UUID.randomUUID(), oldSession = UUID.randomUUID();
        db.update(
                "INSERT INTO agent_assignment(id,agent_id,student_user_id,assigned_by_admin_user_id) VALUES (?,?,?,?)",
                oldAssignment,
                agent,
                oldStudent,
                actor);
        db.update(
                "INSERT INTO agent_session(id,owner_user_id,agent_id,configuration_id,title) VALUES (?,?,?,?,'Historical pinned chat')",
                oldSession,
                oldStudent,
                agent,
                config);
        String assignmentBefore =
                db.queryForObject(
                        "SELECT to_jsonb(x)::text FROM agent_assignment x WHERE id=?",
                        String.class,
                        oldAssignment);
        String sessionBefore =
                db.queryForObject(
                        "SELECT to_jsonb(x)::text FROM agent_session x WHERE id=?",
                        String.class,
                        oldSession);
        String before =
                db.queryForObject(
                        "SELECT (to_jsonb(c)-'experience_config')::text FROM agent_configuration c WHERE id=?",
                        String.class,
                        config);
        var oldHistory =
                db.queryForList(
                        "SELECT version,checksum FROM flyway_schema_history ORDER BY installed_rank");
        var flyway =
                Flyway.configure()
                        .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                        .load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(5);
        flyway.validate();
        assertThat(
                        db.queryForObject(
                                "SELECT to_jsonb(x)::text FROM agent_assignment x WHERE id=?",
                                String.class,
                                oldAssignment))
                .isEqualTo(assignmentBefore);
        assertThat(
                        db.queryForObject(
                                "SELECT to_jsonb(x)::text FROM agent_session x WHERE id=?",
                                String.class,
                                oldSession))
                .isEqualTo(sessionBefore);
        assertThat(
                        db.queryForObject(
                                "SELECT count(*) FROM agent_role_binding WHERE agent_id IS NULL",
                                Integer.class))
                .isEqualTo(2);

        assertThat(
                        db.queryForObject(
                                "SELECT (to_jsonb(c)-'experience_config')::text FROM agent_configuration c WHERE id=?",
                                String.class,
                                config))
                .isEqualTo(before);
        assertThat(
                        db.queryForObject(
                                "SELECT experience_config IS NULL FROM agent_configuration WHERE id=?",
                                Boolean.class,
                                config))
                .isTrue();
        assertThat(
                        db.queryForList(
                                "SELECT version,checksum FROM flyway_schema_history WHERE installed_rank<=28 ORDER BY installed_rank"))
                .isEqualTo(oldHistory);
        assertThat(
                        db.queryForObject(
                                "SELECT count(*) FROM agent_execution WHERE configuration_id=? AND status='SUCCEEDED' AND configuration_hash=?",
                                Integer.class,
                                config,
                                hash))
                .isEqualTo(1);
        assertThat(
                        db.queryForObject(
                                "SELECT published_configuration_id FROM agent WHERE id=?",
                                UUID.class,
                                agent))
                .isEqualTo(config);
    }
}
