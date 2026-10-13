package edu.hcmute.careersim.guidance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Operator-pinned list-price estimates, never a claim about the provider's invoice. */
@Component
public class SynUsagePricing {
    public record Rate(
            String provider,
            String model,
            String snapshot,
            BigDecimal inputUsdPerMillion,
            BigDecimal outputUsdPerMillion) {}

    private final Map<String, Rate> rates = new HashMap<>();

    public SynUsagePricing(
            ObjectMapper mapper, @Value("${app.syn.cost-rates-json:[]}") String configuration) {
        try {
            var values = mapper.readValue(configuration, Rate[].class);
            if (values == null || values.length > 30) throw new IllegalArgumentException();
            for (Rate rate : values) {
                if (rate == null
                        || !Set.of("gemini", "openai").contains(rate.provider())
                        || rate.model() == null
                        || rate.model().isBlank()
                        || rate.model().length() > 180
                        || rate.snapshot() == null
                        || rate.snapshot().isBlank()
                        || rate.snapshot().length() > 240
                        || invalidRate(rate.inputUsdPerMillion())
                        || invalidRate(rate.outputUsdPerMillion())
                        || rates.putIfAbsent(rate.provider() + "/" + rate.model(), rate) != null)
                    throw new IllegalArgumentException();
            }
        } catch (Exception invalid) {
            // Do not echo environment configuration or parser source text into startup logs.
            throw new IllegalArgumentException("Invalid Syn pricing snapshot configuration.");
        }
    }

    private static boolean invalidRate(BigDecimal rate) {
        return rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("1000000")) > 0;
    }

    public Map<String, Object> enrich(Map<String, Object> trace) {
        List<?> calls = trace.get("calls") instanceof List<?> value ? value : List.of();
        List<?> usage = trace.get("tokenUsage") instanceof List<?> value ? value : List.of();
        var lines = new ArrayList<Map<String, Object>>();
        var phaseCounts = new TreeMap<String, Integer>();
        for (Object call : calls) {
            if (call instanceof Map<?, ?> row)
                phaseCounts.merge(String.valueOf(row.get("phase")), 1, Integer::sum);
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        int priced = 0;
        for (Object item : usage) {
            if (!(item instanceof Map<?, ?> row)) continue;
            var line = new LinkedHashMap<String, Object>();
            line.put("phase", row.get("phase"));
            line.put("provider", row.get("provider"));
            line.put("model", row.get("model"));
            Rate rate = rates.get(row.get("provider") + "/" + row.get("model"));
            Long input =
                    row.get("inputTokens") instanceof Number n
                            ? SynExecutionScope.validTokens(n)
                            : null;
            Long output =
                    row.get("outputTokens") instanceof Number n
                            ? SynExecutionScope.validTokens(n)
                            : null;
            if (input == null || output == null) {
                line.put("status", "USAGE_NOT_REPORTED");
            } else if (rate == null) {
                line.put("status", "UNPRICED_MODEL");
            } else {
                BigDecimal estimate =
                        rate.inputUsdPerMillion()
                                .multiply(BigDecimal.valueOf(input))
                                .add(
                                        rate.outputUsdPerMillion()
                                                .multiply(BigDecimal.valueOf(output)))
                                .movePointLeft(6);
                line.put("status", "ESTIMATED");
                line.put("snapshot", rate.snapshot());
                line.put("inputUsdPerMillion", rate.inputUsdPerMillion());
                line.put("outputUsdPerMillion", rate.outputUsdPerMillion());
                line.put("estimatedUsd", estimate);
                subtotal = subtotal.add(estimate);
                priced++;
            }
            lines.add(line);
        }
        int unreportedCalls = Math.max(0, calls.size() - usage.size());
        boolean complete = calls.size() == priced && usage.size() == priced;
        var cost = new LinkedHashMap<String, Object>();
        cost.put(
                "status",
                calls.isEmpty() && usage.isEmpty()
                        ? "NO_EXTERNAL_CALLS"
                        : complete ? "ESTIMATED" : priced > 0 ? "PARTIAL" : "UNAVAILABLE");
        cost.put("currency", "USD");
        cost.put("method", "PINNED_UNDISCOUNTED_TOKEN_RATES");
        cost.put("estimatedSubtotalUsd", subtotal);
        cost.put("totalEstimateUsd", complete ? subtotal : null);
        cost.put("callsWithoutUsage", unreportedCalls);
        cost.put("lines", lines);
        var result = new LinkedHashMap<>(trace);
        result.put("callCounts", phaseCounts);
        result.put("cost", cost);
        return result;
    }
}
