package edu.hcmute.careersim.guidance.dto;

import java.util.List;

public record SynMessageResponse(
        String id,
        String text,
        String provenance,
        DashboardResponse.RecentResult resultCard,
        List<String> suggestions,
        PlanDraft planDraft) {

    public record PlanDraft(String title, List<String> steps) {}
}
