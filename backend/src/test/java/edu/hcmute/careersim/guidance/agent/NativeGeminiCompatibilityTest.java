package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.infrastructure.NativeAgentModelProvider;
import edu.hcmute.careersim.guidance.agent.service.AgentModelProvider.*;
import edu.hcmute.careersim.guidance.service.*;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

class NativeGeminiCompatibilityTest {
    final ObjectMapper json = new ObjectMapper();

    @Test
    void twoRoundPreviewExplicitlyClosesFunctionsAfterRealObservation() throws Exception {
        var requests = new ArrayList<JsonNode>();
        var executed = new java.util.concurrent.atomic.AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                e -> {
                    JsonNode request = json.readTree(e.getRequestBody());
                    requests.add(request);
                    // Reproduce a model continuing native tool use from history unless explicitly
                    // closed.
                    boolean closed =
                            request.at("/toolConfig/functionCallingConfig/mode")
                                    .asText()
                                    .equals("NONE");
                    Object part =
                            closed
                                    ? Map.of(
                                            "text",
                                            "{\"schemaVersion\":1,\"outcome\":\"ANSWER\",\"text\":\"Compare the supplied evidence.\",\"citations\":[{\"referenceId\":\"K1\",\"quote\":\"Backend builds APIs.\"}],\"suggestions\":[]}")
                                    : Map.of(
                                            "thoughtSignature",
                                            "synthetic-signature",
                                            "functionCall",
                                            Map.of(
                                                    "id",
                                                    "lookup",
                                                    "name",
                                                    "kb_search",
                                                    "args",
                                                    Map.of("query", "compare roles")));
                    byte[] body =
                            json.writeValueAsBytes(
                                    Map.of(
                                            "candidates",
                                            List.of(
                                                    Map.of(
                                                            "content",
                                                            Map.of(
                                                                    "role",
                                                                    "model",
                                                                    "parts",
                                                                    List.of(part))))));
                    e.getResponseHeaders().set("Content-Type", "application/json");
                    e.sendResponseHeaders(200, body.length);
                    e.getResponseBody().write(body);
                    e.close();
                });
        server.start();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(10), () -> true)) {
            var provider = adapter("http://127.0.0.1:" + server.getAddress().getPort());
            var result =
                    edu.hcmute.careersim.guidance.agent.domain.BoundedNativeExecution.execute(
                            2,
                            8,
                            true,
                            new edu.hcmute.careersim.guidance.agent.domain.BoundedNativeExecution
                                    .Host() {
                                public void checkpoint() {}

                                public Decision decide(boolean allowed, List<Exchange> history) {
                                    return provider.decide(
                                            "gemini/gemini-3.5-flash-lite",
                                            "Platform security",
                                            "Compare two roles",
                                            history,
                                            allowed);
                                }

                                public JsonNode output(Decision d) {
                                    return d.output();
                                }

                                public List<JsonNode> tools(Decision d) {
                                    executed.incrementAndGet();
                                    return List.of(
                                            CoreValues.tree(
                                                    Map.of(
                                                            "referenceId",
                                                            "K1",
                                                            "text",
                                                            "Backend builds APIs.")));
                                }
                            });
            assertThat(result.path("outcome").asText()).isEqualTo("ANSWER");
            assertThat(executed).hasValue(1);
            assertThat(requests).hasSize(2);
            assertThat(requests.get(0).has("tools")).isTrue();
            assertThat(requests.get(0).has("toolConfig")).isFalse();
            assertThat(requests.get(1).has("tools")).isFalse();
            assertThat(requests.get(1).at("/toolConfig/functionCallingConfig/mode").asText())
                    .isEqualTo("NONE");
            assertThat(requests.get(1).at("/generationConfig/responseMimeType").asText())
                    .isEqualTo("application/json");
            assertThat(requests.get(1).path("systemInstruction").toString())
                    .contains("No further tool calls", "INSUFFICIENT_EVIDENCE");
            assertThat(requests.get(1).path("contents").toString())
                    .contains("synthetic-signature", "functionResponse", "Backend builds APIs.");
            assertThat(result.toString()).doesNotContain("synthetic-signature");
        } finally {
            server.stop(0);
        }
    }

    NativeAgentModelProvider adapter(String url) {
        return new NativeAgentModelProvider(
                json,
                RestClient.builder(),
                "gemini",
                "synthetic-test-only",
                "",
                "legacy",
                "synthetic",
                url,
                url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite"})
    void modernStableModelsUseCompatibleGenerateContentRequestAndStrictJson(String name)
            throws Exception {
        var request = new AtomicReference<JsonNode>();
        var path = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                e -> {
                    path.set(e.getRequestURI().getPath());
                    request.set(json.readTree(e.getRequestBody().readAllBytes()));
                    byte[] body =
                            json.writeValueAsBytes(
                                    Map.of(
                                            "candidates",
                                            List.of(
                                                    Map.of(
                                                            "content",
                                                            Map.of(
                                                                    "role",
                                                                    "model",
                                                                    "parts",
                                                                    List.of(
                                                                            Map.of(
                                                                                    "text",
                                                                                    "{\"schemaVersion\":1,\"outcome\":\"ANSWER\",\"text\":\"Synthetic answer\",\"citations\":[],\"suggestions\":[]}")))))));
                    e.getResponseHeaders().set("Content-Type", "application/json");
                    e.sendResponseHeaders(200, body.length);
                    e.getResponseBody().write(body);
                    e.close();
                });
        server.start();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(10), () -> true)) {
            var a = adapter("http://127.0.0.1:" + server.getAddress().getPort());
            assertThat(a.models().stream().map(Model::key))
                    .contains("gemini/" + name, "gemini/legacy");
            var decision =
                    a.streamStructured(
                            "gemini/" + name,
                            "Safe fixture instructions",
                            "Synthetic query",
                            List.of(),
                            List.of(
                                    new Tool(
                                            "kb.search",
                                            "Read synthetic passages",
                                            CoreValues.tree(
                                                    Map.of(
                                                            "type",
                                                            "object",
                                                            "properties",
                                                            Map.of(
                                                                    "query",
                                                                    Map.of("type", "string")),
                                                            "required",
                                                            List.of("query"))))),
                            edu.hcmute.careersim.guidance.agent.domain.AgentResponseSchema.forTask(
                                    "CHAT", CoreValues.tree(Map.of("maxCharacters", 4000))),
                            null);
            assertThat(decision.output().path("text").asText()).isEqualTo("Synthetic answer");
            assertThat(path.get()).isEqualTo("/models/" + name + ":generateContent");
            assertThat(request.get().path("generationConfig").has("temperature")).isFalse();
            assertThat(
                            request.get()
                                    .path("generationConfig")
                                    .path("thinkingConfig")
                                    .path("thinkingLevel")
                                    .asText())
                    .isEqualTo("LOW");
            assertThat(request.get().path("generationConfig").path("responseMimeType").asText())
                    .isEqualTo("application/json");
            assertThat(request.get().path("tools").size()).isEqualTo(1);
            assertThat(
                            request.get()
                                    .path("generationConfig")
                                    .path("responseJsonSchema")
                                    .path("properties")
                                    .path("suggestions")
                                    .path("maxItems")
                                    .asInt())
                    .isEqualTo(3);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fallbackPreservesAuthorizedObservationButNeverReplaysForeignModelSignature()
            throws Exception {
        var request = new AtomicReference<JsonNode>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                e -> {
                    request.set(json.readTree(e.getRequestBody().readAllBytes()));
                    byte[] b =
                            json.writeValueAsBytes(
                                    Map.of(
                                            "candidates",
                                            List.of(
                                                    Map.of(
                                                            "content",
                                                            Map.of(
                                                                    "parts",
                                                                    List.of(
                                                                            Map.of(
                                                                                    "text",
                                                                                    "{\"schemaVersion\":1,\"outcome\":\"REFUSED\",\"text\":\"Synthetic abstention\",\"citations\":[],\"suggestions\":[]}")))))));
                    e.getResponseHeaders().set("Content-Type", "application/json");
                    e.sendResponseHeaders(200, b.length);
                    e.getResponseBody().write(b);
                    e.close();
                });
        server.start();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(10), () -> true)) {
            var prior =
                    new Decision(
                            CoreValues.tree(
                                    Map.of(
                                            "role",
                                            "model",
                                            "parts",
                                            List.of(
                                                    Map.of(
                                                            "thoughtSignature",
                                                            "FOREIGN_SIGNATURE",
                                                            "functionCall",
                                                            Map.of(
                                                                    "name",
                                                                    "kb_search",
                                                                    "args",
                                                                    Map.of("query", "guide")))))),
                            List.of(
                                    new Call(
                                            "same-call-id",
                                            "kb.search",
                                            CoreValues.tree(Map.of("query", "guide")))),
                            null,
                            "gemini/gemini-3.6-flash");
            var observation =
                    CoreValues.tree(
                            Map.of("referenceId", "K1", "text", "Approved synthetic passage"));
            adapter("http://127.0.0.1:" + server.getAddress().getPort())
                    .decide(
                            "gemini/gemini-3.5-flash-lite",
                            "Fixture",
                            "Question",
                            List.of(new Exchange(prior, List.of(observation))),
                            false);
            assertThat(request.get().toString())
                    .doesNotContain("FOREIGN_SIGNATURE", "functionResponse")
                    .contains("K1", "Approved synthetic passage");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void providerSafetyRefusalIsValidRefusalNeverAvailabilityFailure() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                e -> {
                    e.getRequestBody().readAllBytes();
                    byte[] b = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}".getBytes();
                    e.getResponseHeaders().set("Content-Type", "application/json");
                    e.sendResponseHeaders(200, b.length);
                    e.getResponseBody().write(b);
                    e.close();
                });
        server.start();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(10), () -> true)) {
            var result =
                    adapter("http://127.0.0.1:" + server.getAddress().getPort())
                            .decide(
                                    "gemini/gemini-3.6-flash",
                                    "Fixture",
                                    "Unsafe request",
                                    List.of(),
                                    false);
            assertThat(result.output().path("outcome").asText()).isEqualTo("REFUSED");
            assertThat(result.calls()).isEmpty();
        } finally {
            server.stop(0);
        }
    }
}
