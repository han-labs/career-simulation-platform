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
