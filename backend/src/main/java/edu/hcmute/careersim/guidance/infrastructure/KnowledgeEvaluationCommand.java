package edu.hcmute.careersim.guidance.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.dao.KnowledgeDao;
import edu.hcmute.careersim.guidance.domain.*;
import edu.hcmute.careersim.guidance.service.EmbeddingProvider;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Opt-in ablation runner. It never publishes documents or calls the generation model. */
@Component
@ConditionalOnProperty(name = "app.knowledge.eval-file")
public class KnowledgeEvaluationCommand implements ApplicationRunner {
    private final KnowledgeDao dao;
    private final EmbeddingProvider embeddings;
    private final ObjectMapper mapper;
    private final String input;
    private final String output;
    private final double minimum;

    public KnowledgeEvaluationCommand(
            KnowledgeDao dao,
            EmbeddingProvider embeddings,
            ObjectMapper mapper,
            @Value("${app.knowledge.eval-file}") String input,
            @Value("${app.knowledge.eval-output:rag-evaluation.json}") String output,
            @Value("${app.knowledge.minimum-similarity:0.5}") double minimum) {
        this.dao = dao;
        this.embeddings = embeddings;
        this.mapper = mapper;
        this.input = input;
        this.output = output;
        this.minimum = minimum;
    }

    public void run(ApplicationArguments args) throws Exception {
        if (!embeddings.available())
            throw new IllegalStateException(
                    "Vector evaluation requires an enabled embedding provider.");
        var path = Path.of(input);
        if (Files.size(path) > 2_000_000)
            throw new IllegalArgumentException("Evaluation file too large");
        var cases = mapper.readTree(Files.readString(path));
        if (!cases.isArray() || cases.isEmpty() || cases.size() > 200)
            throw new IllegalArgumentException("Expected 1–200 reviewed cases.");
        for (var c : cases)
            if (!c.path("reviewed").asBoolean(false)
                    || !c.path("query").isTextual()
                    || c.path("query").textValue().length() > 600
                    || !c.path("expectedSources").isArray())
                throw new IllegalArgumentException(
                        "Review the evaluation cases before running; candidate labels are not gold evidence.");
        var rows = new ArrayList<Map<String, Object>>();
        for (var c : cases) {
            String query = c.path("query").textValue();
            List<String> expected = new ArrayList<>();
            c.path("expectedSources").forEach(v -> expected.add(v.asText()));
            long started = System.nanoTime();
            var vector =
                    dao.vector(embeddings.embed(query, true), embeddings.profile(), 20, minimum);
            var lexical = dao.lexical(HybridRetrievalPolicy.query(query), 20);
            var modes =
                    Map.of(
                            "lexical",
                            lexical.stream().limit(5).toList(),
                            "vector",
                            vector.stream().limit(5).toList(),
                            "hybrid",
                            HybridRetrievalPolicy.merge(vector, lexical));
            for (var entry : modes.entrySet()) {
                var chunks = entry.getValue();
                var retrieved = chunks.stream().map(KnowledgeChunk::sourceKey).distinct().toList();
                var row = new LinkedHashMap<String, Object>();
                row.put("id", c.path("id").asText());
                row.put("group", c.path("group").asText());
                row.put("mode", entry.getKey());
                row.put("expectedSources", expected);
                row.put("retrievedSources", retrieved);
                row.put("chunkIds", chunks.stream().map(KnowledgeChunk::referenceId).toList());
                row.put(
                        "versions",
                        chunks.stream().map(KnowledgeChunk::versionId).distinct().toList());
                row.put(
                        "sourceRecallAt5",
                        expected.isEmpty()
                                ? null
                                : expected.stream().filter(retrieved::contains).count()
                                        / (double) expected.size());
                row.put("emptyRetrieval", chunks.isEmpty());
                row.put("combinedRetrievalMs", (System.nanoTime() - started) / 1_000_000);
                rows.add(row);
            }
        }
        var report =
                Map.of(
                        "createdAt",
                        java.time.Instant.now().toString(),
                        "embeddingProfile",
                        embeddings.profile(),
                        "minimumSimilarity",
                        minimum,
                        "scope",
                        "Retrieval ablation only; source-level labels. Citation support, answer correctness and adversarial generation require separate human review.",
                        "rows",
                        rows);
        Files.writeString(
                Path.of(output),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report) + "\n");
    }
}
