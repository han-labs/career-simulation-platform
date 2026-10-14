package edu.hcmute.careersim.guidance.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.guidance.service.EmbeddingProvider;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {
    private final RestClient client;
    private final String key;
    private final boolean enabled;

    public GeminiEmbeddingProvider(
            RestClient.Builder builder,
            @Value("${app.knowledge.embedding-enabled:false}") boolean enabled,
            @Value("${app.ai.gemini.api-key:}") String key,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
                    String base) {
        this.enabled = enabled;
        this.key = key;
        client =
                builder.clone()
                        .baseUrl(base)
                        .requestFactory(SynHttpRequests.deadlineAware(3, "gemini"))
                        .build();
    }

    public boolean available() {
        return enabled && !key.isBlank();
    }

    public String profile() {
        return "gemini-embedding-001:768:retrieval-v1";
    }

    public double[] embed(String text, boolean query) {
        if (!available()) throw new IllegalStateException("Embedding is disabled.");
        return edu.hcmute.careersim.guidance.service.SynExecutionScope.observe(
                query ? "QUERY_EMBEDDING" : "DOCUMENT_EMBEDDING",
                () -> requestEmbedding(text, query));
    }

    private double[] requestEmbedding(String text, boolean query) {
        JsonNode response =
                client.post()
                        .uri("/models/gemini-embedding-001:embedContent")
                        .header("x-goog-api-key", key)
                        .body(
                                Map.of(
                                        "model",
                                        "models/gemini-embedding-001",
                                        "content",
                                        Map.of("parts", List.of(Map.of("text", text))),
                                        "taskType",
                                        query ? "RETRIEVAL_QUERY" : "RETRIEVAL_DOCUMENT",
                                        "outputDimensionality",
                                        768))
                        .retrieve()
                        .body(JsonNode.class);
        if (response != null)
            edu.hcmute.careersim.guidance.service.SynExecutionScope.recordUsage(
                    "gemini",
                    "gemini-embedding-001",
                    response.path("usageMetadata").path("promptTokenCount").numberValue(),
                    0L);
        if (response == null
                || !response.path("embedding").path("values").isArray()
                || response.path("embedding").path("values").size() != 768)
            throw new IllegalStateException("Invalid embedding dimension.");
        double[] values = new double[768];
        double norm = 0;
        for (int i = 0; i < 768; i++) {
            JsonNode value = response.path("embedding").path("values").get(i);
            if (!value.isNumber()) throw new IllegalStateException("Invalid embedding value.");
            values[i] = value.doubleValue();
            if (!Double.isFinite(values[i]))
                throw new IllegalStateException("Non-finite embedding.");
            norm += values[i] * values[i];
        }
        if (norm <= 0 || !Double.isFinite(norm))
            throw new IllegalStateException("Invalid embedding norm.");
        norm = Math.sqrt(norm);
        for (int i = 0; i < 768; i++) values[i] /= norm;
        return values;
    }
}
