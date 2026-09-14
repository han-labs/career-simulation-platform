package edu.hcmute.careersim.guidance.dto;

import java.util.List;

public record DashboardResponse(
        String source,
        String studentName,
        Progress progress,
        List<InterestSignal> interestSignals,
        List<SkillSummary> skills,
        List<RecentResult> recentResults,
        CurrentPlan plan) {

    public record Progress(int completed, int total, String label) {}

    public record InterestSignal(String code, String label, int score) {}

    public record SkillSummary(String name, String status, String evidence) {}

    public record RecentResult(
            long id,
            String title,
            int score,
            String completedLabel,
            List<ResultOutcome> outcomes) {}

    public record ResultOutcome(String label, String status) {}

    public record CurrentPlan(String title, String nextStep, String status) {}
}
