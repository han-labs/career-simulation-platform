package edu.hcmute.careersim.guidance.agent.domain;

import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.Configuration;
import java.time.Instant;
import java.util.*;

/** Explicit immutable authority snapshot; revalidated at every dispatch/commit/replay. */
public record ExecutionContext(
        UUID executionId,
        long actor,
        String mode,
        Configuration configuration,
        Set<UUID> knowledgeBases,
        String query,
        int topK,
        boolean retrievalOnly,
        String modelKey,
        long privacyVersion,
        Instant deadline) {
    public ExecutionContext {
        knowledgeBases = Set.copyOf(knowledgeBases);
    }
}
