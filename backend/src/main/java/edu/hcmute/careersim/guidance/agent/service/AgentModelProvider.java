package edu.hcmute.careersim.guidance.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/**
 * Protected native protocol stays request-scoped, never in public traces or the execution ledger.
 */
public interface AgentModelProvider {
    record Model(String key, String provider, String model, boolean available) {}

    record Call(String id, String name, JsonNode arguments) {}

    record Decision(JsonNode nativeMessage, List<Call> calls, JsonNode output) {}

    record Exchange(Decision decision, List<JsonNode> observations) {}

    List<Model> models();

    default Set<String> modelKeys() {
        return models().stream().map(Model::key).collect(java.util.stream.Collectors.toSet());
    }

    Decision decide(
            String modelKey,
            String instructions,
            String query,
            List<Exchange> exchanges,
            boolean kbSearch);
}
