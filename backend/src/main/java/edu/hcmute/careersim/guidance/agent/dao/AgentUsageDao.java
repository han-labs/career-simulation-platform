package edu.hcmute.careersim.guidance.agent.dao;

import static edu.hcmute.careersim.guidance.agent.dto.AgentUsageContracts.*;

import edu.hcmute.careersim.common.value.CoreValues;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Operational projection only; private execution JSON is never selected into the result. */
@Repository
@RequiredArgsConstructor
public class AgentUsageDao {
    private final JdbcTemplate jdbc;

    private static final String JOINS =
            " FROM public.agent_execution e LEFT JOIN public.agent_configuration c ON c.id=e.configuration_id LEFT JOIN public.agent a ON a.id=c.agent_id ";
    private static final String PROVIDER =
            "CASE WHEN position('/' IN e.model_key)>0 THEN split_part(e.model_key,'/',1) ELSE NULL END";
    private static final String COUNTS =
            "count(*) AS requests,count(*) FILTER(WHERE e.status='SUCCEEDED') AS succeeded,count(*) FILTER(WHERE e.status='FAILED') AS failed,count(*) FILTER(WHERE e.status='REFUSED') AS refused,count(*) FILTER(WHERE e.status='CANCELLED') AS cancelled,count(*) FILTER(WHERE e.status IN ('ACCEPTED','RUNNING')) AS pending";

    private record Sql(String where, List<Object> args) {}

    private Sql scope(Filter f) {
        var args = new ArrayList<Object>();
        args.add(java.sql.Timestamp.from(f.from()));
        args.add(java.sql.Timestamp.from(f.to()));
        var where = new StringBuilder(" WHERE e.started_at>=? AND e.started_at<?");
        for (var pair :
                List.of(
                        new Object[] {"a.id", f.agent()},
                        new Object[] {PROVIDER, f.provider()},
                        new Object[] {"e.model_key", f.model()},
                        new Object[] {"e.status", f.outcome()},
                        new Object[] {"e.execution_mode", f.mode()})) {
            if (pair[1] != null) {
                where.append(" AND ").append(pair[0]).append("=?");
                args.add(pair[1]);
            }
        }
        return new Sql(where.toString(), args);
    }

    public Summary summary(Filter f) {
        var q = scope(f);
        return jdbc.queryForObject(
                "SELECT "
                        + COUNTS
                        + """
            ,sum(e.estimated_cost) FILTER(WHERE e.cost_status IN ('ESTIMATED','PARTIAL')) AS cost,
            count(*) FILTER(WHERE e.cost_status='ESTIMATED') AS estimated,
            count(*) FILTER(WHERE e.cost_status='PARTIAL') AS partial,
            count(*) FILTER(WHERE e.cost_status='UNKNOWN') AS unknown,
            count(*) FILTER(WHERE e.cost_status='NOT_APPLICABLE') AS not_applicable,
            avg(e.latency_ms) FILTER(WHERE e.finished_at IS NOT NULL) AS latency,
            count(e.latency_ms) FILTER(WHERE e.finished_at IS NOT NULL) AS samples,
            count(*) FILTER(WHERE e.finished_at IS NOT NULL AND e.latency_ms IS NULL) AS missing
            """
                        + JOINS
                        + q.where(),
                (r, n) ->
                        new Summary(
                                r.getLong("requests"),
                                r.getLong("succeeded"),
                                r.getLong("failed"),
                                r.getLong("refused"),
                                r.getLong("cancelled"),
                                r.getLong("pending"),
                                r.getBigDecimal("cost"),
                                r.getLong("estimated"),
                                r.getLong("partial"),
                                r.getLong("unknown"),
                                r.getLong("not_applicable"),
                                r.getBigDecimal("latency"),
                                r.getLong("samples"),
                                r.getLong("missing")),
                q.args().toArray());
    }

    public List<Bucket> trend(Filter f, String unit) {
        var q = scope(f);
        // Unit is chosen by the service, never interpolated from a request.
        String bucket =
                "date_trunc('"
                        + ("hour".equals(unit) ? "hour" : "day")
                        + "',e.started_at AT TIME ZONE 'UTC') AT TIME ZONE 'UTC'";
        return jdbc.query(
                "SELECT "
                        + bucket
                        + " AS bucket,"
                        + COUNTS
                        + JOINS
                        + q.where()
                        + " GROUP BY 1 ORDER BY 1",
                (r, n) ->
                        new Bucket(
                                r.getTimestamp("bucket").toInstant(),
                                r.getLong("requests"),
                                r.getLong("succeeded"),
                                r.getLong("failed"),
                                r.getLong("refused"),
                                r.getLong("cancelled"),
                                r.getLong("pending")),
                q.args().toArray());
    }

    public List<Execution> recent(Filter f, Position position) {
        var q = scope(f);
        var args = new ArrayList<>(q.args());
        String cursor = "";
        if (position != null) {
            cursor = " AND (e.started_at,e.id)<(?,?::uuid)";
            args.add(java.sql.Timestamp.from(position.startedAt()));
            args.add(position.id());
        }
        args.add(f.limit() + 1);
        // Select individual metadata columns only. Private JSON never crosses the DAO boundary.
        String query =
                """
            SELECT e.id,e.started_at,e.finished_at,e.execution_mode,a.id AS agent_id,a.name AS agent_name,
            e.configuration_id,c.version_number,e.prompt_hash,e.model_key,e.status,e.latency_ms,
            e.input_tokens,e.output_tokens,e.estimated_cost,e.cost_status,
            CASE WHEN e.refusal_or_error_code IN ('TIMEOUT_OR_TRANSPORT','PROVIDER_REJECTED','PROVIDER_RATE_LIMITED',
              'PROVIDER_UNAVAILABLE','PROVIDER_NOT_CONFIGURED','PROVIDER_CLIENT_UNAVAILABLE','PROVIDER_AUTH_REJECTED',
              'PROVIDER_PERMISSION_REJECTED','PROVIDER_PAYMENT_REQUIRED','PROVIDER_MODEL_UNAVAILABLE','PROVIDER_DISABLED',
              'PROVIDER_HTTP_UNAVAILABLE','PROVIDER_NETWORK_UNAVAILABLE','PROVIDER_TRANSPORT_DEADLINE',
              'MALFORMED_OUTPUT','UNAVAILABLE_TOOL','EMPTY_EVIDENCE','CONSENT_REVOKED',
              'ITERATION_LIMIT','EXECUTION_FAILED','EXTERNAL_AI_CONSENT_REQUIRED','INSUFFICIENT_EVIDENCE','REFUSED')
              THEN e.refusal_or_error_code WHEN e.refusal_or_error_code IS NOT NULL THEN 'OTHER' ELSE NULL END AS failure,
            CASE WHEN jsonb_typeof(e.pricing_snapshot#>'{generation,calls}')='array'
              THEN jsonb_array_length(e.pricing_snapshot#>'{generation,calls}')::bigint ELSE NULL END AS model_attempts,
            (SELECT s->>'effectiveModel' FROM jsonb_array_elements(e.tool_steps_and_sources) WITH ORDINALITY AS x(s,n)
              WHERE s->>'type'='MODEL_ATTEMPT' AND s->>'effectiveModel' ~ '^(gemini|openai)/[A-Za-z0-9._-]{1,100}$'
              ORDER BY n DESC LIMIT 1) AS effective_model,
            (SELECT coalesce(jsonb_agg(DISTINCT s->>'reason' ORDER BY s->>'reason'),'[]'::jsonb) FROM jsonb_array_elements(e.tool_steps_and_sources) s
              WHERE s->>'type'='MODEL_ATTEMPT' AND s->>'effectiveModel'<>s->>'requestedModel'
              AND s->>'reason' IN ('PROVIDER_MODEL_UNAVAILABLE','PROVIDER_HTTP_UNAVAILABLE','PROVIDER_RATE_LIMITED',
                'PROVIDER_TRANSPORT_DEADLINE','PROVIDER_NETWORK_UNAVAILABLE','PROVIDER_REJECTED','STRUCTURED_OUTPUT_INVALID')) AS fallback_reasons,
            (SELECT count(*) FROM jsonb_array_elements(e.tool_steps_and_sources) s WHERE s->>'type'='ACTUAL_NATIVE_CALL') AS native_count,
            (SELECT count(*) FROM jsonb_array_elements(e.tool_steps_and_sources) s WHERE s->>'type'='SERVER_RETRIEVAL') AS retrieval_count,
            (SELECT coalesce(jsonb_agg(t.name ORDER BY t.name COLLATE "C"),'[]'::jsonb) FROM (
              SELECT DISTINCT CASE WHEN s->>'name' IN ('kb.search','assessment.lookup','simulation.explore','simulation.myEvidence')
                THEN s->>'name' ELSE 'UNKNOWN' END AS name
              FROM jsonb_array_elements(e.tool_steps_and_sources) s WHERE s->>'type'='ACTUAL_NATIVE_CALL') t) AS tools,
            """
                        + PROVIDER
                        + " AS provider"
                        + JOINS
                        + q.where()
                        + cursor
                        + " ORDER BY e.started_at DESC,e.id DESC LIMIT ?";
        return jdbc.query(
                query,
                (r, n) -> {
                    var tools = new ArrayList<String>();
                    CoreValues.json(r.getString("tools")).forEach(x -> tools.add(x.asText()));
                    var fallbackReasons = new ArrayList<String>();
                    CoreValues.json(r.getString("fallback_reasons"))
                            .forEach(x -> fallbackReasons.add(x.asText()));
                    return new Execution(
                            r.getString("id"),
                            r.getTimestamp("started_at").toInstant(),
                            r.getTimestamp("finished_at") == null
                                    ? null
                                    : r.getTimestamp("finished_at").toInstant(),
                            r.getString("execution_mode"),
                            r.getString("agent_id"),
                            r.getString("agent_name"),
                            r.getString("configuration_id"),
                            r.getObject("version_number", Integer.class),
                            r.getString("prompt_hash"),
                            r.getString("provider"),
                            r.getString("model_key"),
                            r.getString("status"),
                            r.getString("failure"),
                            r.getObject("latency_ms", Long.class),
                            r.getObject("input_tokens", Long.class),
                            r.getObject("output_tokens", Long.class),
                            r.getBigDecimal("estimated_cost"),
                            r.getString("cost_status"),
                            r.getLong("native_count"),
                            tools,
                            r.getLong("retrieval_count"),
                            r.getObject("model_attempts", Long.class),
                            r.getString("effective_model"),
                            fallbackReasons);
                },
                args.toArray());
    }

    public Options options(Filter f) {
        Object[] range = {java.sql.Timestamp.from(f.from()), java.sql.Timestamp.from(f.to())};
        String time = " WHERE e.started_at>=? AND e.started_at<?";
        var agents =
                jdbc.query(
                        "SELECT DISTINCT a.id,a.name"
                                + JOINS
                                + time
                                + " AND a.id IS NOT NULL ORDER BY a.name,a.id LIMIT 101",
                        (r, n) -> new AgentOption(r.getString("id"), r.getString("name")),
                        range);
        var models =
                jdbc.query(
                        "SELECT DISTINCT e.model_key,"
                                + PROVIDER
                                + " AS provider"
                                + JOINS
                                + time
                                + " AND e.model_key IS NOT NULL ORDER BY e.model_key LIMIT 101",
                        (r, n) ->
                                new ModelOption(r.getString("provider"), r.getString("model_key")),
                        range);
        return new Options(
                agents.stream().limit(100).toList(),
                models.stream().limit(100).toList(),
                agents.size() > 100 || models.size() > 100);
    }

    public Map<String, Object> aggregate(Instant from, Instant to, String mode) {
        Object[] args = {java.sql.Timestamp.from(from), java.sql.Timestamp.from(to), mode, mode};
        String where =
                " WHERE started_at>=? AND started_at<? AND (?::text IS NULL OR execution_mode=?)";
        var totals =
                jdbc.queryForList(
                        "SELECT execution_mode AS mode,status,count(*) AS executions,sum(COALESCE(jsonb_array_length(pricing_snapshot#>'{generation,calls}'),0)) AS recorded_model_attempts,count(*) FILTER(WHERE pricing_snapshot#>'{generation,calls}' IS NULL) AS unknown_model_attempt_executions,count(*) FILTER(WHERE finished_at IS NOT NULL) AS latency_samples,avg(latency_ms) FILTER(WHERE finished_at IS NOT NULL) AS mean_latency_ms,sum(input_tokens) AS known_input_tokens,sum(output_tokens) AS known_output_tokens,count(*) FILTER(WHERE input_tokens IS NULL OR output_tokens IS NULL) AS unknown_token_executions,sum(estimated_cost) AS known_estimated_subtotal_usd,count(*) FILTER(WHERE cost_status='UNKNOWN') AS unknown_cost_executions,count(*) FILTER(WHERE cost_status='PARTIAL') AS partial_cost_executions,count(*) FILTER(WHERE cost_status='ESTIMATED') AS estimated_cost_executions,count(*) FILTER(WHERE cost_status='NOT_APPLICABLE') AS not_applicable_executions FROM public.agent_execution"
                                + where
                                + " GROUP BY execution_mode,status ORDER BY execution_mode,status",
                        args);
        var toolCounts =
                jdbc.queryForList(
                        "SELECT e.execution_mode AS mode,CASE WHEN s->>'name' IN ('kb.search','assessment.lookup','simulation.explore','simulation.myEvidence') THEN s->>'name' ELSE 'UNKNOWN' END AS tool,CASE WHEN s->>'status' IN ('SUCCEEDED','DENIED','FAILED') THEN s->>'status' ELSE 'UNKNOWN' END AS status,count(*) AS native_calls FROM (SELECT * FROM public.agent_execution"
                                + where
                                + ") e CROSS JOIN LATERAL jsonb_array_elements(e.tool_steps_and_sources) s WHERE s->>'type'='ACTUAL_NATIVE_CALL' GROUP BY 1,2,3 ORDER BY 1,2,3",
                        args);
        return Map.of(
                "from",
                from,
                "to",
                to,
                "groups",
                totals,
                "nativeTools",
                toolCounts,
                "semantics",
                Map.of(
                        "cost",
                        "Known estimated subtotal only; unknown/partial are not zero or billed amounts.",
                        "tokens",
                        "Known totals; missing values remain unknown.",
                        "latency",
                        "Finished executions only; milliseconds.",
                        "tools",
                        "Actual provider-requested native dispatch attempts, separated by success/denial. Server retrieval excluded."));
    }
}
