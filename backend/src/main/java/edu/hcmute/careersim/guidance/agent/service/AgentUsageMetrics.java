package edu.hcmute.careersim.guidance.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.service.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgentUsageMetrics {
    private final SynUsagePricing pricing;

    public record Metrics(
            Long input, Long output, BigDecimal cost, String status, Map<String, Object> detail) {}

    public Metrics usage(boolean retrievalOnly) {
        var trace = SynExecutionScope.trace();
        var calls = (List<?>) trace.get("calls");
        var reported = trace.get("tokenUsage") instanceof List<?> list ? list : List.of();
        var generationCalls =
                calls.stream()
                        .filter(
                                x ->
                                        x instanceof Map<?, ?> m
                                                && "AGENT_MODEL".equals(m.get("phase")))
                        .toList();
        var generationUsage =
                reported.stream()
                        .filter(
                                x ->
                                        x instanceof Map<?, ?> m
                                                && "AGENT_MODEL".equals(m.get("phase")))
                        .toList();
        var priced =
                pricing.enrich(Map.of("calls", generationCalls, "tokenUsage", generationUsage));
        JsonNode cost = CoreValues.tree(priced.get("cost"));
        String state =
                retrievalOnly || generationCalls.isEmpty()
                        ? "NOT_APPLICABLE"
                        : cost.path("status").asText().equals("ESTIMATED")
                                ? "ESTIMATED"
                                : cost.path("status").asText().equals("PARTIAL")
                                        ? "PARTIAL"
                                        : "UNKNOWN";
        boolean complete =
                !generationCalls.isEmpty()
                        && generationCalls.size() == generationUsage.size()
                        && generationUsage.stream()
                                .allMatch(
                                        x ->
                                                x instanceof Map<?, ?> m
                                                        && m.get("inputTokens") instanceof Number
                                                        && m.get("outputTokens") instanceof Number);
        Long input =
                complete
                        ? generationUsage.stream()
                                .mapToLong(
                                        x ->
                                                ((Number) ((Map<?, ?>) x).get("inputTokens"))
                                                        .longValue())
                                .sum()
                        : null;
        Long output =
                complete
                        ? generationUsage.stream()
                                .mapToLong(
                                        x ->
                                                ((Number) ((Map<?, ?>) x).get("outputTokens"))
                                                        .longValue())
                                .sum()
                        : null;
        BigDecimal estimate =
                Set.of("ESTIMATED", "PARTIAL").contains(state)
                        ? cost.path(
                                        state.equals("ESTIMATED")
                                                ? "totalEstimateUsd"
                                                : "estimatedSubtotalUsd")
                                .decimalValue()
                        : null;
        if (estimate != null && estimate.compareTo(new BigDecimal("9999999999.99999999")) > 0) {
            state = "UNKNOWN";
            estimate = null;
        }
        var resource =
                pricing.enrich(
                        Map.of(
                                "calls",
                                calls.stream()
                                        .filter(
                                                x ->
                                                        x instanceof Map<?, ?> m
                                                                && !"AGENT_MODEL"
                                                                        .equals(m.get("phase")))
                                        .toList(),
                                "tokenUsage",
                                reported.stream()
                                        .filter(
                                                x ->
                                                        x instanceof Map<?, ?> m
                                                                && !"AGENT_MODEL"
                                                                        .equals(m.get("phase")))
                                        .toList()));
        return new Metrics(
                retrievalOnly ? null : input,
                retrievalOnly ? null : output,
                retrievalOnly ? null : estimate,
                state,
                Map.of(
                        "generation",
                        priced,
                        "embeddingResources",
                        resource,
                        "denominator",
                        "MODEL_GENERATION_ONLY; EMBEDDING_RESOURCE_USAGE_SEPARATE"));
    }
}
