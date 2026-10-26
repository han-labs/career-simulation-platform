package edu.hcmute.careersim.guidance.agent.infrastructure;

import com.fasterxml.jackson.databind.*;
import edu.hcmute.careersim.guidance.agent.service.AgentModelProvider;
import edu.hcmute.careersim.guidance.infrastructure.SynHttpRequests;
import edu.hcmute.careersim.guidance.service.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

/** Wire adapter reuses Syn's credentials, bounded HTTP, cancellation, usage and pricing scope. */
@Component
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class NativeAgentModelProvider implements AgentModelProvider {
    private final ObjectMapper json;
    private final RestClient gemini, openai;
    private final String googleKey,
            openaiKey,
            googleModel,
            openaiModel,
            selected,
            googleBase,
            openaiBase;

    public NativeAgentModelProvider(
            ObjectMapper json,
            RestClient.Builder builder,
            @Value("${app.ai.provider:disabled}") String selected,
            @Value("${app.ai.gemini.api-key:}") String googleKey,
            @Value("${app.ai.api-key:}") String openaiKey,
            @Value("${app.ai.gemini.model}") String googleModel,
            @Value("${app.ai.model}") String openaiModel,
            @Value("${app.ai.gemini.base-url}") String googleBase,
            @Value("${app.ai.base-url}") String openaiBase) {
        this.json = json;
        this.googleBase = googleBase;
        this.openaiBase = openaiBase;
        this.selected = selected;
        this.googleKey = googleKey;
        this.openaiKey = openaiKey;
        this.googleModel = googleModel;
        this.openaiModel = openaiModel;
        gemini =
                builder.clone()
                        .baseUrl(googleBase)
                        .requestFactory(SynHttpRequests.agentDeadlineAware("gemini"))
                        .build();
        openai =
                builder.clone()
                        .baseUrl(openaiBase)
                        .requestFactory(SynHttpRequests.agentDeadlineAware("openai"))
                        .build();
    }

    public List<Model> models() {
        return List.of(
                new Model(
                        "gemini/" + googleModel,
                        "gemini",
                        googleModel,
                        "gemini".equals(selected) && !googleKey.isBlank()),
                new Model(
                        "openai/" + openaiModel,
                        "openai",
                        openaiModel,
                        "openai".equals(selected) && !openaiKey.isBlank()));
    }

    private Map<String, Object> parameters() {
        return Map.of(
                "type",
                "object",
                "properties",
                Map.of("query", Map.of("type", "string"), "topK", Map.of("type", "integer")),
                "required",
                List.of("query"),
                "additionalProperties",
                false);
    }

    // Gemini's parameters dialect cannot express additionalProperties. Project a copy;
    // canonical schemas and backend argument validation remain authoritative and strict.
    private JsonNode geminiParameters(Object parameters) {
        JsonNode projected = json.valueToTree(parameters);
        projected = projected.deepCopy();
        normalizeGeminiSchema(projected);
        return projected;
    }

    private void normalizeGeminiSchema(JsonNode schema) {
        if (schema.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) schema)
                    .remove("additionalProperties");
            schema.fields()
                    .forEachRemaining(
                            field -> {
                                if (field.getKey().equals("properties"))
                                    // Property names are not schema keywords.
                                    field.getValue().forEach(this::normalizeGeminiSchema);
                                else normalizeGeminiSchema(field.getValue());
                            });
        } else if (schema.isArray()) schema.forEach(this::normalizeGeminiSchema);
    }

    public Decision decide(
            String key,
            String instructions,
            String query,
            List<Exchange> exchanges,
            boolean search) {
        var model = requireModel(key);
        try {
            return model.provider().equals("gemini")
                    ? gemini(model, instructions, query, exchanges, search, null, null)
                    : openai(model, instructions, query, exchanges, search, null, null);
        } catch (SynGuidanceProvider.ProviderException failure) {
            throw failure;
        } catch (RestClientResponseException remote) {
            throw httpFailure(remote.getStatusCode().value());
        } catch (RestClientException transport) {
            throw transportFailure(SynHttpRequests.networkCategory(transport));
        }
    }

    public Map<String, Object> state() {
        return Map.of(
                "selectedProvider",
                Set.of("gemini", "openai").contains(selected) ? selected : "disabled",
                "adapterInstantiated",
                true,
                "generationEnabled",
                Set.of("gemini", "openai").contains(selected),
                "geminiCredentialConfigured",
                !googleKey.isBlank(),
                "openaiCredentialConfigured",
                !openaiKey.isBlank());
    }

    public void requireAvailable(String key) {
        requireModel(key);
    }

    private Model requireModel(String key) {
        var m =
                models().stream()
                        .filter(x -> x.key().equals(key))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        safeFailure(
                                                SynGuidanceProvider.FailureKind
                                                        .PROVIDER_MODEL_UNAVAILABLE,
                                                false,
                                                Map.of()));
        if (!m.provider().equals(selected))
            throw safeFailure(SynGuidanceProvider.FailureKind.PROVIDER_DISABLED, false, Map.of());
        if (!(m.provider().equals("gemini") ? !googleKey.isBlank() : !openaiKey.isBlank()))
            throw safeFailure(
                    SynGuidanceProvider.FailureKind.PROVIDER_NOT_CONFIGURED, false, Map.of());
        return m;
    }

    private SynGuidanceProvider.ProviderException safeFailure(
            SynGuidanceProvider.FailureKind kind, boolean outbound, Map<String, Object> metadata) {
        var d = new LinkedHashMap<String, Object>(state());
        d.putAll(metadata);
        d.put("outboundAttempted", outbound);
        return new SynGuidanceProvider.ProviderException(kind, d);
    }

    private SynGuidanceProvider.ProviderException httpFailure(int status) {
        var kind =
                status == 401 || status == 403
                        ? SynGuidanceProvider.FailureKind.PROVIDER_AUTH_REJECTED
                        : status == 404
                                ? SynGuidanceProvider.FailureKind.PROVIDER_MODEL_UNAVAILABLE
                                : status == 429
                                        ? SynGuidanceProvider.FailureKind.PROVIDER_RATE_LIMITED
                                        : status >= 500
                                                ? SynGuidanceProvider.FailureKind
                                                        .PROVIDER_HTTP_UNAVAILABLE
                                                : SynGuidanceProvider.FailureKind.PROVIDER_REJECTED;
        return safeFailure(
                kind,
                true,
                Map.of("providerHttpStatus", status, "httpStatusClass", (status / 100) + "xx"));
    }

    private SynGuidanceProvider.ProviderException transportFailure(String category) {
        return safeFailure(
                category.contains("TIMEOUT") || category.equals("TRANSPORT_DEADLINE")
                        ? SynGuidanceProvider.FailureKind.PROVIDER_TRANSPORT_DEADLINE
                        : SynGuidanceProvider.FailureKind.PROVIDER_NETWORK_UNAVAILABLE,
                true,
                Map.of("networkCategory", category));
    }

    private Decision gemini(
            Model model,
            String instructions,
            String query,
            List<Exchange> exchanges,
            boolean search,
            List<Tool> definitions,
            java.util.function.Consumer<String> delta) {
        var contents = new ArrayList<Object>();
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", query))));
        for (var x : exchanges) {
            contents.add(x.decision().nativeMessage());
            var responses = new ArrayList<Object>();
            for (int i = 0; i < x.decision().calls().size(); i++) {
                var c = x.decision().calls().get(i);
                var response = new LinkedHashMap<String, Object>();
                response.put("name", wireName(c.name()));
                response.put("response", x.observations().get(i));
                if (c.id() != null) response.put("id", c.id());
                responses.add(Map.of("functionResponse", response));
            }
            contents.add(Map.of("role", "user", "parts", responses));
        }
        var body = new LinkedHashMap<String, Object>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", instructions))));
        body.put("contents", contents);
        body.put("generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", 2048));
        if (search)
            body.put(
                    "tools",
                    List.of(
                            Map.of(
                                    "functionDeclarations",
                                    List.of(
                                            Map.of(
                                                    "name",
                                                    "kb_search",
                                                    "description",
                                                    "Retrieve passages from explicitly granted published knowledge bases. No answer generation or writes.",
                                                    "parameters",
                                                    geminiParameters(parameters()))))));
        if (definitions != null) {
            body.remove("tools");
            if (!definitions.isEmpty())
                body.put(
                        "tools",
                        List.of(
                                Map.of(
                                        "functionDeclarations",
                                        definitions.stream()
                                                .map(
                                                        t ->
                                                                Map.of(
                                                                        "name",
                                                                        wireName(t.name()),
                                                                        "description",
                                                                        t.description(),
                                                                        "parameters",
                                                                        geminiParameters(
                                                                                t.parameters())))
                                                .toList())));
        }
        bounded(body);
        var response =
                delta != null
                        ? streamed(model, body, delta)
                        : gemini.post()
                                .uri("/models/{model}:generateContent", model.model())
                                .header("x-goog-api-key", googleKey)
                                .body(body)
                                .retrieve()
                                .body(JsonNode.class);
        if (response == null) throw failure(SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
        var usage = response.path("usageMetadata");
        SynExecutionScope.recordUsage(
                "gemini",
                model.model(),
                usage.path("promptTokenCount").numberValue(),
                SynExecutionScope.outputWithThinking(
                        usage.path("candidatesTokenCount").numberValue(),
                        usage.path("thoughtsTokenCount").numberValue()));
        var content = response.path("candidates").path(0).path("content");
        var calls = new ArrayList<Call>();
        var text = new StringBuilder();
        for (var part : content.path("parts")) {
            if (part.has("functionCall")) {
                var call = part.path("functionCall");
                calls.add(
                        new Call(
                                call.has("id") ? call.path("id").asText() : null,
                                canonical(call.path("name").asText()),
                                call.path("args")));
            } else if (part.has("text") && !part.path("thought").asBoolean())
                text.append(part.path("text").asText());
        }
        return new Decision(
                content.deepCopy(),
                List.copyOf(calls),
                calls.isEmpty() ? output(text.toString()) : null);
    }

    private Decision openai(
            Model model,
            String instructions,
            String query,
            List<Exchange> exchanges,
            boolean search,
            List<Tool> definitions,
            java.util.function.Consumer<String> delta) {
        var messages = new ArrayList<Object>();
        messages.add(Map.of("role", "system", "content", instructions));
        messages.add(Map.of("role", "user", "content", query));
        for (var x : exchanges) {
            messages.add(x.decision().nativeMessage());
            for (int i = 0; i < x.decision().calls().size(); i++)
                messages.add(
                        Map.of(
                                "role",
                                "tool",
                                "tool_call_id",
                                x.decision().calls().get(i).id(),
                                "content",
                                x.observations().get(i).toString()));
        }
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model.model());
        body.put("messages", messages);
        body.put("max_completion_tokens", 2048);
        if (search)
            body.put(
                    "tools",
                    List.of(
                            Map.of(
                                    "type",
                                    "function",
                                    "function",
                                    Map.of(
                                            "name",
                                            "kb_search",
                                            "description",
                                            "Read explicitly granted published knowledge passages, without writes.",
                                            "parameters",
                                            parameters()))));
        if (definitions != null) {
            body.remove("tools");
            if (!definitions.isEmpty())
                body.put(
                        "tools",
                        definitions.stream()
                                .map(
                                        t ->
                                                Map.of(
                                                        "type",
                                                        "function",
                                                        "function",
                                                        Map.of(
                                                                "name",
                                                                wireName(t.name()),
                                                                "description",
                                                                t.description(),
                                                                "parameters",
                                                                t.parameters())))
                                .toList());
        }
        if (delta != null) {
            body.put("stream", true);
            body.put("stream_options", Map.of("include_usage", true));
        }
        bounded(body);
        var response =
                delta != null
                        ? streamed(model, body, delta)
                        : openai.post()
                                .uri("/chat/completions")
                                .header("Authorization", "Bearer " + openaiKey)
                                .body(body)
                                .retrieve()
                                .body(JsonNode.class);
        if (response == null) throw failure(SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
        var usage = response.path("usage");
        SynExecutionScope.recordUsage(
                "openai",
                model.model(),
                usage.path("prompt_tokens").numberValue(),
                usage.path("completion_tokens").numberValue());
        var message = response.path("choices").path(0).path("message");
        var calls = new ArrayList<Call>();
        for (var call : message.path("tool_calls")) {
            if (!"function".equals(call.path("type").asText()) || !call.path("id").isTextual())
                throw failure(SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
            calls.add(
                    new Call(
                            call.path("id").asText(),
                            canonical(call.path("function").path("name").asText()),
                            output(call.path("function").path("arguments").asText())));
        }
        return new Decision(
                message.deepCopy(),
                List.copyOf(calls),
                calls.isEmpty() ? output(message.path("content").asText()) : null);
    }

    public Decision stream(
            String key,
            String instructions,
            String query,
            List<Exchange> exchanges,
            List<Tool> tools,
            java.util.function.Consumer<String> delta) {
        var model = requireModel(key);
        try {
            return model.provider().equals("gemini")
                    ? gemini(model, instructions, query, exchanges, false, tools, delta)
                    : openai(model, instructions, query, exchanges, false, tools, delta);
        } catch (SynGuidanceProvider.ProviderException failure) {
            var d = failure.diagnostics();
            if (d.get("providerHttpStatus") instanceof Integer status) throw httpFailure(status);
            if (d.get("networkCategory") instanceof String category)
                throw transportFailure(category);
            throw failure;
        } catch (RestClientResponseException remote) {
            throw httpFailure(remote.getStatusCode().value());
        } catch (RestClientException transport) {
            throw transportFailure(SynHttpRequests.networkCategory(transport));
        }
    }

    private JsonNode streamed(Model model, Object body, java.util.function.Consumer<String> delta) {
        var text = new StringBuilder();
        var parts = json.createArrayNode();
        var calls = new TreeMap<Integer, com.fasterxml.jackson.databind.node.ObjectNode>();
        var usage =
                new java.util.concurrent.atomic.AtomicReference<JsonNode>(json.createObjectNode());
        boolean google = model.provider().equals("gemini");
        String url =
                (google ? googleBase : openaiBase).replaceAll("/$", "")
                        + (google
                                ? "/models/" + model.model() + ":streamGenerateContent?alt=sse"
                                : "/chat/completions");
        try {
            SynHttpRequests.stream(
                    java.net.URI.create(url),
                    model.provider(),
                    google
                            ? Map.of("x-goog-api-key", googleKey)
                            : Map.of("Authorization", "Bearer " + openaiKey),
                    json.writeValueAsBytes(body),
                    raw -> {
                        JsonNode e;
                        try {
                            e = json.readTree(raw);
                        } catch (Exception invalid) {
                            throw failure(SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
                        }
                        if (e.has("error"))
                            throw httpFailure(e.path("error").path("code").asInt(400));
                        if (google) {
                            if (e.has("usageMetadata")) usage.set(e.path("usageMetadata"));
                            for (var p :
                                    e.path("candidates").path(0).path("content").path("parts")) {
                                if (p.has("text") && !p.path("thought").asBoolean()) {
                                    String d = p.path("text").asText();
                                    text.append(d);
                                    delta.accept(d);
                                } else if (p.has("functionCall") || p.has("thoughtSignature"))
                                    parts.add(p.deepCopy());
                            }
                        } else {
                            if (e.hasNonNull("usage")) usage.set(e.path("usage"));
                            var d = e.path("choices").path(0).path("delta");
                            if (d.path("content").isTextual()) {
                                String value = d.path("content").asText();
                                text.append(value);
                                delta.accept(value);
                            }
                            for (var c : d.path("tool_calls")) {
                                int index = c.path("index").asInt();
                                if (index < 0 || index > 7)
                                    throw failure(SynGuidanceProvider.FailureKind.ITERATION_LIMIT);
                                var target =
                                        calls.computeIfAbsent(
                                                index,
                                                i ->
                                                        json.createObjectNode()
                                                                .put("id", "")
                                                                .put("type", "function")
                                                                .set(
                                                                        "function",
                                                                        json.createObjectNode()
                                                                                .put("name", "")
                                                                                .put(
                                                                                        "arguments",
                                                                                        "")));
                                if (c.has("id"))
                                    target.put(
                                            "id",
                                            target.path("id").asText() + c.path("id").asText());
                                var f =
                                        (com.fasterxml.jackson.databind.node.ObjectNode)
                                                target.path("function");
                                if (c.path("function").has("name"))
                                    f.put(
                                            "name",
                                            f.path("name").asText()
                                                    + c.path("function").path("name").asText());
                                if (c.path("function").has("arguments"))
                                    f.put(
                                            "arguments",
                                            f.path("arguments").asText()
                                                    + c.path("function")
                                                            .path("arguments")
                                                            .asText());
                            }
                        }
                        if (text.length() > 16000)
                            throw failure(SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
                    });
        } catch (java.io.IOException e) {
            throw failure(SynGuidanceProvider.FailureKind.TIMEOUT_OR_TRANSPORT);
        }
        if (google) {
            if (!text.isEmpty()) parts.addObject().put("text", text.toString());
            return json.createObjectNode()
                    .<com.fasterxml.jackson.databind.node.ObjectNode>set(
                            "usageMetadata", usage.get())
                    .set(
                            "candidates",
                            json.createArrayNode()
                                    .add(
                                            json.createObjectNode()
                                                    .set(
                                                            "content",
                                                            json.createObjectNode()
                                                                    .put("role", "model")
                                                                    .set("parts", parts))));
        }
        var message =
                json.createObjectNode().put("role", "assistant").put("content", text.toString());
        if (!calls.isEmpty()) message.set("tool_calls", json.valueToTree(calls.values()));
        return json.createObjectNode()
                .<com.fasterxml.jackson.databind.node.ObjectNode>set("usage", usage.get())
                .set(
                        "choices",
                        json.createArrayNode()
                                .add(json.createObjectNode().set("message", message)));
    }

    private void bounded(Object body) {
        try {
            if (json.writeValueAsBytes(body).length > 64000)
                throw failure(SynGuidanceProvider.FailureKind.ITERATION_LIMIT);
        } catch (java.io.IOException invalid) {
            throw failure(SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
        }
    }

    private JsonNode output(String text) {
        try {
            if (text == null
                    || text.isBlank()
                    || text.getBytes(StandardCharsets.UTF_8).length > 16000)
                throw new IllegalArgumentException();
            return json.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(text);
        } catch (Exception malformed) {
            throw failure(SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
        }
    }

    private String canonical(String name) {
        return Set.of(
                                "kb_search",
                                "assessment_lookup",
                                "simulation_explore",
                                "simulation_myEvidence")
                        .contains(name)
                ? name.replaceFirst("_", ".")
                : name;
    }

    private String wireName(String name) {
        return name.replace('.', '_');
    }

    private SynGuidanceProvider.ProviderException failure(SynGuidanceProvider.FailureKind kind) {
        return new SynGuidanceProvider.ProviderException(kind);
    }
}
