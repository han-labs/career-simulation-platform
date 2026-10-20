package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Current-run authorized Core projection. History and user statements are not objective evidence.
 */
public record DomainEvidence(
        String referenceId,
        String kind,
        String access,
        String versionId,
        String text,
        JsonNode data) {}
