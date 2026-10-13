package edu.hcmute.careersim.guidance.dto;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        String source,
        String studentName,
        Progress progress,
        List<InterestSignal> interestSignals,
        List<SkillSummary> skills,
        List<RecentResult> recentResults,
        CurrentPlan plan,
        Long assessmentAttemptId) {

    public DashboardResponse(
            String source,
            String studentName,
            Progress progress,
            List<InterestSignal> interestSignals,
            List<SkillSummary> skills,
            List<RecentResult> recentResults,
            CurrentPlan plan) {
        this(source, studentName, progress, interestSignals, skills, recentResults, plan, null);
    }

    public record Progress(int completed, int total, String label) {}

    public record InterestSignal(String code, String label, int score) {}

    public record SkillSummary(String name, String status, String evidence) {}

    public record RecentResult(
            long id,
            String title,
            int score,
            String completedLabel,
            List<ResultOutcome> outcomes,
            Instant completedAt,
            String careerTrack) {

        public RecentResult(
                long id,
                String title,
                int score,
                String completedLabel,
                List<ResultOutcome> outcomes,
                Instant completedAt) {
            this(id, title, score, completedLabel, outcomes, completedAt, null);
        }

        public RecentResult(
                long id,
                String title,
                int score,
                String completedLabel,
                List<ResultOutcome> outcomes) {
            this(id, title, score, completedLabel, outcomes, null, null);
        }
    }

    public record ResultOutcome(String label, String status) {}

    public record CurrentPlan(String title, String nextStep, String status) {}
}
