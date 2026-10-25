package edu.hcmute.careersim.guidance.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

class GeminiEmbeddingProviderTest {
    @Test
    void usesSeparateQueryTaskAndValidatesAndNormalizes768Dimensions() throws Exception {
        var mapper = new ObjectMapper();
        var values = new double[768];
        values[0] = 3;
        values[1] = 4;
        var response =
                new AtomicReference<>(
                        mapper.writeValueAsBytes(
                                Map.of(
                                        "embedding",
                                        Map.of("values", values),
                                        "usageMetadata",
                                        Map.of("promptTokenCount", 12))));
        var received = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/models/gemini-embedding-001:embedContent",
                exchange -> {
                    received.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    java.nio.charset.StandardCharsets.UTF_8));
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    byte[] body = response.get();
                    exchange.sendResponseHeaders(200, body.length);
                    try {
                        exchange.getResponseBody().write(body);
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try (var scope =
                edu.hcmute.careersim.guidance.service.SynExecutionScope.open(
                        java.time.Duration.ofSeconds(20), () -> true)) {
            var provider =
                    new GeminiEmbeddingProvider(
                            RestClient.builder(),
                            true,
                            "synthetic-key",
                            "http://127.0.0.1:" + server.getAddress().getPort());
            var query = provider.embed("Synthetic SQL query", true);
            assertThat(query).hasSize(768);
            assertThat(query[0]).isEqualTo(.6);
            assertThat(query[1]).isEqualTo(.8);
            assertThat(received.get()).contains("RETRIEVAL_QUERY", "768");
            var usage =
                    (List<?>)
                            edu.hcmute.careersim.guidance.service.SynExecutionScope.trace()
                                    .get("tokenUsage");
            assertThat(usage).hasSize(1);
            assertThat(usage.get(0).toString())
                    .contains(
                            "QUERY_EMBEDDING",
                            "gemini-embedding-001",
                            "inputTokens=12",
                            "outputTokens=0");
            provider.embed("Synthetic SQL document", false);
            assertThat(received.get()).contains("RETRIEVAL_DOCUMENT");
            response.set(
                    mapper.writeValueAsBytes(Map.of("embedding", Map.of("values", List.of(1, 2)))));
            assertThatThrownBy(() -> provider.embed("fixture", true))
                    .isInstanceOf(IllegalStateException.class);
            response.set(
                    mapper.writeValueAsBytes(
                            Map.of("embedding", Map.of("values", new double[768]))));
            assertThatThrownBy(() -> provider.embed("fixture", true))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void classifiesProviderRejectionWithoutExposingItsBodyOrCredentials() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/models/gemini-embedding-001:embedContent",
                exchange -> {
                    exchange.getRequestBody().readAllBytes();
                    byte[] body =
                            "sensitive-provider-detail-must-not-escape"
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(403, body.length);
                    try {
                        exchange.getResponseBody().write(body);
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try (var scope =
                edu.hcmute.careersim.guidance.service.SynExecutionScope.open(
                        Duration.ofSeconds(10), () -> true)) {
            var provider =
                    new GeminiEmbeddingProvider(
                            RestClient.builder(),
                            true,
                            "synthetic-secret",
                            "http://127.0.0.1:" + server.getAddress().getPort(),
                            5);
            assertThatThrownBy(() -> provider.embed("Synthetic text", false))
                    .isInstanceOf(
                            edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                    .ProviderException.class)
                    .hasMessage("PROVIDER_REJECTED")
                    .hasNoCause();
        } finally {
            server.stop(0);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SYN_EMBEDDING_LIVE_TEST", matches = "true")
    void liveProviderProducesNormalizedDocumentAndQueryVectors() {
        var provider =
                new GeminiEmbeddingProvider(
                        RestClient.builder(),
                        true,
                        System.getenv().getOrDefault("GEMINI_API_KEY", ""),
                        "https://generativelanguage.googleapis.com/v1beta");
        assertThat(provider.available()).isTrue();
        try {
            for (boolean query : List.of(false, true)) {
                var vector =
                        provider.embed(
                                query
                                        ? "Why use an idempotency key?"
                                        : "An idempotency key identifies a repeated API request.",
                                query);
                assertThat(vector).hasSize(768);
                assertThat(Arrays.stream(vector).map(v -> v * v).sum())
                        .isCloseTo(1.0, within(0.000001));
            }
        } catch (RuntimeException error) {
            throw new AssertionError(
                    "Live embedding unavailable: " + error.getClass().getSimpleName());
        }
    }
}
