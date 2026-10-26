package edu.hcmute.careersim.guidance.knowledge.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.*;

public final class KnowledgeContracts {
    private KnowledgeContracts() {}

    public record Base(
            String id,
            String name,
            String scopeType,
            String visibility,
            String status,
            long publicationRevision,
            long revision,
            Instant createdAt) {}

    public record BaseSummary(
            String id,
            String name,
            String scopeType,
            String visibility,
            String status,
            long publicationRevision,
            long revision,
            Instant createdAt,
            long documentCount,
            long publishedChunkCount) {}

    public record BaseInput(String name, String status) {}

    public record Upload(
            String assetKey,
            String title,
            String license,
            String sourceUri,
            String language,
            String track,
            boolean reviewed,
            UUID requestId) {}

    public record Job(
            String id,
            String knowledgeBaseId,
            String assetKey,
            String uploaderId,
            String sourceFileId,
            String state,
            String producedVersionId,
            String errorCode,
            String parserProfile,
            String embeddingProfile,
            JsonNode metadata,
            JsonNode attempts,
            long revision,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt) {}

    public record AssetVersion(
            String id,
            String documentId,
            String knowledgeBaseId,
            String assetKey,
            int versionNumber,
            String title,
            String status,
            String contentHash,
            String embeddingProfile,
            String mimeType,
            String sourceFileId,
            String sourceUri,
            String license,
            String reviewer,
            String language,
            JsonNode provenance,
            Instant publishedAt,
            List<Passage> chunks) {}

    public record Passage(
            String referenceId,
            String chunkId,
            String versionId,
            String knowledgeBaseId,
            String assetKey,
            String title,
            String text,
            String section,
            JsonNode sourceLocator,
            String sourceUri,
            String embeddingProfile,
            double score,
            String retrievalMethod) {}

    public record Test(
            UUID requestId,
            String query,
            boolean retrievalOnly,
            Integer topK,
            String modelKey,
            UUID configurationId) {}

    public record Confirmation(boolean confirmed) {}
}
