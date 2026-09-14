package edu.hcmute.careersim.guidance.domain;

import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StandardGuidancePolicy {

    public DraftReply createReply(
            String requestedAction,
            String message,
            DashboardResponse dashboard,
            Long requestedAttemptId) {
        String action = classify(requestedAction, message);
        Optional<DashboardResponse.RecentResult> result =
                selectResult(dashboard, requestedAttemptId);
        DashboardResponse.InterestSignal topSignal =
                dashboard.interestSignals().stream().findFirst().orElse(null);
        DashboardResponse.SkillSummary evidenceGap =
                dashboard.skills().stream()
                        .filter(skill -> "NEEDS_MORE_EVIDENCE".equals(skill.status()))
                        .findFirst()
                        .orElse(null);

        return switch (action) {
            case "EXPLAIN_RIASEC" -> explainRiasec(topSignal);
            case "REVIEW_LATEST" -> reviewResult(result.orElse(null));
            case "NEEDS_EVIDENCE" -> explainEvidenceGap(evidenceGap, result.orElse(null));
            case "EXPLORE_NEXT" -> exploreNext();
            case "DRAFT_PLAN" -> draftPlan(topSignal, evidenceGap);
            case "SAFETY_BOUNDARY" -> safetyBoundary();
            default -> generalHelp();
        };
    }

    private String classify(String requestedAction, String message) {
        if (!"FREE_TEXT".equals(requestedAction)) {
            return requestedAction;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        if (requiresSafetyBoundary(message)) {
            return "SAFETY_BOUNDARY";
        }
        if (containsAny(normalized, "plan", "next step")) {
            return "DRAFT_PLAN";
        }
        if (containsAny(normalized, "result", "score", "simulation")) {
            return "REVIEW_LATEST";
        }
        if (containsAny(normalized, "riasec", "interest")) {
            return "EXPLAIN_RIASEC";
        }
        if (containsAny(normalized, "evidence", "practise", "practice", "improve")) {
            return "NEEDS_EVIDENCE";
        }
        return "GENERAL";
    }

    public boolean requiresSafetyBoundary(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return containsAny(
                normalized,
                "ignore previous",
                "ignore all",
                "system prompt",
                "developer message",
                "answer key",
                "evaluation rule",
                "reveal secret",
                "api key",
                "guarantee",
                "choose my career");
    }

    private boolean containsAny(String message, String... phrases) {
        for (String phrase : phrases) {
            if (message.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private Optional<DashboardResponse.RecentResult> selectResult(
            DashboardResponse dashboard, Long requestedAttemptId) {
        if (requestedAttemptId == null) {
            return dashboard.recentResults().stream().findFirst();
        }
        return dashboard.recentResults().stream()
                .filter(result -> result.id() == requestedAttemptId)
                .findFirst();
    }

    private DraftReply explainRiasec(DashboardResponse.InterestSignal topSignal) {
        if (topSignal == null) {
            return new DraftReply(
                    "There is not enough assessment evidence yet. Complete the RIASEC assessment to create a starting point for reflection.",
                    null,
                    List.of("Start the RIASEC assessment", "Explore a short simulation"),
                    null);
        }
        return new DraftReply(
                "Your strongest current signal is "
                        + topSignal.label()
                        + ". Use it as a direction to test through experience, not as a career verdict.",
                null,
                List.of("Compare this signal with a simulation result", "Find another experience"),
                null);
    }

    private DraftReply reviewResult(DashboardResponse.RecentResult result) {
        if (result == null) {
            return new DraftReply(
                    "There is no completed simulation to review yet. Try a short simulation, then return to compare the objective task outcomes.",
                    null,
                    List.of("Browse simulations"),
                    null);
        }
        return new DraftReply(
                "Here is the objective evidence from this simulation. The score and task outcomes come from deterministic evaluation; Syn does not change them.",
                result,
                List.of("Find evidence gaps", "Draft a next-step plan"),
                null);
    }

    private DraftReply explainEvidenceGap(
            DashboardResponse.SkillSummary gap, DashboardResponse.RecentResult result) {
        if (gap == null) {
            return new DraftReply(
                    "The current results do not identify one clear evidence gap. Another focused simulation can still help you compare how different work feels.",
                    result,
                    List.of("Explore another simulation", "Reflect on the latest result"),
                    null);
        }
        return new DraftReply(
                gap.name()
                        + " currently needs more evidence. That is not a judgement of competence; one more focused task would make the comparison more useful.",
                result,
                List.of("Try another focused task", "Review the related outcome"),
                null);
    }

    private DraftReply exploreNext() {
        return new DraftReply(
                "Gather one more piece of evidence before choosing a direction. You could try a different simulation, repeat a weak task type, or record what felt engaging and difficult.",
                null,
                List.of("Browse simulations", "Draft a small plan"),
                null);
    }

    private DraftReply draftPlan(
            DashboardResponse.InterestSignal topSignal, DashboardResponse.SkillSummary gap) {
        String comparison =
                topSignal == null
                        ? "Compare the experience with what interested you most."
                        : "Compare the new evidence with your "
                                + topSignal.label()
                                + " interest signal.";
        String practice =
                gap == null
                        ? "Complete one short simulation in a different IT direction."
                        : "Complete one focused task involving " + gap.name() + ".";
        SynMessageResponse.PlanDraft plan =
                new SynMessageResponse.PlanDraft(
                        "Build one more piece of evidence",
                        List.of(
                                practice,
                                "Note what felt engaging and what felt difficult.",
                                comparison));
        return new DraftReply(
                "I drafted a small exploration plan from the evidence available. Review every step before choosing whether to save it.",
                null,
                List.of(),
                plan);
    }

    private DraftReply safetyBoundary() {
        return new DraftReply(
                "I cannot reveal protected evaluation material, override system rules, guarantee success, or choose a career for you. I can help you reflect on your own completed evidence.",
                null,
                List.of("Explain my interests", "Review my latest result"),
                null);
    }

    private DraftReply generalHelp() {
        return new DraftReply(
                "I can help you understand your RIASEC signals, review completed simulation evidence, identify where more practice may help, or draft a small exploration plan.",
                null,
                List.of(
                        "Explain my RIASEC signals",
                        "Review my latest result",
                        "Draft an exploration plan"),
                null);
    }

    public record DraftReply(
            String text,
            DashboardResponse.RecentResult resultCard,
            List<String> suggestions,
            SynMessageResponse.PlanDraft planDraft) {}
}
