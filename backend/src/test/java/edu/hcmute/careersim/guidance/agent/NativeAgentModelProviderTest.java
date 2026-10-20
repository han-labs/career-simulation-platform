package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import edu.hcmute.careersim.guidance.agent.infrastructure.NativeAgentModelProvider;
import edu.hcmute.careersim.guidance.agent.service.AgentModelProvider;
import edu.hcmute.careersim.guidance.service.*;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

class NativeAgentModelProviderTest {
    final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"gemini", "openai"})
    void actualNativeProtocolRoundTripPreservesCorrelationButNotPrivateThoughtInOutput(
            String provider) throws Exception {
        var requests = new ArrayList<JsonNode>();
        var round = new AtomicInteger();
        var server =
                HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    try {
                        var request = json.readTree(exchange.getRequestBody());
                        requests.add(request);
                        boolean first = round.getAndIncrement() == 0;
                        Object body;
                        if (provider.equals("gemini")) {
                            Object parts =
                                    first
                                            ? List.of(
                                                    Map.of(
                                                            "thoughtSignature",
                                                            "synthetic-private-signature",
                                                            "functionCall",
                                                            Map.of(
                                                                    "id",
                                                                    "call-one",
                                                                    "name",
                                                                    "kb_search",
                                                                    "args",
                                                                    Map.of(
                                                                            "query",
                                                                            "synthetic practice"))))
                                            : List.of(
                                                    Map.of(
                                                            "thought",
                                                            true,
                                                            "text",
                                                            "private protocol thought"),
                                                    Map.of(
                                                            "text",
                                                            "{\"schemaVersion\":1,\"outcome\":\"REFUSED\",\"text\":\"No evidence.\",\"citations\":[],\"suggestions\":[]}"));
                            body =
                                    Map.of(
                                            "candidates",
                                            List.of(
                                                    Map.of(
                                                            "content",
                                                            Map.of(
                                                                    "role", "model", "parts",
                                                                    parts))),
                                            "usageMetadata",
                                            Map.of(
                                                    "promptTokenCount",
                                                    12,
                                                    "candidatesTokenCount",
                                                    3,
                                                    "thoughtsTokenCount",
                                                    2));
                        } else {
                            Object message =
                                    first
                                            ? Map.of(
                                                    "role",
                                                    "assistant",
                                                    "tool_calls",
                                                    List.of(
                                                            Map.of(
                                                                    "id",
                                                                    "call-one",
                                                                    "type",
                                                                    "function",
                                                                    "function",
                                                                    Map.of(
                                                                            "name",
                                                                            "kb_search",
                                                                            "arguments",
                                                                            "{\"query\":\"synthetic practice\"}"))))
                                            : Map.of(
                                                    "role",
                                                    "assistant",
                                                    "content",
                                                    "{\"schemaVersion\":1,\"outcome\":\"REFUSED\",\"text\":\"No evidence.\",\"citations\":[],\"suggestions\":[]}");
                            body =
                                    Map.of(
                                            "choices",
                                            List.of(Map.of("message", message)),
                                            "usage",
                                            Map.of("prompt_tokens", 12, "completion_tokens", 5));
                        }
                        byte[] bytes = json.writeValueAsBytes(body);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(10), () -> true)) {
            String url = "http://localhost:" + server.getAddress().getPort();
            var adapter =
                    new NativeAgentModelProvider(
                            json,
                            RestClient.builder(),
                            provider,
                            "synthetic-not-a-credential",
                            "synthetic-not-a-credential",
                            "synthetic",
                            "synthetic",
                            url,
                            url);
            var first =
                    SynExecutionScope.observe(
                            "AGENT_MODEL",
                            () ->
                                    adapter.decide(
                                            provider + "/synthetic",
                                            "Synthetic system",
                                            "Synthetic question",
                                            List.of(),
                                            true));
            assertThat(first.calls())
                    .singleElement()
                    .satisfies(
                            call -> {
                                assertThat(call.id()).isEqualTo("call-one");
                                assertThat(call.name()).isEqualTo("kb.search");
                            });
            var second =
                    SynExecutionScope.observe(
                            "AGENT_MODEL",
                            () ->
                                    adapter.decide(
                                            provider + "/synthetic",
                                            "Synthetic system",
                                            "Synthetic question",
                                            List.of(
                                                    new AgentModelProvider.Exchange(
                                                            first,
                                                            List.of(
                                                                    json.valueToTree(
                                                                            Map.of(
                                                                                    "status",
                                                                                    "SUCCEEDED",
                                                                                    "passages",
                                                                                    List.of()))))),
                                            false));
            assertThat(second.output().path("text").asText()).isEqualTo("No evidence.");
            assertThat(second.output().toString()).doesNotContain("private", "Signature");
            assertThat(requests.get(0).toString()).contains("kb_search");
            assertThat(requests.get(1).toString()).contains("call-one");
            assertThat(requests.get(1).has("tools")).isFalse();
            if (provider.equals("gemini"))
                assertThat(requests.get(1).toString())
                        .contains("thoughtSignature", "functionResponse");
            else assertThat(requests.get(1).toString()).contains("tool_call_id");
            assertThat(SynExecutionScope.trace().toString())
                    .contains("inputTokens=12", "outputTokens=5")
                    .doesNotContain("synthetic-private-signature");
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"gemini", "openai"})
    void disabledProvidersAndOversizedRequestsNeverDispatch(String provider) throws Exception {
        var count = new AtomicInteger();
        var server =
                HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(
                "/",
                e -> {
                    count.incrementAndGet();
                    e.sendResponseHeaders(500, -1);
                    e.close();
                });
        server.start();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(3), () -> true)) {
            String url = "http://localhost:" + server.getAddress().getPort();
            var disabled =
                    new NativeAgentModelProvider(
                            json,
                            RestClient.builder(),
                            "disabled",
                            "",
                            "",
                            "synthetic",
                            "synthetic",
                            url,
                            url);
            assertThatThrownBy(
                            () ->
                                    disabled.decide(
                                            provider + "/synthetic",
                                            "Synthetic",
                                            "Question",
                                            List.of(),
                                            false))
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
            var enabled =
                    new NativeAgentModelProvider(
                            json,
                            RestClient.builder(),
                            provider,
                            "synthetic-only",
                            "synthetic-only",
                            "synthetic",
                            "synthetic",
                            url,
                            url);
            assertThatThrownBy(
                            () ->
                                    enabled.decide(
                                            provider + "/synthetic",
                                            "x".repeat(65000),
                                            "Question",
                                            List.of(),
                                            false))
                    .isInstanceOf(SynGuidanceProvider.ProviderException.class);
            assertThat(count).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"gemini", "openai"})
    void classifiesRateLimitAndRejectsTrailingNonJsonAtRealWireBoundary(String provider)
            throws Exception {
        var count = new AtomicInteger();
        var server =
                HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    try {
                        // Drain the request before closing an empty HTTP response; otherwise
                        // the fixture can reset the socket before the client observes status429.
                        exchange.getRequestBody().readAllBytes();
                        if (count.getAndIncrement() == 0) {
                            exchange.sendResponseHeaders(429, -1);
                            return;
                        }
                        String invalid =
                                "{\"schemaVersion\":1,\"outcome\":\"REFUSED\",\"text\":\"No evidence.\",\"citations\":[],\"suggestions\":[]} trailing";
                        Object body =
                                provider.equals("gemini")
                                        ? Map.of(
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
                                                                                        invalid))))))
                                        : Map.of(
                                                "choices",
                                                List.of(
                                                        Map.of(
                                                                "message",
                                                                Map.of(
                                                                        "role",
                                                                        "assistant",
                                                                        "content",
                                                                        invalid))));
                        byte[] bytes = json.writeValueAsBytes(body);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(10), () -> true)) {
            String url = "http://localhost:" + server.getAddress().getPort();
            var adapter =
                    new NativeAgentModelProvider(
                            json,
                            RestClient.builder(),
                            provider,
                            "synthetic-only",
                            "synthetic-only",
                            "synthetic",
                            "synthetic",
                            url,
                            url);
            for (var expected :
                    List.of(
                            SynGuidanceProvider.FailureKind.PROVIDER_RATE_LIMITED,
                            SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT))
                assertThatThrownBy(
                                () ->
                                        adapter.decide(
                                                provider + "/synthetic",
                                                "Synthetic system",
                                                "Synthetic question",
                                                List.of(),
                                                false))
                        .isInstanceOfSatisfying(
                                SynGuidanceProvider.ProviderException.class,
                                e -> assertThat(e.kind()).isEqualTo(expected));
            assertThat(count).hasValue(2);
        } finally {
            server.stop(0);
        }
    }
}
