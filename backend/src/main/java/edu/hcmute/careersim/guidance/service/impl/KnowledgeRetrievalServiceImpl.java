package edu.hcmute.careersim.guidance.service.impl;

import edu.hcmute.careersim.guidance.dao.KnowledgeDao;
import edu.hcmute.careersim.guidance.domain.HybridRetrievalPolicy;
import edu.hcmute.careersim.guidance.domain.KnowledgeChunk;
import edu.hcmute.careersim.guidance.service.*;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {
    private final KnowledgeDao dao;
    private final EmbeddingProvider embedding;
    private final double minimum;

    public KnowledgeRetrievalServiceImpl(
            KnowledgeDao dao,
            EmbeddingProvider embedding,
            @Value("${app.knowledge.minimum-similarity:0.5}") double minimum) {
        this.dao = dao;
        this.embedding = embedding;
        this.minimum = minimum;
    }

    public Retrieval retrieve(String message, boolean externalConsent) {
        var cached = SynExecutionScope.cachedRetrieval(message);
        if (cached != null) return cached;
        // No student name/ID/score is appended. Remove emails, URLs and standalone numbers.
        String safe =
                message.replaceAll("(?i)https?://\\S+|[\\w.+-]+@[\\w.-]+|\\b\\d+\\b", " ").trim();
        List<KnowledgeChunk> vector = List.of();
        String status = "LEXICAL_ONLY";
        if (externalConsent && embedding.available()) {
            try {
                vector = dao.vector(embedding.embed(safe, true), embedding.profile(), 20, minimum);
                status = "HYBRID";
            } catch (RuntimeException failure) {
                status = "EMBEDDING_UNAVAILABLE";
            }
        }
        List<KnowledgeChunk> lexical = dao.lexical(HybridRetrievalPolicy.query(safe), 20);
        var chunks = HybridRetrievalPolicy.merge(vector, lexical);
        var result = new Retrieval(chunks.isEmpty() ? "NO_RELEVANT_KNOWLEDGE" : status, chunks);
        SynExecutionScope.cacheRetrieval(message, result);
        return result;
    }

    public Optional<KnowledgeChunk> published(long id) {
        return dao.published(id);
    }
}
