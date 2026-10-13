package edu.hcmute.careersim.guidance.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

class SynUsagePricingTest {
    private final SynUsagePricing pricing =
            new SynUsagePricing(
                    new ObjectMapper(),
                    """
        [{"provider":"gemini","model":"test-model","snapshot":"synthetic-test-only",
          "inputUsdPerMillion":2,"outputUsdPerMillion":6},
         {"provider":"gemini","model":"gemini-embedding-001","snapshot":"synthetic-test-only",
          "inputUsdPerMillion":0.1,"outputUsdPerMillion":0}]
        """);

    @Test
    void separatesPhasesAndModelsAndIncludesThinkingWithoutRoundingAwaySmallCosts() {
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(20), () -> true)) {
            SynExecutionScope.observe(
                    "PLANNING",
                    () -> {
                        SynExecutionScope.recordUsage(
                                "gemini",
                                "test-model",
                                1_000_000,
                                SynExecutionScope.outputWithThinking(20_000, 5_000));
                        return true;
                    });
            SynExecutionScope.observe(
                    "QUERY_EMBEDDING",
                    () -> {
                        SynExecutionScope.recordUsage("gemini", "gemini-embedding-001", 2_000, 0);
                        return true;
                    });
            var trace = pricing.enrich(SynExecutionScope.trace());
            assertThat(trace.get("callCounts"))
                    .isEqualTo(Map.of("PLANNING", 1, "QUERY_EMBEDDING", 1));
            var cost = (Map<?, ?>) trace.get("cost");
            assertThat(cost.get("status")).isEqualTo("ESTIMATED");
            assertThat((BigDecimal) cost.get("totalEstimateUsd")).isEqualByComparingTo("2.1502");
            assertThat(cost.toString())
                    .contains("synthetic-test-only", "PINNED_UNDISCOUNTED_TOKEN_RATES");
        }
        assertThat(SynExecutionScope.trace().get("tokenUsage")).isEqualTo("NOT_REPORTED");
    }

    @Test
    void unknownModelMissingUsageAndTransportFailureNeverBecomeAZeroTotal() {
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(20), () -> true)) {
            for (String model : List.of("test-model", "changed-model"))
                SynExecutionScope.observe(
                        "GENERATION",
                        () -> {
                            SynExecutionScope.recordUsage("gemini", model, 100, 10);
                            return true;
                        });
            SynExecutionScope.observe(
                    "QUERY_EMBEDDING",
                    () -> {
                        SynExecutionScope.recordUsage("gemini", "gemini-embedding-001", null, 0);
                        return true;
                    });
            assertThatThrownBy(
                            () ->
                                    SynExecutionScope.observe(
                                            "GENERATION",
                                            () -> {
                                                throw new IllegalStateException(
                                                        "synthetic timeout");
                                            }))
                    .isInstanceOf(IllegalStateException.class);
            var cost = (Map<?, ?>) pricing.enrich(SynExecutionScope.trace()).get("cost");
            assertThat(cost.get("status")).isEqualTo("PARTIAL");
            assertThat(cost.get("totalEstimateUsd")).isNull();
            assertThat(cost.get("callsWithoutUsage")).isEqualTo(1);
            assertThat(cost.toString()).contains("UNPRICED_MODEL", "USAGE_NOT_REPORTED");
        }
    }

    @Test
    void rejectsInvalidPricesAndInvalidTokenCountsWithoutEchoingConfiguration() {
        for (String json :
                List.of(
                        "not-json-secret",
                        "null",
                        """
            [{"provider":"gemini","model":"x","snapshot":"test",
              "inputUsdPerMillion":-1,"outputUsdPerMillion":0}]
            """))
            assertThatThrownBy(() -> new SynUsagePricing(new ObjectMapper(), json))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid Syn pricing snapshot configuration.");
        assertThat(SynExecutionScope.validTokens(-1)).isNull();
        assertThat(SynExecutionScope.validTokens(1.5)).isNull();
        assertThat(SynExecutionScope.validTokens(Double.NaN)).isNull();
        assertThat(SynExecutionScope.outputWithThinking(100, -1)).isNull();
    }

    @Test
    void standardOnlyTurnHasNoExternalCallsAndDoesNotNeedConfiguredRates() {
        var unconfigured = new SynUsagePricing(new ObjectMapper(), "[]");
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(20), () -> true)) {
            var cost = (Map<?, ?>) unconfigured.enrich(SynExecutionScope.trace()).get("cost");
            assertThat(cost.get("status")).isEqualTo("NO_EXTERNAL_CALLS");
            assertThat(cost.get("totalEstimateUsd")).isEqualTo(BigDecimal.ZERO);
        }
    }
}
