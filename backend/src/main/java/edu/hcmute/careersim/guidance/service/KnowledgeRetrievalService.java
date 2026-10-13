package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.domain.KnowledgeChunk;
import java.util.List;
import java.util.Optional;

public interface KnowledgeRetrievalService {
    Retrieval retrieve(String query, boolean externalConsent);

    Optional<KnowledgeChunk> published(long id);

    record Retrieval(String status, List<KnowledgeChunk> chunks) {
        public Retrieval {
            chunks = List.copyOf(chunks);
        }
    }
}
