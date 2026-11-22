package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.simulation.domain.AnalyticsFilter;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ScenarioAnalyticsDao {
    private final NamedParameterJdbcTemplate jdbc;

    private static final String FILTERED =
            """
            WITH selected AS (
                SELECT e.id, e.student_id, e.scenario_version_id, e.status, e.started_at,
                       v.title, v.version_number
                FROM public.simulation_enrollment e
                JOIN public.scenario_version v ON v.id = e.scenario_version_id
                JOIN public.scenario s ON s.id = v.scenario_id
                WHERE s.enterprise_id = :enterprise AND v.status = 'PUBLISHED'
                  AND (CAST(:scenario AS bigint) IS NULL OR s.id = :scenario)
                  AND (CAST(:from AS timestamptz) IS NULL OR e.enrolled_at >= :from)
                  AND (CAST(:until AS timestamptz) IS NULL OR e.enrolled_at < :until)
                  AND (CAST(:status AS text) IS NULL OR e.status = :status)
            )
            """;

    private MapSqlParameterSource parameters(long enterprise, AnalyticsFilter filter) {
        return new MapSqlParameterSource("enterprise", enterprise)
                .addValue("scenario", filter.scenario(), java.sql.Types.BIGINT)
                .addValue("from", filter.from(), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("until", filter.until(), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("status", filter.status(), java.sql.Types.VARCHAR);
    }

    public List<AnalyticsProgram> programs(long enterprise) {
        return jdbc.query(
                """
                SELECT s.id::text, latest.title
                FROM public.scenario s
                JOIN LATERAL (
                    SELECT v.title FROM public.scenario_version v
                    WHERE v.scenario_id = s.id AND v.status = 'PUBLISHED'
                    ORDER BY v.version_number DESC LIMIT 1
                ) latest ON true
                WHERE s.enterprise_id = :enterprise
                ORDER BY latest.title, s.id
                """,
                new MapSqlParameterSource("enterprise", enterprise),
                (r, n) -> new AnalyticsProgram(r.getString(1), r.getString(2)));
    }

    public AnalyticsCounts counts(long enterprise, AnalyticsFilter filter) {
        return jdbc.queryForObject(
                FILTERED
                        + """
                SELECT count(*), count(started_at), count(*) FILTER (WHERE status = 'COMPLETED'),
                       count(DISTINCT student_id)
                FROM selected
                """,
                parameters(enterprise, filter),
                (r, n) ->
                        new AnalyticsCounts(
                                r.getLong(1), r.getLong(2), r.getLong(3), r.getLong(4)));
    }

    public List<AnalyticsTask> tasks(long enterprise, AnalyticsFilter filter) {
        return jdbc.query(
                FILTERED
                        + """
                SELECT e.scenario_version_id::text, e.title, e.version_number, t.id::text,
                       t.position, t.title, count(*) FILTER (WHERE sub.id IS NULL),
                       count(*) FILTER (WHERE sub.status = 'DRAFT'),
                       count(*) FILTER (WHERE sub.status = 'FINAL')
                FROM selected e
                JOIN public.simulation_task t ON t.scenario_version_id = e.scenario_version_id
                LEFT JOIN public.task_submission sub ON sub.enrollment_id = e.id
                    AND sub.task_id = t.id AND sub.scenario_version_id = e.scenario_version_id
                GROUP BY e.scenario_version_id, e.title, e.version_number, t.id
                ORDER BY e.title, e.version_number, e.scenario_version_id, t.position
                """,
                parameters(enterprise, filter),
                (r, n) ->
                        new AnalyticsTask(
                                r.getString(1),
                                r.getString(2),
                                r.getInt(3),
                                r.getString(4),
                                r.getInt(5),
                                r.getString(6),
                                r.getLong(7),
                                r.getLong(8),
                                r.getLong(9)));
    }

    public List<AnalyticsEvidence> evidence(
            long enterprise, AnalyticsFilter filter, Long before, int limit) {
        return jdbc.query(
                FILTERED
                        + """
                SELECT c.id::text, e.title, e.version_number, c.completed_at
                FROM selected e
                JOIN public.simulation_completion c ON c.enrollment_id = e.id
                JOIN public.app_users u ON u.id = e.student_id AND u.status = 'ACTIVE'
                WHERE e.status = 'COMPLETED'
                  AND (CAST(:before AS bigint) IS NULL OR c.id < :before)
                  AND EXISTS (
                    SELECT 1 FROM public.consent_grant g
                    WHERE g.user_id = u.id AND g.purpose = 'COMPLETION_SHARE'
                      AND g.recipient_key = :recipient AND g.confirmed_at <= CURRENT_TIMESTAMP
                      AND g.revoked_at IS NULL
                      AND (g.expires_at IS NULL OR g.expires_at > CURRENT_TIMESTAMP)
                  )
                ORDER BY c.id DESC LIMIT :limit
                """,
                parameters(enterprise, filter)
                        .addValue("before", before, java.sql.Types.BIGINT)
                        .addValue("limit", limit)
                        .addValue("recipient", "enterprise:" + enterprise),
                (r, n) ->
                        new AnalyticsEvidence(
                                r.getString(1),
                                r.getString(2),
                                r.getInt(3),
                                r.getTimestamp(4).toInstant()));
    }
}
