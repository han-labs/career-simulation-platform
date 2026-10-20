package edu.hcmute.careersim.guidance.agent.dao;

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
