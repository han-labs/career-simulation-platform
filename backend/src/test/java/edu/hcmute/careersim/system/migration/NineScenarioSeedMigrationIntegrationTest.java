package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class NineScenarioSeedMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:0.8.2-pg17")
                            .asCompatibleSubstituteFor("postgres"));

    @Test
    void freshDatabaseContainsTheApprovedPublishedCatalogue() {
        Flyway flyway = flyway(PG.getJdbcUrl(), "32");

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(32);
        var db = jdbc(PG.getJdbcUrl());
        assertThat(db.queryForObject("SELECT count(*) FROM scenario", Integer.class)).isEqualTo(9);
        assertThat(
                        db.queryForObject(
                                "SELECT count(*) FROM scenario s JOIN scenario_version v ON v.id=s.current_version_id AND v.scenario_id=s.id WHERE s.availability_status='AVAILABLE' AND s.industry_code='IT' AND v.status='PUBLISHED'",
                                Integer.class))
                .isEqualTo(9);
        assertThat(db.queryForObject("SELECT count(*) FROM scenario_version", Integer.class))
                .isEqualTo(16);
        assertThat(db.queryForObject("SELECT count(*) FROM simulation_task", Integer.class))
                .isEqualTo(47);
        assertThat(
                        db.queryForObject(
                                "SELECT count(*) FROM simulation_task t JOIN scenario s ON s.current_version_id=t.scenario_version_id",
                                Integer.class))
                .isEqualTo(26);
        assertThat(
                        db.queryForObject(
                                "SELECT count(*) FROM task_deliverable_definition", Integer.class))
                .isEqualTo(47);
        assertThat(db.queryForObject("SELECT count(*) FROM career_skill", Integer.class))
                .isEqualTo(57);
        assertThat(db.queryForObject("SELECT count(*) FROM task_skill", Integer.class))
                .isEqualTo(139);
        assertThat(db.queryForObject("SELECT count(*) FROM model_answer", Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT count(*) FROM task_resource", Integer.class)).isZero();
        flyway.validate();
    }

    @Test
    void partialReferenceStateFailsClosedWithoutInstallingScenarios() throws Exception {
        PG.execInContainer("createdb", "-U", PG.getUsername(), "v32_conflict");
        String url =
                "jdbc:postgresql://"
                        + PG.getHost()
                        + ":"
                        + PG.getMappedPort(5432)
                        + "/v32_conflict";
        flyway(url, "31").migrate();
        var db = jdbc(url);
        db.update("INSERT INTO enterprise(name) VALUES ('Existing owner')");

        assertThatThrownBy(() -> flyway(url, "32").migrate())
                .hasMessageContaining("V32_REFERENCE_DATA_CONFLICT");
        assertThat(db.queryForObject("SELECT count(*) FROM scenario", Integer.class)).isZero();
    }

    private Flyway flyway(String url, String target) {
        return Flyway.configure()
                .dataSource(url, PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private JdbcTemplate jdbc(String url) {
        return new JdbcTemplate(
                new DriverManagerDataSource(url, PG.getUsername(), PG.getPassword()));
    }
}
