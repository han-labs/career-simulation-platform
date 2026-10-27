package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.BoundedNativeExecution;
import edu.hcmute.careersim.guidance.agent.service.AgentModelProvider.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BoundedNativeExecutionTest {
    @Test
    void studentReservesOneRepairWithinFourRoundsWithoutReplayingTools() {
        var flags = new ArrayList<Boolean>();
        var executed = new AtomicInteger();
        var outputs = new AtomicInteger();
        var repairs = new AtomicInteger();
        var host =
                new BoundedNativeExecution.Host() {
                    public void checkpoint() {}

                    public boolean reserveOutputRepair() {
                        return true;
                    }

                    public Decision decide(boolean allowed, List<Exchange> history) {
                        flags.add(allowed);
                        assertThat(history).hasSize(Math.min(flags.size() - 1, 2));
                        return allowed
                                ? new Decision(
                                        null,
                                        List.of(
                                                new Call(
                                                        "call" + flags.size(),
                                                        "assessment.lookup",
                                                        CoreValues.tree(Map.of()))),
                                        null)
                                : new Decision(
                                        null, List.of(), CoreValues.tree(Map.of("accepted", true)));
                    }

                    public JsonNode output(Decision d) {
                        if (outputs.incrementAndGet() == 1)
                            throw new edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                    .ProviderException(
                                    edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                            .FailureKind.MALFORMED_OUTPUT);
                        return d.output();
                    }

                    public boolean recoverInvalidOutput() {
                        return repairs.incrementAndGet() == 1;
                    }

                    public List<JsonNode> tools(Decision d) {
                        executed.incrementAndGet();
                        return List.of(CoreValues.tree(Map.of("status", "SUCCEEDED")));
                    }
                };
        assertThat(BoundedNativeExecution.execute(4, 4, true, host).path("accepted").asBoolean())
                .isTrue();
        assertThat(flags).containsExactly(true, true, false, false);
        assertThat(executed).hasValue(2);
        assertThat(repairs).hasValue(1);
    }

    @Test
    void finalRoundDisablesToolsAndBudgetRejectsWithoutExecutingSideEffects() {
        var dispatched = new AtomicInteger();
        var rounds = new AtomicInteger();
        var host =
                new BoundedNativeExecution.Host() {
                    public void checkpoint() {}

                    public Decision decide(boolean tools, List<Exchange> history) {
                        rounds.incrementAndGet();
                        return new Decision(
                                CoreValues.tree(Map.of()),
                                List.of(
                                        new Call(
                                                "x",
                                                "assessment.lookup",
                                                CoreValues.tree(Map.of()))),
                                null);
                    }

                    public JsonNode output(Decision d) {
                        throw new AssertionError();
                    }

                    public List<JsonNode> tools(Decision d) {
                        dispatched.incrementAndGet();
                        return List.of(CoreValues.tree(Map.of("status", "SUCCEEDED")));
                    }
                };
        assertThatThrownBy(() -> BoundedNativeExecution.execute(2, 8, true, host))
                .isInstanceOf(RuntimeException.class);
        assertThat(dispatched).hasValue(1);
        assertThat(rounds).hasValue(2);
    }

    @Test
    void noEnabledToolsCannotDispatchEvenIfModelInventsCalls() {
        var dispatched = new AtomicInteger();
        var host =
                new BoundedNativeExecution.Host() {
                    public void checkpoint() {}

                    public Decision decide(boolean allowed, List<Exchange> history) {
                        assertThat(allowed).isFalse();
                        return new Decision(
                                null,
                                List.of(new Call("x", "write.private", CoreValues.tree(Map.of()))),
                                null);
                    }

                    public JsonNode output(Decision d) {
                        return null;
                    }

                    public List<JsonNode> tools(Decision d) {
                        dispatched.incrementAndGet();
                        return List.of();
                    }
                };
        assertThatThrownBy(() -> BoundedNativeExecution.execute(3, 8, false, host))
                .isInstanceOf(RuntimeException.class);
        assertThat(dispatched).hasValue(0);
    }
}
