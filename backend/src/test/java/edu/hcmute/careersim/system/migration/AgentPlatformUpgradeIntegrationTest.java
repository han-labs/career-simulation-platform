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
class AgentPlatformUpgradeIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:0.8.2-pg17");

    Flyway flyway(String version) {
        return Flyway.configure()
                .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration")
                .target(version)
                .load();
    }

    @Test
    void populatedV20UpgradePreservesEveryHistoricalRowAndChecksumAndFailedDdlRollsBack() {
        flyway("20").migrate();
        var jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        long d1 =
                jdbc.queryForObject(
                        "INSERT INTO knowledge_documents(source_key,title,license,status) VALUES ('legacy-one','Original reviewed source','Synthetic fixture','REVOKED') RETURNING id",
                        Long.class);
        long d2 =
                jdbc.queryForObject(
                        "INSERT INTO knowledge_documents(source_key,title,license,status) VALUES ('legacy-two','Other reviewed source','Synthetic fixture','REVOKED') RETURNING id",
                        Long.class);
        long v1 =
                jdbc.queryForObject(
                        "INSERT INTO knowledge_versions(document_id,content_hash,language,track,reviewer,embedding_profile) VALUES (?,repeat('a',64),'VI','BACKEND','Original reviewer','historical:768') RETURNING id",
                        Long.class,
                        d1);
        double[] vector = new double[768];
        vector[0] = 1;
        jdbc.update(
                "INSERT INTO knowledge_chunks(version_id,ordinal,section,content,search_text,embedding) VALUES (?,0,'Original section','Original historical source',to_tsvector('simple','historical'),CAST(? AS vector))",
                v1,
                Arrays.toString(vector));
        jdbc.update(
                "UPDATE knowledge_documents SET active_version_id=?,status='PUBLISHED' WHERE id=?",
                v1,
                d1);
        jdbc.update(
                "INSERT INTO consent_grant(user_id,purpose,recipient_key,policy_version,confirmed_at,confirmation_method,revoked_at) VALUES (1,'EXTERNAL_AI','provider:gemini','careersim-purpose-v1',CURRENT_TIMESTAMP,'EXPLICIT_UI',CURRENT_TIMESTAMP)");
        var tables =
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history' ORDER BY table_name",
                        String.class);
        var before = fingerprints(jdbc, tables);
        var old =
                Arrays.stream(flyway("20").info().applied())
                        .map(m -> List.of(m.getVersion(), m.getScript(), m.getChecksum()))
                        .toList();
        // Invalid historical cross-document pointer must block an upgrade, not be silently
        // repaired.
        jdbc.update("UPDATE knowledge_documents SET active_version_id=? WHERE id=?", v1, d2);
        assertThatThrownBy(() -> flyway("21").migrate())
                .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT to_regclass('public.knowledge_base') IS NULL",
                                Boolean.class))
                .isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM flyway_schema_history WHERE version='21'",
                                Integer.class))
                .isZero();
        jdbc.update("UPDATE knowledge_documents SET active_version_id=NULL WHERE id=?", d2);
        assertThat(flyway("21").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(fingerprints(jdbc, tables)).isEqualTo(before);
        assertThat(
                        Arrays.stream(flyway("21").info().applied())
                                .limit(20)
                                .map(m -> List.of(m.getVersion(), m.getScript(), m.getChecksum()))
                                .toList())
                .isEqualTo(old);
        assertThat(flyway("21").migrate().migrationsExecuted).isZero();
        flyway("21").validate();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM knowledge_document_scope", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agent_session", Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT source_locator IS NULL FROM knowledge_chunks WHERE version_id=?",
                                Boolean.class,
                                v1))
                .isTrue();
    }

    Map<String, String> fingerprints(JdbcTemplate jdbc, List<String> tables) {
        var map = new TreeMap<String, String>();
        for (String table : tables) {
            String excluded = table.equals("knowledge_chunks") ? "-'source_locator'" : "";
            map.put(
                    table,
                    jdbc.queryForObject(
                            "SELECT md5(COALESCE(string_agg(data,E'\\n' ORDER BY data),'')) FROM (SELECT (to_jsonb(t)"
                                    + excluded
                                    + ")::text AS data FROM public."
                                    + table
                                    + " t) q",
                            String.class));
        }
        return map;
    }
}
