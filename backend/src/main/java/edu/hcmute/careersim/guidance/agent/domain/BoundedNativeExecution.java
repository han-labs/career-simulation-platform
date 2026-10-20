package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.guidance.agent.service.AgentModelProvider.*;
import edu.hcmute.careersim.guidance.service.SynGuidanceProvider;
import java.util.*;

/**
 * Single native-call controller shared by all three modes; hosts supply current authority and
 * persistence.
 */
public final class BoundedNativeExecution {
    private BoundedNativeExecution() {}

    public interface Host {
        void checkpoint();

        Decision decide(boolean tools, List<Exchange> history);

        JsonNode output(Decision decision);

        List<JsonNode> tools(Decision decision);
    }

    public static JsonNode execute(int rounds, int maxCalls, boolean toolsEnabled, Host host) {
        if (rounds < 1 || rounds > 4 || maxCalls < 0 || maxCalls > 8)
            throw new IllegalArgumentException("Invalid execution budget");
        var exchanges = new ArrayList<Exchange>();
        int calls = 0;
        for (int round = 0; round < rounds; round++) {
            host.checkpoint();
            boolean allowed = toolsEnabled && calls < maxCalls && round < rounds - 1;
            var decision = host.decide(allowed, List.copyOf(exchanges));
            host.checkpoint();
            if (decision == null || decision.calls() == null) throw failure();
            if (decision.calls().isEmpty()) return host.output(decision);
            if (!allowed
                    || decision.calls().size() > 8
                    || (calls += decision.calls().size()) > maxCalls) throw failure();
            var ids = new HashSet<String>();
            for (var call : decision.calls())
                if (call == null || call.id() != null && !ids.add(call.id())) throw failure();
            var observations = host.tools(decision);
            if (observations.size() != decision.calls().size()) throw failure();
            exchanges.add(new Exchange(decision, List.copyOf(observations)));
        }
        throw failure();
    }

    private static SynGuidanceProvider.ProviderException failure() {
        return new SynGuidanceProvider.ProviderException(
                SynGuidanceProvider.FailureKind.ITERATION_LIMIT);
    }
}
