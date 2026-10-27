package edu.hcmute.careersim.guidance.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/**
 * Protected native protocol stays request-scoped, never in public traces or the execution ledger.
 */
public interface AgentModelProvider {
    record Model(String key, String provider, String model, boolean available) {}

    record Call(String id, String name, JsonNode arguments) {}

    record Decision(JsonNode nativeMessage, List<Call> calls, JsonNode output, String modelKey) {
        public Decision(JsonNode nativeMessage, List<Call> calls, JsonNode output) {
            this(nativeMessage, calls, output, null);
        }
    }

    record Exchange(Decision decision, List<JsonNode> observations) {}

    record Tool(String name, String description, JsonNode parameters) {}

    /** Wire schema supplements, never replaces, host evidence and business validation. */
    default Decision streamStructured(
            String model,
            String instructions,
            String query,
            List<Exchange> exchanges,
            List<Tool> tools,
            JsonNode responseSchema,
            java.util.function.Consumer<String> delta) {
        return stream(model, instructions, query, exchanges, tools, delta);
    }

    /** Real deltas when supported; compatibility providers emit no invented stream segments. */
    default Decision stream(
            String model,
            String instructions,
            String query,
            List<Exchange> exchanges,
            List<Tool> tools,
            java.util.function.Consumer<String> delta) {
        return decide(
                model,
                instructions,
                query,
                exchanges,
                tools.stream().anyMatch(t -> t.name().equals("kb.search")));
    }

    default Map<String, Object> state() {
        return Map.of("adapterInstantiated", true);
    }

    default void requireAvailable(String key) {
        var model =
                models().stream()
                        .filter(m -> m.key().equals(key))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new edu.hcmute.careersim.guidance.service
                                                .SynGuidanceProvider.ProviderException(
                                                edu.hcmute.careersim.guidance.service
                                                        .SynGuidanceProvider.FailureKind
                                                        .PROVIDER_MODEL_UNAVAILABLE));
        if (!model.available())
            throw new edu.hcmute.careersim.guidance.service.SynGuidanceProvider.ProviderException(
                    edu.hcmute.careersim.guidance.service.SynGuidanceProvider.FailureKind
                            .PROVIDER_NOT_CONFIGURED);
    }

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
