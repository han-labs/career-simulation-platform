package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class Uc05FilePolicyMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:0.8.2-pg17")
                            .asCompatibleSubstituteFor("postgres"));

    private static final Map<String, Map<Integer, String>> POLICIES =
            Map.of(
                    "hpe-software-engineering",
                            Map.of(2, "application/zip", 3, "application/zip", 4, "text/plain"),
                    "hpe-presales",
                            Map.of(
                                    1,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    3,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    4,
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                    "hpe-digital-sales",
                            Map.of(
                                    1,
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                    3,
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                    "datacom-software-development",
                            Map.of(2, "application/zip"),
                    "datacom-introduction-to-cloud",
                            Map.of(1, "application/pdf"),
                    "datacom-automation-ai-accelerator",
                            Map.of(2, "application/pdf", -2, "image/png"),
                    "datacom-cyber-security-operations",
                            Map.of(
                                    1,
                                    "application/pdf",
                                    2,
                                    "application/pdf",
                                    -2,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    @Test
    void freshDatabaseStartsWithoutThirdPartyContentAndExistingVersionsAreClonedOnce() throws Exception {
        Flyway fresh = flyway(PG.getJdbcUrl(), "28");
        assertThat(fresh.migrate().migrationsExecuted).isEqualTo(28);
        var clean = jdbc(PG.getJdbcUrl());
        assertThat(
                        clean.queryForObject(
                                "SELECT count(*) FROM scenario WHERE slug IN ("
                                        + placeholders(POLICIES.size())
                                        + ")",
                                Integer.class,
                                POLICIES.keySet().toArray()))
                .isZero();
        fresh.validate();

        PG.execInContainer(
                "createdb", "-U", PG.getUsername(), "uc05_upgrade");
        String upgradeUrl =
                "jdbc:postgresql://"
                        + PG.getHost()
                        + ":"
                        + PG.getMappedPort(5432)
                        + "/uc05_upgrade";
        flyway(upgradeUrl, "27").migrate();
        var db = jdbc(upgradeUrl);
        long enterprise =
                db.queryForObject(
                        "INSERT INTO enterprise(name,verification_status,publishing_status) VALUES ('Synthetic UC05 owner','VERIFIED','ENABLED') RETURNING id",
                        Long.class);
        Map<String, Long> oldVersions = new LinkedHashMap<>();
        for (var entry : POLICIES.entrySet())
            oldVersions.put(entry.getKey(), scenario(db, enterprise, entry.getKey(), entry.getValue()));

        long pinnedVersion = oldVersions.get("hpe-software-engineering");
        long enrollment =
                db.queryForObject(
                        "INSERT INTO simulation_enrollment(student_id,scenario_version_id,status,started_at) VALUES (1,?,'IN_PROGRESS',CURRENT_TIMESTAMP) RETURNING id",
                        Long.class,
                        pinnedVersion);
        long oldTask =
                db.queryForObject(
                        "SELECT id FROM simulation_task WHERE scenario_version_id=? AND position=1",
                        Long.class,
                        pinnedVersion);
        long submission =
                db.queryForObject(
                        "INSERT INTO task_submission(enrollment_id,task_id,scenario_version_id) VALUES (?,?,?) RETURNING id",
                        Long.class,
                        enrollment,
                        oldTask,
                        pinnedVersion);

        assertThat(flyway(upgradeUrl, "28").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway(upgradeUrl, "28").migrate().migrationsExecuted).isZero();
        flyway(upgradeUrl, "28").validate();

        assertThat(
                        db.queryForObject(
                                "SELECT count(*) FROM task_deliverable_definition d JOIN simulation_task t ON t.id=d.task_id JOIN scenario_version v ON v.id=t.scenario_version_id JOIN scenario s ON s.id=v.scenario_id WHERE v.id=s.current_version_id AND d.format='FILE' AND d.validation_config->>'schemaVersion'='1' AND (d.validation_config->>'maxBytes')::bigint=10485760 AND jsonb_array_length(d.validation_config->'allowedMimeTypes')>0",
                                Integer.class))
                .isEqualTo(13);
        assertThat(
                        db.queryForObject(
                                "SELECT count(*) FROM scenario_version v JOIN scenario s ON s.id=v.scenario_id WHERE s.slug IN ("
                                        + placeholders(POLICIES.size())
                                        + ")",
                                Integer.class,
                                POLICIES.keySet().toArray()))
                .isEqualTo(14);
        for (var old : oldVersions.entrySet()) {
            assertThat(
                            db.queryForObject(
                                    "SELECT status FROM scenario_version WHERE id=?",
                                    String.class,
                                    old.getValue()))
                    .isEqualTo("PUBLISHED");
            assertThat(
                            db.queryForObject(
                                    "SELECT current_version_id<>? FROM scenario WHERE slug=?",
                                    Boolean.class,
                                    old.getValue(),
                                    old.getKey()))
                    .isTrue();
        }
        assertThat(
                        db.queryForObject(
                                "SELECT scenario_version_id FROM simulation_enrollment WHERE id=?",
                                Long.class,
                                enrollment))
                .isEqualTo(pinnedVersion);
        assertThat(
                        db.queryForObject(
                                "SELECT count(*) FROM task_submission WHERE id=? AND enrollment_id=?",
                                Integer.class,
                                submission,
                                enrollment))
                .isOne();

        assertPolicy(db, "datacom-automation-ai-accelerator", 2, List.of("application/pdf", "image/png"));
        assertPolicy(
                db,
                "datacom-cyber-security-operations",
                2,
                List.of(
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private long scenario(
            JdbcTemplate db, long enterprise, String slug, Map<Integer, String> policies) {
        long root =
                db.queryForObject(
                        "INSERT INTO scenario(enterprise_id,slug,industry_code) VALUES (?,?,'IT') RETURNING id",
                        Long.class,
                        enterprise,
                        slug);
        long version =
                db.queryForObject(
                        "INSERT INTO scenario_version(scenario_id,version_number,title,target_role,career_track,summary,introduction,learning_objectives,difficulty,estimated_minutes,provenance) VALUES (?,1,?,'Developer','BACKEND_DEVELOPMENT','Synthetic summary','Synthetic introduction','Synthetic objective','INTRODUCTORY',10,'{\"source\":\"synthetic-test\"}') RETURNING id",
                        Long.class,
                        root,
                        "Synthetic " + slug);
        db.update(
                "INSERT INTO scenario_learning_policy VALUES (?,'ordered-review/1','ORDERED_SELF_REVIEW','1')",
                version);
        int maxPosition = policies.keySet().stream().mapToInt(Math::abs).max().orElse(2);
        maxPosition = Math.max(2, maxPosition);
        for (int position = 1; position <= maxPosition; position++) {
            long task =
                    db.queryForObject(
                            "INSERT INTO simulation_task(scenario_version_id,position,title,workplace_context,instructions,required) VALUES (?,?,?,'Synthetic context','Synthetic instructions',true) RETURNING id",
                            Long.class,
                            version,
                            position,
                            "Synthetic task " + position);
            boolean file = policies.containsKey(position) || policies.containsKey(-position);
            db.update(
                    "INSERT INTO task_deliverable_definition(task_id,position,name,format,required,validation_config) VALUES (?,1,'Synthetic response',?,true,?::jsonb)",
                    task,
                    file ? "FILE" : "TEXT",
                    file
                            ? "{}"
                            : "{\"schemaVersion\":1,\"minLength\":1,\"maxLength\":100}");
            db.update(
                    "INSERT INTO model_answer(task_id,content) VALUES (?,'Synthetic example answer')",
                    task);
            db.update("INSERT INTO task_learning_policy VALUES (?,'PRACTICAL')", task);
        }
        db.update(
                "UPDATE scenario_version SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP,content_hash=repeat('a',64) WHERE id=?",
                version);
        db.update(
                "UPDATE scenario SET current_version_id=?,availability_status='AVAILABLE' WHERE id=?",
                version,
                root);
        return version;
    }

    private void assertPolicy(JdbcTemplate db, String slug, int position, List<String> mimeTypes) {
        assertThat(
                        db.queryForList(
                                "SELECT jsonb_array_elements_text(d.validation_config->'allowedMimeTypes') FROM task_deliverable_definition d JOIN simulation_task t ON t.id=d.task_id JOIN scenario_version v ON v.id=t.scenario_version_id JOIN scenario s ON s.id=v.scenario_id WHERE s.slug=? AND s.current_version_id=v.id AND t.position=? ORDER BY 1",
                                String.class,
                                slug,
                                position))
                .containsExactlyInAnyOrderElementsOf(mimeTypes);
    }

    private Flyway flyway(String url, String target) {
        return Flyway.configure()
                .dataSource(url, PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private JdbcTemplate jdbc(String url) {
        return new JdbcTemplate(new DriverManagerDataSource(url, PG.getUsername(), PG.getPassword()));
    }

    private String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }
}
