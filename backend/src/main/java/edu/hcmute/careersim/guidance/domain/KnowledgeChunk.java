package edu.hcmute.careersim.guidance.domain;

public record KnowledgeChunk(
        long id,
        long versionId,
        String title,
        String section,
        String content,
        String language,
        String sourceUrl,
        double score,
        String sourceKey) {
    public KnowledgeChunk(
            long id,
            long versionId,
            String title,
            String section,
            String content,
            String language,
            String sourceUrl,
            double score) {
        this(id, versionId, title, section, content, language, sourceUrl, score, null);
    }

    public String referenceId() {
        return "K" + id;
    }
}
