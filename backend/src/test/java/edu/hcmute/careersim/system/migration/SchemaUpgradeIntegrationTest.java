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
    void isolatedEmailCollisionStopsV15WithoutRepairingSource() throws Exception {
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
                        .target("14")
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
                                        .target("18")
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
    void populatedV14UpgradePreservesLegacyAndInstallsExactlyPackageA() throws Exception {
        flyway("14").migrate();
        seedLegacyEvidence();
        var before = fingerprints();
        var applied = flyway("14").info().applied();
        assertThat(applied).hasSize(14);
        assertThat(flyway("18").migrate().migrationsExecuted).isEqualTo(4);
        assertThat(fingerprints()).isEqualTo(before);
        assertThat(
                        Arrays.stream(flyway("18").info().applied())
                                .limit(applied.length)
                                .map(
                                        migration ->
                                                List.of(
                                                        migration.getVersion(),
                                                        migration.getScript(),
                                                        migration.getChecksum()))
                                .toList())
                .containsExactlyElementsOf(
                        Arrays.stream(applied)
                                .map(
                                        migration ->
                                                List.of(
                                                        migration.getVersion(),
                                                        migration.getScript(),
                                                        migration.getChecksum()))
                                .toList());
        assertThat(flyway("18").info().current().getVersion().toString()).isEqualTo("18");
        assertThat(flyway("18").migrate().migrationsExecuted).isZero();
        try (var c = PG.createConnection("");
                var s = c.createStatement()) {
            try (var r =
                    s.executeQuery(
                            "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history'")) {
                r.next();
                assertThat(r.getInt(1)).isEqualTo(49);
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

    private void seedLegacyEvidence() throws Exception {
        try (var connection = PG.createConnection("");
                var statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    INSERT INTO knowledge_documents(source_key,title,license,status)
                    VALUES ('synthetic-upgrade','Synthetic upgrade source','Original test fixture','PUBLISHED');
                    INSERT INTO knowledge_versions(document_id,content_hash,language,track,reviewer,embedding_profile)
                    SELECT id,repeat('a',64),'vi','BACKEND_DEVELOPMENT','Synthetic reviewer','synthetic-768'
                    FROM knowledge_documents WHERE source_key='synthetic-upgrade';
                    UPDATE knowledge_documents SET active_version_id=(SELECT id FROM knowledge_versions
                    WHERE document_id=knowledge_documents.id) WHERE source_key='synthetic-upgrade';
                    INSERT INTO knowledge_chunks(version_id,ordinal,section,content,search_text,embedding)
                    SELECT id,1,'Synthetic section','Preserve original reviewed text',
                    to_tsvector('simple','Preserve original reviewed text'),array_fill(0.25::real,ARRAY[768])::vector
                    FROM knowledge_versions WHERE embedding_profile='synthetic-768';
                    INSERT INTO syn_sessions(id,student_id,summary)
                    VALUES ('11111111-1111-1111-1111-111111111111',1,'Synthetic bounded summary');
                    INSERT INTO syn_turns(id,student_id,request_id,request_hash,session_id,session_version,state,response,trace)
                    VALUES ('22222222-2222-2222-2222-222222222222',1,
                    '33333333-3333-3333-3333-333333333333',repeat('b',64),
                    '11111111-1111-1111-1111-111111111111',0,'COMPLETED',
                    '{"text":"Synthetic preserved response"}','{"fixture":"merged-main-v14"}');
                    INSERT INTO assessment_attempts(student_id,status,assessment_type,completed_at)
                    VALUES (1,'COMPLETED','INTERESTS',CURRENT_TIMESTAMP);
                    INSERT INTO career_quiz_answers(attempt_id,question_id,score)
                    SELECT a.id,q.id,3 FROM assessment_attempts a CROSS JOIN career_quiz_questions q
                    WHERE a.assessment_type='INTERESTS' AND q.quiz_type='INTERESTS';
                    INSERT INTO career_quiz_scores(attempt_id,category,score)
                    SELECT a.attempt_id,q.category,sum(a.score) FROM career_quiz_answers a
                    JOIN career_quiz_questions q ON q.id=a.question_id GROUP BY a.attempt_id,q.category;
                    """);
        }
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
                        "career_quiz_questions",
                        "career_quiz_answers",
                        "career_quiz_scores",
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
