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
    private final String googleKey, openaiKey, googleModel, openaiModel, selected;

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
        this.selected = selected;
        this.googleKey = googleKey;
        this.openaiKey = openaiKey;
        this.googleModel = googleModel;
        this.openaiModel = openaiModel;
        gemini =
                builder.clone()
                        .baseUrl(googleBase)
                        .requestFactory(SynHttpRequests.deadlineAware(12, "gemini"))
                        .build();
        openai =
                builder.clone()
                        .baseUrl(openaiBase)
                        .requestFactory(SynHttpRequests.deadlineAware(12, "openai"))
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

    public Decision decide(
            String key,
            String instructions,
            String query,
            List<Exchange> exchanges,
            boolean search) {
        var model =
                models().stream()
                        .filter(m -> m.key().equals(key) && m.available())
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        failure(
                                                SynGuidanceProvider.FailureKind
                                                        .PROVIDER_UNAVAILABLE));
        try {
            return model.provider().equals("gemini")
                    ? gemini(model, instructions, query, exchanges, search)
                    : openai(model, instructions, query, exchanges, search);
        } catch (SynGuidanceProvider.ProviderException failure) {
            throw failure;
        } catch (RestClientResponseException remote) {
            int code = remote.getStatusCode().value();
            throw failure(
                    code == 429
                            ? SynGuidanceProvider.FailureKind.PROVIDER_RATE_LIMITED
                            : code >= 500
                                    ? SynGuidanceProvider.FailureKind.PROVIDER_UNAVAILABLE
                                    : SynGuidanceProvider.FailureKind.PROVIDER_REJECTED);
        } catch (RestClientException transport) {
            throw failure(SynGuidanceProvider.FailureKind.TIMEOUT_OR_TRANSPORT);
        }
    }

    private Decision gemini(
            Model model,
            String instructions,
            String query,
            List<Exchange> exchanges,
            boolean search) {
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
                                                    parameters())))));
        bounded(body);
        var response =
                gemini.post()
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
            boolean search) {
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
        bounded(body);
        var response =
                openai.post()
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
        return "kb_search".equals(name) ? "kb.search" : name;
    }

    private String wireName(String name) {
        return "kb.search".equals(name) ? "kb_search" : name;
    }

    private SynGuidanceProvider.ProviderException failure(SynGuidanceProvider.FailureKind kind) {
        return new SynGuidanceProvider.ProviderException(kind);
    }
}
