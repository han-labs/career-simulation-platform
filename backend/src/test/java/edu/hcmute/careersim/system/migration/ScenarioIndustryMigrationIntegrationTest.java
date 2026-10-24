package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.simulation.dao.*;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ScenarioIndustryMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:0.8.2-pg17")
                            .asCompatibleSubstituteFor("postgres"));

    @Test
    void v23PreservesRowsConstrainsClassificationAndFiltersBeforePagination() {
        var inspection = PG.getDockerClient().inspectContainerCmd(PG.getContainerId()).exec();
        System.out.printf(
                "ISOLATED_TESTCONTAINER id=%s name=%s mounts=%s preservation=synthetic-disposable-no-backup%n",
                PG.getContainerId(), inspection.getName(), inspection.getMounts());
        var flyway =
                Flyway.configure()
                        .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                        .locations("classpath:db/migration")
                        .target("22")
                        .load();
        flyway.migrate();
        var jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        long enterprise =
                jdbc.queryForObject(
                        "INSERT INTO enterprise(name) VALUES ('Synthetic catalogue owner') RETURNING id",
                        Long.class);
        long legacy = scenario(jdbc, enterprise, "legacy-null", "Legacy null", "AVAILABLE");
        Flyway.configure()
                .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration")
                .target("23")
                .load()
                .migrate();

        assertThat(
                        jdbc.queryForObject(
                                "SELECT industry_code FROM scenario WHERE id=?",
                                String.class,
                                legacy))
                .isNull();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE scenario SET industry_code='FINANCE' WHERE id=?",
                                        legacy))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        jdbc.update("UPDATE scenario SET industry_code='IT' WHERE id=?", legacy);
        scenario(jdbc, enterprise, "other-null", "Other null", "AVAILABLE");
        long secondIt = scenario(jdbc, enterprise, "second-it", "Second IT", "AVAILABLE");
        jdbc.update("UPDATE scenario SET industry_code='IT' WHERE id=?", secondIt);
        long archived = scenario(jdbc, enterprise, "archived-it", "Archived IT", "ARCHIVED");
        jdbc.update("UPDATE scenario SET industry_code='IT' WHERE id=?", archived);

        var dao = new ScenarioDao(jdbc, new LearningPolicyDao(jdbc));
        var reads = new ScenarioReadDao(jdbc, dao);
        var unfiltered =
                reads.search(new CatalogQuery(null, null, null, "ID_ASC", null, 1, ""));
        assertThat(unfiltered.items()).hasSize(1);
        assertThat(unfiltered.nextCursor()).isNotBlank();
        assertThat(
                        reads.search(
                                        new CatalogQuery(
                                                null,
                                                null,
                                                null,
                                                "ID_ASC",
                                                unfiltered.nextCursor(),
                                                10,
                                                ""))
                                .items())
                .isNotEmpty();

        var filtered = reads.search(new CatalogQuery(null, null, null, "ID_ASC", null, 10, "IT"));
        assertThat(filtered.items())
                .extracting(s -> List.of(s.slug(), s.industryCode()))
                .containsExactly(
                        List.of("legacy-null", "IT"), List.of("second-it", "IT"));
        assertThat(dao.catalog(null, 10, "IT"))
                .extracting(s -> List.of(s.slug(), s.industryCode()))
                .containsExactly(
                        List.of("legacy-null", "IT"), List.of("second-it", "IT"));
        assertThatThrownBy(
                        () ->
                                reads.search(
                                        new CatalogQuery(
                                                null,
                                                null,
                                                null,
                                                "ID_ASC",
                                                unfiltered.nextCursor(),
                                                10,
                                                "IT")))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        var industryCursor =
                reads.search(new CatalogQuery(null, null, null, "ID_ASC", null, 1, "IT"))
                        .nextCursor();
        assertThat(industryCursor).isNotBlank();
        assertThatThrownBy(
                        () ->
                                reads.search(
                                        new CatalogQuery(
                                                null,
                                                null,
                                                null,
                                                "ID_ASC",
                                                industryCursor,
                                                10,
                                                "")))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    private long scenario(
            JdbcTemplate jdbc, long enterprise, String slug, String title, String availability) {
        long root =
                jdbc.queryForObject(
                        "INSERT INTO scenario(enterprise_id,slug) VALUES (?,?) RETURNING id",
                        Long.class,
                        enterprise,
                        slug);
        long version =
                jdbc.queryForObject(
                        "INSERT INTO scenario_version(scenario_id,version_number,status,title,target_role,career_track,summary,introduction,learning_objectives,difficulty,estimated_minutes,provenance,content_hash,published_at) VALUES (?,1,'PUBLISHED',?,'Developer','BACKEND_DEVELOPMENT','Synthetic summary','Synthetic introduction','Practice','INTRODUCTORY',10,'{}',repeat('a',64),CURRENT_TIMESTAMP) RETURNING id",
                        Long.class,
                        root,
                        title);
        jdbc.update(
                "UPDATE scenario SET current_version_id=?,availability_status=? WHERE id=?",
                version,
                availability,
                root);
        return root;
    }
}
