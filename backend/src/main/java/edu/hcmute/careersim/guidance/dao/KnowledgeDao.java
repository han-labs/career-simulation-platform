package edu.hcmute.careersim.guidance.dao;

import edu.hcmute.careersim.guidance.domain.KnowledgeChunk;
import java.util.List;
import java.util.Optional;

public interface KnowledgeDao {
    List<KnowledgeChunk> vector(double[] vector, String profile, int limit, double minimum);

    List<KnowledgeChunk> lexical(String query, int limit);

    Optional<KnowledgeChunk> published(long id);

    long publish(Document doc, String hash, String profile, List<EmbeddedChunk> chunks);

    void revoke(String sourceKey);

    record Document(
            String sourceKey,
            String title,
            String sourceUrl,
            String license,
            String language,
            String track,
            String reviewer,
            boolean reviewed,
            String content) {}

    record EmbeddedChunk(String section, String text, double[] vector) {}
}
