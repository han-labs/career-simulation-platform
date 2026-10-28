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
class StudentActionsUpgradeIntegrationTest {
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

    Map<String, String> fingerprints(JdbcTemplate jdbc) {
        var result = new TreeMap<String, String>();
        for (var table :
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history' ORDER BY table_name",
                        String.class))
            result.put(
                    table,
                    jdbc.queryForObject(
                            "SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,'' ORDER BY to_jsonb(t)::text),'')) FROM public.\""
                                    + table
                                    + "\" t",
                            String.class));
        return result;
    }

    @Test
    void v32ToV33PreservesEveryExistingRowAndChecksumAndIsIdempotent() {
        flyway("32").migrate();
        var jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        var before = fingerprints(jdbc);
        var checksums =
                Arrays.stream(flyway("32").info().applied())
                        .map(m -> List.of(m.getVersion(), m.getScript(), m.getChecksum()))
                        .toList();
        assertThat(flyway("33").migrate().migrationsExecuted).isEqualTo(1);
        flyway("33").validate();
        assertThat(flyway("33").migrate().migrationsExecuted).isZero();
        assertThat(fingerprints(jdbc)).isEqualTo(before);
        assertThat(
                        Arrays.stream(flyway("33").info().applied())
                                .limit(32)
                                .map(m -> List.of(m.getVersion(), m.getScript(), m.getChecksum()))
                                .toList())
                .isEqualTo(checksums);
        for (var function : List.of("guard_insight_report", "guard_confirmed_plan_source"))
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT pg_get_functiondef(oid) FROM pg_proc WHERE proname=?",
                                    String.class,
                                    function))
                    .contains("requestedAction", "e.status='SUCCEEDED'", "s.status='ACTIVE'");
    }
}
