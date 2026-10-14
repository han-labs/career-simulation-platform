package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

/** Required PG gate: Docker absence fails rather than reporting unexecuted DB checks as green. */
@Testcontainers
class SchemaUpgradeIntegrationTest {
    @Test
    void isolatedEmailCollisionStopsV13WithoutRepairingSource() throws Exception {
        try (var connection = PG.createConnection("");
                var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE package_a_collision");
        }
        String url =
                "jdbc:postgresql://"
                        + PG.getHost()
                        + ":"
                        + PG.getMappedPort(5432)
                        + "/package_a_collision";
        var legacy =
                Flyway.configure()
                        .dataSource(url, PG.getUsername(), PG.getPassword())
                        .locations("classpath:db/migration")
                        .target("12")
                        .load();
        legacy.migrate();
        try (var connection =
                        java.sql.DriverManager.getConnection(
                                url, PG.getUsername(), PG.getPassword());
                var statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO app_users(email,password_hash,display_name,role) VALUES (' STUDENT@DEMO.COM ','synthetic','Collision fixture','STUDENT')");
        }
        assertThatThrownBy(
                        () ->
                                Flyway.configure()
                                        .dataSource(url, PG.getUsername(), PG.getPassword())
                                        .locations("classpath:db/migration")
                                        .target("16")
                                        .load()
                                        .migrate())
                .hasStackTraceContaining("CORE_EMAIL_COLLISION");
        try (var connection =
                        java.sql.DriverManager.getConnection(
                                url, PG.getUsername(), PG.getPassword());
                var statement = connection.createStatement();
                var rows =
                        statement.executeQuery(
                                "SELECT (SELECT count(*) FROM app_users WHERE lower(btrim(email))='student@demo.com'),to_regclass('public.enterprise')")) {
            rows.next();
            assertThat(rows.getInt(1)).isEqualTo(2);
            assertThat(rows.getObject(2)).isNull();
        }
    }

    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:0.8.2-pg17")
                            .asCompatibleSubstituteFor("postgres"));

    @Test
    void populatedV12UpgradePreservesLegacyAndInstallsExactlyPackageA() throws Exception {
        flyway("12").migrate();
        var before = fingerprints();
        flyway("16").migrate();
        assertThat(fingerprints()).isEqualTo(before);
        assertThat(flyway("16").info().current().getVersion().toString()).isEqualTo("16");
        assertThat(flyway("16").migrate().migrationsExecuted).isZero();
        try (var c = PG.createConnection("");
                var s = c.createStatement()) {
            try (var r =
                    s.executeQuery(
                            "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history'")) {
                r.next();
                assertThat(r.getInt(1)).isEqualTo(46);
            }
            try (var r =
                    s.executeQuery(
                            "SELECT (SELECT count(*) FROM public.consent_grant)+(SELECT count(*) FROM public.simulation_completion)")) {
                r.next();
                assertThat(r.getInt(1)).isZero();
            }
            assertThatThrownBy(
                            () ->
                                    s.executeUpdate(
                                            "INSERT INTO public.app_users(email,password_hash,display_name,role) VALUES (' STUDENT@DEMO.COM ','fixture','Fixture','STUDENT')"))
                    .isInstanceOf(SQLException.class);
        }
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private Map<String, String> fingerprints() throws Exception {
        Map<String, String> result = new TreeMap<>();
        List<String> legacy =
                List.of(
                        "app_users",
                        "student_profiles",
                        "riasec_questions",
                        "assessment_attempts",
                        "assessment_answers",
                        "assessment_scores",
                        "career_simulations",
                        "simulation_tasks",
                        "simulation_attempts",
                        "task_submissions",
                        "evaluation_results",
                        "guidance_reports",
                        "exploration_plans",
                        "syn_sessions",
                        "career_path_profiles",
                        "learning_resources",
                        "simulation_task_skills",
                        "syn_turns",
                        "knowledge_documents",
                        "knowledge_versions",
                        "knowledge_chunks");
        try (var c = PG.createConnection("");
                var s = c.createStatement()) {
            for (String t : legacy) {
                String expression = "to_jsonb(t)";
                if (t.equals("app_users")) expression += "-'row_version'-'privacy_version'";
                if (t.equals("student_profiles"))
                    expression += "-'major'-'career_goal'-'declared_interests'-'row_version'";
                try (var r =
                        s.executeQuery(
                                "SELECT md5(COALESCE(string_agg(data,E'\\n' ORDER BY data),'')) FROM (SELECT ("
                                        + expression
                                        + ")::text AS data FROM public."
                                        + t
                                        + " t) x")) {
                    r.next();
                    result.put(t, r.getString(1));
                }
            }
        }
        return result;
    }
}
