package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.simulation.dao.LearningPolicyDao;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class SecurityCoreUpgradeIntegrationTest {
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
    void populatedV18UpgradePreservesAllPriorRowsHashesAndHistoricalPolicy() throws Exception {
        flyway("18").migrate();
        var jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        long enterprise =
                jdbc.queryForObject(
                        "INSERT INTO enterprise(name) VALUES ('Synthetic historical organization') RETURNING id",
                        Long.class);
        long scenario =
                jdbc.queryForObject(
                        "INSERT INTO scenario(enterprise_id,slug) VALUES (?,'historical-policy-fixture') RETURNING id",
                        Long.class,
                        enterprise);
        long version =
                jdbc.queryForObject(
                        "INSERT INTO scenario_version(scenario_id,version_number,title,target_role,career_track,summary,introduction,learning_objectives,difficulty,estimated_minutes,provenance) VALUES (?,1,'Historical synthetic content','Developer','BACKEND_DEVELOPMENT','Fixture','Fixture','Practice','INTRODUCTORY',10,'{}') RETURNING id",
                        Long.class,
                        scenario);
        long required =
                jdbc.queryForObject(
                        "INSERT INTO simulation_task(scenario_version_id,position,title,workplace_context,instructions,required) VALUES (?,1,'Required','Fixture','Fixture',true) RETURNING id",
                        Long.class,
                        version);
        long optional =
                jdbc.queryForObject(
                        "INSERT INTO simulation_task(scenario_version_id,position,title,workplace_context,instructions,required) VALUES (?,2,'Optional','Fixture','Fixture',false) RETURNING id",
                        Long.class,
                        version);
        jdbc.update(
                "INSERT INTO task_deliverable_definition(task_id,position,name,format,required,validation_config) VALUES (?,1,'Response','TEXT',true,'{\"schemaVersion\":1,\"minLength\":1,\"maxLength\":100}')",
                optional);
        jdbc.update(
                "INSERT INTO model_answer(task_id,content) VALUES (?,'Original historical answer')",
                required);
        jdbc.update(
                "UPDATE scenario_version SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP,content_hash=repeat('a',64) WHERE id=?",
                version);
        jdbc.update(
                "UPDATE scenario SET current_version_id=?,availability_status='AVAILABLE' WHERE id=?",
                version,
                scenario);
        long enrollment =
                jdbc.queryForObject(
                        "INSERT INTO simulation_enrollment(student_id,scenario_version_id,status,started_at,completed_at) VALUES (1,?,'COMPLETED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING id",
                        Long.class,
                        version);
        long submission =
                jdbc.queryForObject(
                        "INSERT INTO task_submission(enrollment_id,task_id,scenario_version_id,status,final_request_id,final_payload_hash,submitted_at) VALUES (?,?,?,'FINAL',?,repeat('b',64),CURRENT_TIMESTAMP) RETURNING id",
                        Long.class,
                        enrollment,
                        required,
                        version,
                        UUID.randomUUID());
        jdbc.update(
                "INSERT INTO simulation_completion(enrollment_id,completed_at,resume_snippet_text,snippet_template_version,certificate_code,certificate_snapshot,certificate_issued_at) VALUES (?,CURRENT_TIMESTAMP,'Original historical snippet','historical/1','HISTORY-FIXTURE','{\"fixture\":true}',CURRENT_TIMESTAMP)",
                enrollment);
        jdbc.update(
                "INSERT INTO consent_grant(user_id,purpose,recipient_key,policy_version,confirmed_at,confirmation_method,revoked_at) VALUES (1,'EXTERNAL_AI','provider:gemini','careersim-purpose-v1',CURRENT_TIMESTAMP,'EXPLICIT_UI',CURRENT_TIMESTAMP)");
        List<String> tables =
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history' ORDER BY table_name",
                        String.class);
        assertThat(tables).hasSize(49);
        var before = fingerprints(jdbc, tables);
        var old =
                Arrays.stream(flyway("18").info().applied())
                        .map(m -> List.of(m.getVersion(), m.getScript(), m.getChecksum()))
                        .toList();
        assertThat(flyway("20").migrate().migrationsExecuted).isEqualTo(2);
        assertThat(fingerprints(jdbc, tables)).isEqualTo(before);
        assertThat(
                        Arrays.stream(flyway("20").info().applied())
                                .limit(18)
                                .map(m -> List.of(m.getVersion(), m.getScript(), m.getChecksum()))
                                .toList())
                .isEqualTo(old);
        assertThat(flyway("20").migrate().migrationsExecuted).isZero();
        flyway("20").validate();
        var learning = new LearningPolicyDao(jdbc);
        assertThat(learning.ordered(version)).isFalse();
        assertThat(learning.progress(enrollment, version))
                .allMatch(p -> p.accessible() && !p.reviewed());
        assertThat(learning.actor(submission)).isNull();
        assertThat(learning.score(submission)).isNull();
        assertThat(learning.reviewed(submission)).isNull();
        for (String table :
                List.of(
                        "external_identity",
                        "auth_token_revocation",
                        "scenario_learning_policy",
                        "task_learning_policy",
                        "task_rubric",
                        "task_quiz_result",
                        "task_self_review",
                        "task_submission_actor"))
            assertThat(jdbc.queryForObject("SELECT count(*) FROM public." + table, Integer.class))
                    .as(table + " is not backfilled")
                    .isZero();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO scenario_learning_policy VALUES (?,'ordered-review/1','ORDERED_SELF_REVIEW','1')",
                                        version))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE model_answer SET content='changed' WHERE task_id=?",
                                        required))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT password_hash IS NOT NULL FROM app_users WHERE id=1",
                                Boolean.class))
                .isTrue();
        // The upgraded application must still allow a historical optional task after completion,
        // with no invented predecessor review and without changing the fixed old artifact.
        try (var context =
                new org.springframework.boot.builder.SpringApplicationBuilder(
                                edu.hcmute.careersim.CareerSimulationApplication.class)
                        .run(
                                "--server.port=0",
                                "--server.address=127.0.0.1",
                                "--spring.datasource.url=" + PG.getJdbcUrl(),
                                "--spring.datasource.username=" + PG.getUsername(),
                                "--spring.datasource.password=" + PG.getPassword(),
                                "--spring.flyway.url=" + PG.getJdbcUrl(),
                                "--spring.flyway.user=" + PG.getUsername(),
                                "--spring.flyway.password=" + PG.getPassword(),
                                "--app.core-v2-enabled=true",
                                "--app.legacy-write-enabled=false",
                                "--app.auth.jwt.signing-key=",
                                "--app.auth.jwt.allow-ephemeral-key=true",
                                "--app.auth.google.enabled=false",
                                "--app.ai.provider=disabled",
                                "--app.syn.cleanup-enabled=false")) {
            var service =
                    context.getBean(
                            edu.hcmute.careersim.simulation.service.TaskSubmissionService.class);
            var enrollmentService =
                    context.getBean(
                            edu.hcmute.careersim.simulation.service.EnrollmentService.class);
            var historical = enrollmentService.read(1, enrollment);
            assertThat(historical.progressionMode()).isEqualTo("LEGACY");
            var task =
                    historical.tasks().stream()
                            .filter(t -> t.id().equals(Long.toString(optional)))
                            .findFirst()
                            .orElseThrow();
            var finalized =
                    service.finalizeTask(
                            1,
                            enrollment,
                            optional,
                            0,
                            new edu.hcmute.careersim.simulation.dto.ScenarioContracts.Finalize(
                                    UUID.randomUUID(),
                                    List.of(
                                            new edu.hcmute.careersim.simulation.dto
                                                    .ScenarioContracts.Item(
                                                    task.definitions().get(0).id(),
                                                    "New actual optional work",
                                                    null,
                                                    null,
                                                    null))));
            assertThat(finalized.status()).isEqualTo("FINAL");
            assertThat(finalized.reviewedAt()).isNull();
            assertThat(finalized.quizResult()).isNull();
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT resume_snippet_text FROM simulation_completion WHERE enrollment_id=?",
                                    String.class,
                                    enrollment))
                    .isEqualTo("Original historical snippet");
            assertThat(learning.reviewed(submission)).isNull();
            assertThat(learning.actor(submission)).isNull();
        }
    }

    private Map<String, String> fingerprints(JdbcTemplate jdbc, List<String> tables) {
        var result = new TreeMap<String, String>();
        for (String table : tables) {
            String excluded =
                    table.equals("app_users")
                            ? "-'auth_version'"
                            : table.equals("enterprise") ? "-'description'" : "";
            result.put(
                    table,
                    jdbc.queryForObject(
                            "SELECT md5(COALESCE(string_agg(data,E'\\n' ORDER BY data),'')) FROM (SELECT (to_jsonb(t)"
                                    + excluded
                                    + ")::text AS data FROM public."
                                    + table
                                    + " t) q",
                            String.class));
        }
        return result;
    }
}
