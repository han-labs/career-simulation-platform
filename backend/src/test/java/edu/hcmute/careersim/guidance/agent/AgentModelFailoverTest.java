package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.*;
import edu.hcmute.careersim.guidance.agent.service.AgentModelProvider.*;
import edu.hcmute.careersim.guidance.infrastructure.AgentTransportBudget;
import edu.hcmute.careersim.guidance.service.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AgentModelFailoverTest {
    final List<Map<String, Object>> audit = new ArrayList<>();
    final List<Long> sleeps = new ArrayList<>();

    AgentModelFailover policy() {
        return new AgentModelFailover(
                "gemini/primary",
                CoreValues.tree(Map.of("fallbackModelKeys", List.of("gemini/secondary"))),
                audit::add,
                sleeps::add,
                () -> 25);
    }

    SynGuidanceProvider.ProviderException remote(int status) {
        return new SynGuidanceProvider.ProviderException(
                SynGuidanceProvider.FailureKind.PROVIDER_HTTP_UNAVAILABLE,
                Map.of("providerHttpStatus", status));
    }

    Decision finalDecision() {
        return new Decision(null, List.of(), CoreValues.tree(Map.of("valid", true)));
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void retriesOnceThenUsesCompatibleFallbackWithoutToolDispatch(int status) {
        var calls = new ArrayList<String>();
        var p = policy();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
            assertThat(
                            p.call(
                                            key -> {
                                                calls.add(key);
                                                if (key.endsWith("primary")) throw remote(status);
                                                return finalDecision();
                                            })
                                    .output()
                                    .path("valid")
                                    .asBoolean())
                    .isTrue();
            assertThat(calls)
                    .containsExactly("gemini/primary", "gemini/primary", "gemini/secondary");
            assertThat(sleeps).containsExactly(225L);
            assertThat(audit.get(2))
                    .containsEntry("effectiveModel", "gemini/secondary")
                    .containsEntry("reason", "PROVIDER_HTTP_UNAVAILABLE");
            assertThat(SynExecutionScope.trace().get("calls")).asList().hasSize(3);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 402, 403, 409, 501})
    void rejectsNonTransientHttpWithoutRetryOrModelSwitch(int status) {
        var calls = new AtomicInteger();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
            assertThatThrownBy(
                            () ->
                                    policy().call(
                                                    key -> {
                                                        calls.incrementAndGet();
                                                        throw remote(status);
                                                    }))
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
        }
        assertThat(calls).hasValue(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void missingModelSwitchesImmediatelyAndSafetyRefusalDoesNotSwitch() {
        var calls = new ArrayList<String>();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
            policy().call(
                            key -> {
                                calls.add(key);
                                if (key.endsWith("primary"))
                                    throw new SynGuidanceProvider.ProviderException(
                                            SynGuidanceProvider.FailureKind
                                                    .PROVIDER_MODEL_UNAVAILABLE);
                                return finalDecision();
                            });
            assertThat(calls).containsExactly("gemini/primary", "gemini/secondary");
            assertThat(sleeps).isEmpty();
            calls.clear();
            var refused =
                    policy().call(
                                    key -> {
                                        calls.add(key);
                                        return new Decision(
                                                null,
                                                List.of(),
                                                CoreValues.tree(Map.of("outcome", "REFUSED")));
                                    });
            assertThat(refused.output().path("outcome").asText()).isEqualTo("REFUSED");
            assertThat(calls).hasSize(1);
        }
    }

    @Test
    void capsTheEntireExecutionAndKeepsSelectedFallbackAcrossRounds() {
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
            var p = policy();
            for (int i = 0; i < AgentModelFailover.MAX_ATTEMPTS; i++)
                p.call(key -> finalDecision());
            assertThatThrownBy(() -> p.call(key -> finalDecision()))
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
            assertThat(audit).hasSize(AgentModelFailover.MAX_ATTEMPTS);
        }
    }

    @Test
    void neverRetriesAfterPublishingPartialText() {
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
            assertThatThrownBy(
                            () ->
                                    policy().call(
                                                    key -> {
                                                        throw remote(503);
                                                    },
                                                    () -> false))
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
            assertThat(audit).hasSize(1);
            assertThat(sleeps).isEmpty();
        }
    }

    @Test
    void switchesAfterTransportDeadlineWithoutRepeatingTheTimedOutModelButNotDnsTlsOrPermission() {
        for (String category : List.of("DNS", "TLS", "CONNECT")) {
            audit.clear();
            sleeps.clear();
            try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
                assertThatThrownBy(
                                () ->
                                        policy().call(
                                                        key -> {
                                                            throw new SynGuidanceProvider
                                                                    .ProviderException(
                                                                    SynGuidanceProvider.FailureKind
                                                                            .PROVIDER_NETWORK_UNAVAILABLE,
                                                                    Map.of(
                                                                            "networkCategory",
                                                                            category));
                                                        }))
                        .isInstanceOf(SynGuidanceProvider.ProviderException.class);
                assertThat(audit).hasSize(1);
                assertThat(sleeps).isEmpty();
            }
        }
        var count = new AtomicInteger();
        var selected = new ArrayList<String>();
        audit.clear();
        sleeps.clear();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
            policy().call(
                            key -> {
                                selected.add(key);
                                if (count.getAndIncrement() == 0)
                                    throw new SynGuidanceProvider.ProviderException(
                                            SynGuidanceProvider.FailureKind
                                                    .PROVIDER_TRANSPORT_DEADLINE);
                                return finalDecision();
                            });
            assertThat(count).hasValue(2);
            assertThat(selected).containsExactly("gemini/primary", "gemini/secondary");
            assertThat(sleeps).isEmpty();
        }
    }

    @Test
    void remainingDeadlineBoundsAttemptAndCancelledBackoffCannotDispatchFallback() {
        var allowed = new AtomicBoolean(true);
        var p =
                new AgentModelFailover(
                        "gemini/primary",
                        CoreValues.tree(Map.of("fallbackModelKeys", List.of("gemini/secondary"))),
                        audit::add,
                        millis -> allowed.set(false),
                        () -> 0);
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(8), allowed::get)) {
            assertThatThrownBy(
                            () ->
                                    p.call(
                                            key -> {
                                                assertThat(
                                                                AgentTransportBudget.bound(
                                                                        Duration.ofSeconds(60)))
                                                        .isLessThan(Duration.ofSeconds(4));
                                                throw remote(503);
                                            }))
                    .isInstanceOfSatisfying(
                            SynGuidanceProvider.ProviderException.class,
                            e ->
                                    assertThat(e.kind())
                                            .isEqualTo(
                                                    SynGuidanceProvider.FailureKind
                                                            .CONSENT_REVOKED));
        }
        assertThat(audit).hasSize(1);
        assertThat(AgentTransportBudget.bound(Duration.ofSeconds(60)))
                .isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void invalidJsonGetsOnlyOneRegenerationAndNeverSavesInvalidOutput() {
        var seen = new ArrayList<String>();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
            var p = policy();
            assertThatThrownBy(
                            () ->
                                    p.call(
                                            key -> {
                                                seen.add(key);
                                                throw new SynGuidanceProvider.ProviderException(
                                                        SynGuidanceProvider.FailureKind
                                                                .MALFORMED_OUTPUT);
                                            }))
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
        }
        assertThat(seen).containsExactly("gemini/primary", "gemini/secondary");
    }

    @Test
    void generationFallbackPreservesObservationsWithoutExecutingConfirmedWriteTwice() {
        var writes = new AtomicInteger();
        var decisions = new AtomicInteger();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(60), () -> true)) {
            var p = policy();
            var output =
                    BoundedNativeExecution.execute(
                            3,
                            1,
                            true,
                            new BoundedNativeExecution.Host() {
                                public void checkpoint() {}

                                public Decision decide(boolean tools, List<Exchange> history) {
                                    if (history.isEmpty())
                                        return new Decision(
                                                null,
                                                List.of(
                                                        new Call(
                                                                "confirmed-operation-id",
                                                                "synthetic.confirmed",
                                                                CoreValues.tree(Map.of()))),
                                                null);
                                    assertThat(history).hasSize(1);
                                    assertThat(history.get(0).observations()).hasSize(1);
                                    return p.call(
                                            key -> {
                                                if (decisions.getAndIncrement() < 2)
                                                    throw remote(503);
                                                return finalDecision();
                                            });
                                }

                                public JsonNode output(Decision d) {
                                    return d.output();
                                }

                                public List<JsonNode> tools(Decision d) {
                                    writes.incrementAndGet();
                                    return List.of(
                                            CoreValues.tree(
                                                    Map.of(
                                                            "operationId",
                                                            "confirmed-operation-id",
                                                            "saved",
                                                            true)));
                                }
                            });
            assertThat(output.path("valid").asBoolean()).isTrue();
            assertThat(writes).hasValue(1);
        }
    }

    @Test
    void schemaFailureRepairsInExistingRoundBudgetWithoutReplayingTools() {
        var tools = new AtomicInteger();
        var outputs = new AtomicInteger();
        var p = policy();
        var output =
                BoundedNativeExecution.execute(
                        4,
                        1,
                        true,
                        new BoundedNativeExecution.Host() {
                            public void checkpoint() {}

                            public Decision decide(boolean allowed, List<Exchange> history) {
                                return history.isEmpty()
                                        ? new Decision(
                                                null,
                                                List.of(
                                                        new Call(
                                                                "one",
                                                                "read",
                                                                CoreValues.tree(Map.of()))),
                                                null)
                                        : finalDecision();
                            }

                            public List<JsonNode> tools(Decision d) {
                                tools.incrementAndGet();
                                return List.of(CoreValues.tree(Map.of("data", "authorized")));
                            }

                            public JsonNode output(Decision d) {
                                if (outputs.getAndIncrement() == 0)
                                    throw new SynGuidanceProvider.ProviderException(
                                            SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
                                return d.output();
                            }

                            public boolean recoverInvalidOutput() {
                                return p.repairInvalidOutput();
                            }
                        });
        assertThat(output.path("valid").asBoolean()).isTrue();
        assertThat(tools).hasValue(1);
        assertThat(outputs).hasValue(2);
    }
}
