package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import edu.hcmute.careersim.guidance.agent.domain.AgentTextSegments;
import edu.hcmute.careersim.guidance.agent.infrastructure.NativeAgentModelProvider;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.guidance.service.SynExecutionScope;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

class NativeStudentStreamingTest {
    final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"gemini", "openai"})
    void realWireSseDeliversSemanticTextBeforeEofAndRetainsNativeToolCorrelation(String provider)
            throws Exception {
        var observed = new CountDownLatch(1);
        var early = new AtomicBoolean();
        var requests = new ArrayList<JsonNode>();
        var round = new AtomicInteger();
        var server =
                HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(
                "/",
                e -> {
                    try {
                        requests.add(json.readTree(e.getRequestBody()));
                        e.getResponseHeaders().set("Content-Type", "text/event-stream");
                        e.sendResponseHeaders(200, 0);
                        var out = e.getResponseBody();
                        if (round.getAndIncrement() == 0) {
                            Object event;
                            if (provider.equals("gemini"))
                                event =
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
                                                                                        "thoughtSignature",
                                                                                        "opaque-fixture",
                                                                                        "functionCall",
                                                                                        Map.of(
                                                                                                "id",
                                                                                                "call-one",
                                                                                                "name",
                                                                                                "assessment_lookup",
                                                                                                "args",
                                                                                                Map
                                                                                                        .of())))))));
                            else
                                event =
                                        Map.of(
                                                "choices",
                                                List.of(
                                                        Map.of(
                                                                "delta",
                                                                Map.of(
                                                                        "tool_calls",
                                                                        List.of(
                                                                                Map.of(
                                                                                        "index",
                                                                                        0,
                                                                                        "id",
                                                                                        "call-one",
                                                                                        "function",
                                                                                        Map.of(
                                                                                                "name",
                                                                                                "assessment_lookup",
                                                                                                "arguments",
                                                                                                "{}")))))));
                            out.write(
                                    ("data: " + json.writeValueAsString(event) + "\n\n")
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            out.flush();
                        } else {
                            String prefix =
                                    "{\"schemaVersion\":1,\"outcome\":\"REFUSED\",\"text\":\"First safe sentence. ";
                            String suffix =
                                    "Second sentence.\",\"citations\":[],\"suggestions\":[]}";
                            Object first =
                                    provider.equals("gemini")
                                            ? Map.of(
                                                    "candidates",
                                                    List.of(
                                                            Map.of(
                                                                    "content",
                                                                    Map.of(
                                                                            "parts",
                                                                            List.of(
                                                                                    Map.of(
                                                                                            "text",
                                                                                            prefix))))))
                                            : Map.of(
                                                    "choices",
                                                    List.of(
                                                            Map.of(
                                                                    "delta",
                                                                    Map.of("content", prefix))));
                            out.write(
                                    ("data: " + json.writeValueAsString(first) + "\n\n")
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            out.flush();
                            early.set(observed.await(3, TimeUnit.SECONDS));
                            Object last =
                                    provider.equals("gemini")
                                            ? Map.of(
                                                    "candidates",
                                                    List.of(
                                                            Map.of(
                                                                    "content",
                                                                    Map.of(
                                                                            "parts",
                                                                            List.of(
                                                                                    Map.of(
                                                                                            "text",
                                                                                            suffix))))),
                                                    "usageMetadata",
                                                    Map.of(
                                                            "promptTokenCount",
                                                            20,
                                                            "candidatesTokenCount",
                                                            10))
                                            : Map.of(
                                                    "choices",
                                                    List.of(
                                                            Map.of(
                                                                    "delta",
                                                                    Map.of("content", suffix))),
                                                    "usage",
                                                    Map.of(
                                                            "prompt_tokens",
                                                            20,
                                                            "completion_tokens",
                                                            10));
                            out.write(
                                    ("data: "
                                                    + json.writeValueAsString(last)
                                                    + "\n\ndata: [DONE]\n\n")
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            out.flush();
                        }
                        out.close();
                    } catch (Exception failure) {
                        e.close();
                    }
                });
        server.start();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(10), () -> true)) {
            String base = "http://localhost:" + server.getAddress().getPort();
            var model =
                    new NativeAgentModelProvider(
                            json,
                            RestClient.builder(),
                            provider,
                            "synthetic-key",
                            "synthetic-key",
                            "synthetic",
                            "synthetic",
                            base,
                            base);
            var definitions = StudentEvidenceTools.definitions(StudentEvidenceTools.NAMES);
            var first =
                    model.stream(
                            provider + "/synthetic",
                            "Synthetic instructions",
                            "Synthetic request",
                            List.of(),
                            definitions,
                            d -> {});
            assertThat(first.calls()).hasSize(1);
            assertThat(first.calls().get(0).name()).isEqualTo("assessment.lookup");
            assertThat(first.calls().get(0).id()).isEqualTo("call-one");
            var text = new StringBuilder();
            var segments =
                    new AgentTextSegments(
                            s -> {
                                text.append(s);
                                observed.countDown();
                            });
            var last =
                    SynExecutionScope.observe(
                            "AGENT_MODEL",
                            () ->
                                    model.stream(
                                            provider + "/synthetic",
                                            "Synthetic instructions",
                                            "Synthetic request",
                                            List.of(
                                                    new AgentModelProvider.Exchange(
                                                            first,
                                                            List.of(
                                                                    json.createObjectNode()
                                                                            .put(
                                                                                    "status",
                                                                                    "SUCCEEDED")))),
                                            List.of(),
                                            segments::accept));
            assertThat(early).isTrue();
            assertThat(text.toString()).contains("First safe sentence.");
            assertThat(last.output().path("text").asText())
                    .isEqualTo("First safe sentence. Second sentence.");
            assertThat(last.output().toString()).doesNotContain("opaque-fixture");
            assertThat(requests.get(0).toString())
                    .contains(
                            "assessment_lookup",
                            "simulation_explore",
                            "simulation_myEvidence",
                            "kb_search");
            for (int i = 0; i < definitions.size(); i++) {
                var canonical = definitions.get(i).parameters();
                var transported =
                        provider.equals("gemini")
                                ? requests.get(0)
                                        .at("/tools/0/functionDeclarations/" + i + "/parameters")
                                : requests.get(0).at("/tools/" + i + "/function/parameters");
                JsonNode expected = canonical.deepCopy();
                if (provider.equals("gemini"))
                    ((com.fasterxml.jackson.databind.node.ObjectNode) expected)
                            .remove("additionalProperties");
                assertThat(transported).isEqualTo(expected);
                assertThat(canonical.has("additionalProperties")).isTrue();
                assertThat(canonical.path("additionalProperties").asBoolean()).isFalse();
            }
            assertThat(requests.get(1).toString()).contains("call-one");
            if (provider.equals("gemini"))
                assertThat(requests.get(1).toString()).contains("opaque-fixture");
            assertThat(SynExecutionScope.trace().toString())
                    .contains("20", "10")
                    .doesNotContain("synthetic-key", "opaque-fixture");
        } finally {
            server.stop(0);
        }
    }
}
