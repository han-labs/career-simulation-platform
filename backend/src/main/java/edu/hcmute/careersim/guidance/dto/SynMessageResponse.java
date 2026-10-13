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
        List<SimulationCard> simulationCards,
        DashboardResponse.RecentResult resultCard,
        List<DashboardResponse.RecentResult> resultCards,
        List<String> suggestions,
        PlanDraft planDraft,
        String reasonCode,
        edu.hcmute.careersim.guidance.service.WebSearchProvider.Result webSearch) {
    public SynMessageResponse(
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
            List<SimulationCard> simulationCards,
            DashboardResponse.RecentResult resultCard,
            List<DashboardResponse.RecentResult> resultCards,
            List<String> suggestions,
            PlanDraft planDraft,
            String reasonCode) {
        this(
                id,
                sessionId,
                intent,
                responseLanguage,
                explorationStage,
                text,
                provenance,
                activity,
                evidenceReferences,
                comparisonCard,
                resourceCards,
                simulationCards,
                resultCard,
                resultCards,
                suggestions,
                planDraft,
                reasonCode,
                null);
    }

    public SynMessageResponse withWebSearch(
            edu.hcmute.careersim.guidance.service.WebSearchProvider.Result web) {
        return new SynMessageResponse(
                id,
                sessionId,
                intent,
                responseLanguage,
                explorationStage,
                text,
                provenance,
                activity,
                evidenceReferences,
                comparisonCard,
                resourceCards,
                simulationCards,
                resultCard,
                resultCards,
                suggestions,
                planDraft,
                reasonCode,
                web);
    }

    public SynMessageResponse(
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
            List<SimulationCard> simulationCards,
            DashboardResponse.RecentResult resultCard,
            List<DashboardResponse.RecentResult> resultCards,
            List<String> suggestions,
            PlanDraft planDraft) {
        this(
                id,
                sessionId,
                intent,
                responseLanguage,
                explorationStage,
                text,
                provenance,
                activity,
                evidenceReferences,
                comparisonCard,
                resourceCards,
                simulationCards,
                resultCard,
                resultCards,
                suggestions,
                planDraft,
                null);
    }

    public SynMessageResponse withTurn(String turnId, String reason) {
        return new SynMessageResponse(
                turnId,
                sessionId,
                intent,
                responseLanguage,
                explorationStage,
                text,
                provenance,
                activity,
                evidenceReferences,
                comparisonCard,
                resourceCards,
                simulationCards,
                resultCard,
                resultCards,
                suggestions,
                planDraft,
                reason,
                webSearch);
    }

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
                List.of(),
                resultCard,
                resultCard == null ? List.of() : List.of(resultCard),
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
                List.of(),
                resultCard,
                resultCard == null ? List.of() : List.of(resultCard),
                suggestions,
                planDraft);
    }

    public record PlanDraft(String title, List<String> steps) {}

    public record EvidenceReference(
            String label,
            String sourceType,
            String referenceId,
            String version,
            String section,
            String url,
            String excerpt) {
        public EvidenceReference(String label, String sourceType) {
            this(label, sourceType, null, null, null, null, null);
        }
    }

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

    public record SimulationCard(
            String title,
            String careerTrack,
            String summary,
            String difficulty,
            int estimatedMinutes) {

        public SimulationCard(String title, String careerTrack) {
            this(title, careerTrack, "", "", 0);
        }
    }
}
