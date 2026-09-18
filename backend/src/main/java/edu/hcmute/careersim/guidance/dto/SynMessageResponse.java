package edu.hcmute.careersim.guidance.dto;

import java.util.List;
import java.util.UUID;

public record SynMessageResponse(
        String id,
        UUID sessionId,
        String intent,
        String responseLanguage,
        String explorationStage,
        String text,
        String provenance,
        List<String> activity,
        List<EvidenceReference> evidenceReferences,
        ComparisonCard comparisonCard,
        List<ResourceCard> resourceCards,
        DashboardResponse.RecentResult resultCard,
        List<String> suggestions,
        PlanDraft planDraft) {

    public SynMessageResponse(
            String id,
            UUID sessionId,
            String intent,
            String text,
            String provenance,
            List<String> activity,
            List<EvidenceReference> evidenceReferences,
            ComparisonCard comparisonCard,
            List<ResourceCard> resourceCards,
            DashboardResponse.RecentResult resultCard,
            List<String> suggestions,
            PlanDraft planDraft) {
        this(
                id,
                sessionId,
                intent,
                "EN",
                "EVIDENCE_READY",
                text,
                provenance,
                activity,
                evidenceReferences,
                comparisonCard,
                resourceCards,
                resultCard,
                suggestions,
                planDraft);
    }

    public SynMessageResponse(
            String id,
            String text,
            String provenance,
            DashboardResponse.RecentResult resultCard,
            List<String> suggestions,
            PlanDraft planDraft) {
        this(
                id,
                null,
                "GENERAL",
                "EN",
                "NEW",
                text,
                provenance,
                List.of(),
                List.of(),
                null,
                List.of(),
                resultCard,
                suggestions,
                planDraft);
    }

    public record PlanDraft(String title, List<String> steps) {}

    public record EvidenceReference(String label, String sourceType) {}

    public record ComparisonCard(String title, List<PathComparison> paths) {}

    public record PathComparison(
            String code, String title, String evidenceSummary, String nextExperiment) {}

    public record ResourceCard(
            String title,
            String provider,
            String url,
            String skill,
            String difficulty,
            int estimatedMinutes) {}
}
