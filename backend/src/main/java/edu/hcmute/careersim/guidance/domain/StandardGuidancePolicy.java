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
        return createReply(requestedAction, message, dashboard, requestedAttemptId, "EN");
    }

    public DraftReply createReply(
            String requestedAction,
            String message,
            DashboardResponse dashboard,
            Long requestedAttemptId,
            String responseLanguage) {
        String action = classify(requestedAction, message);
        boolean vi = "VI".equals(responseLanguage);
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
            case "GET_STARTED" -> getStarted(vi);
            case "EXPLAIN_RIASEC" -> explainRiasec(topSignal, vi);
            case "REVIEW_LATEST" -> reviewResult(result.orElse(null), vi);
            case "NEEDS_EVIDENCE" -> explainEvidenceGap(evidenceGap, result.orElse(null), vi);
            case "EXPLORE_NEXT" -> exploreNext(vi);
            case "DRAFT_PLAN" -> draftPlan(topSignal, evidenceGap, vi);
            case "SAFETY_BOUNDARY" -> safetyBoundary(vi);
            default -> generalHelp(vi);
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
                "choose my career",
                "bỏ qua hướng dẫn",
                "bo qua huong dan",
                "tiết lộ đáp án",
                "tiet lo dap an",
                "tiết lộ api key",
                "tiet lo api key",
                "chọn nghề thay tôi",
                "chon nghe thay toi");
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

    private DraftReply getStarted(boolean vi) {
        return new DraftReply(
                vi
                        ? "Bạn chưa cần phải biết ngay mình hợp nghề nào. Syn có thể giúp bạn bắt đầu bằng một bài RIASEC ngắn hoặc một mô phỏng công việc IT. Đây chỉ là hai cách thu thập trải nghiệm ban đầu, không phải bài kiểm tra quyết định nghề nghiệp."
                        : "You do not need to know your best-fit career yet. Syn can help you start with a short RIASEC assessment or one IT work simulation. Both are ways to gather first-hand evidence, not tests that decide your career.",
                null,
                vi
                        ? List.of("Bắt đầu bài RIASEC", "Khám phá một mô phỏng ngắn")
                        : List.of("Start the RIASEC assessment", "Explore a short simulation"),
                null);
    }

    private DraftReply explainRiasec(DashboardResponse.InterestSignal topSignal, boolean vi) {
        if (topSignal == null) {
            return new DraftReply(
                    vi
                            ? "Chưa có đủ dữ liệu đánh giá. Bạn có thể hoàn thành bài RIASEC để tạo một điểm bắt đầu cho việc tự nhìn lại sở thích của mình."
                            : "There is not enough assessment evidence yet. Complete the RIASEC assessment to create a starting point for reflection.",
                    null,
                    vi
                            ? List.of("Bắt đầu bài RIASEC", "Khám phá một mô phỏng ngắn")
                            : List.of("Start the RIASEC assessment", "Explore a short simulation"),
                    null);
        }
        return new DraftReply(
                vi
                        ? "Tín hiệu nổi bật hiện tại của bạn là "
                                + topSignal.label()
                                + ". Hãy xem đây là một hướng để thử qua trải nghiệm, không phải kết luận nghề nghiệp."
                        : "Your strongest current signal is "
                                + topSignal.label()
                                + ". Use it as a direction to test through experience, not as a career verdict.",
                null,
                vi
                        ? List.of("So sánh với kết quả mô phỏng", "Tìm thêm một trải nghiệm")
                        : List.of(
                                "Compare this signal with a simulation result",
                                "Find another experience"),
                null);
    }

    private DraftReply reviewResult(DashboardResponse.RecentResult result, boolean vi) {
        if (result == null) {
            return new DraftReply(
                    vi
                            ? "Bạn chưa có mô phỏng hoàn thành để xem lại. Hãy thử một mô phỏng ngắn rồi quay lại so sánh các kết quả nhiệm vụ khách quan."
                            : "There is no completed simulation to review yet. Try a short simulation, then return to compare the objective task outcomes.",
                    null,
                    vi ? List.of("Xem các mô phỏng") : List.of("Browse simulations"),
                    null);
        }
        return new DraftReply(
                vi
                        ? "Đây là bằng chứng khách quan từ mô phỏng. Điểm và kết quả từng nhiệm vụ được chấm theo quy tắc xác định; Syn không thay đổi chúng."
                        : "Here is the objective evidence from this simulation. The score and task outcomes come from deterministic evaluation; Syn does not change them.",
                result,
                vi
                        ? List.of("Tìm phần cần thêm bằng chứng", "Phác thảo bước tiếp theo")
                        : List.of("Find evidence gaps", "Draft a next-step plan"),
                null);
    }

    private DraftReply explainEvidenceGap(
            DashboardResponse.SkillSummary gap, DashboardResponse.RecentResult result, boolean vi) {
        if (gap == null) {
            return new DraftReply(
                    vi
                            ? "Kết quả hiện tại chưa chỉ ra một khoảng trống bằng chứng rõ ràng. Một mô phỏng có trọng tâm khác vẫn có thể giúp bạn so sánh cảm nhận về từng loại công việc."
                            : "The current results do not identify one clear evidence gap. Another focused simulation can still help you compare how different work feels.",
                    result,
                    vi
                            ? List.of("Thử mô phỏng khác", "Nhìn lại kết quả gần nhất")
                            : List.of("Explore another simulation", "Reflect on the latest result"),
                    null);
        }
        return new DraftReply(
                vi
                        ? gap.name()
                                + " hiện cần thêm bằng chứng. Đây không phải đánh giá năng lực; thêm một nhiệm vụ có trọng tâm sẽ giúp việc so sánh hữu ích hơn."
                        : gap.name()
                                + " currently needs more evidence. That is not a judgement of competence; one more focused task would make the comparison more useful.",
                result,
                vi
                        ? List.of("Thử thêm nhiệm vụ có trọng tâm", "Xem lại kết quả liên quan")
                        : List.of("Try another focused task", "Review the related outcome"),
                null);
    }

    private DraftReply exploreNext(boolean vi) {
        return new DraftReply(
                vi
                        ? "Hãy thu thập thêm một bằng chứng trước khi chọn hướng. Bạn có thể thử mô phỏng khác, lặp lại một dạng nhiệm vụ còn chưa chắc chắn, hoặc ghi lại điều gì khiến bạn hứng thú và thấy khó."
                        : "Gather one more piece of evidence before choosing a direction. You could try a different simulation, repeat a weak task type, or record what felt engaging and difficult.",
                null,
                vi
                        ? List.of("Xem các mô phỏng", "Phác thảo kế hoạch nhỏ")
                        : List.of("Browse simulations", "Draft a small plan"),
                null);
    }

    private DraftReply draftPlan(
            DashboardResponse.InterestSignal topSignal,
            DashboardResponse.SkillSummary gap,
            boolean vi) {
        String comparison =
                topSignal == null
                        ? (vi
                                ? "So sánh trải nghiệm với điều khiến bạn hứng thú nhất."
                                : "Compare the experience with what interested you most.")
                        : (vi
                                ? "So sánh bằng chứng mới với tín hiệu sở thích "
                                        + topSignal.label()
                                        + "."
                                : "Compare the new evidence with your "
                                        + topSignal.label()
                                        + " interest signal.");
        String practice =
                gap == null
                        ? (vi
                                ? "Hoàn thành một mô phỏng ngắn ở hướng IT khác."
                                : "Complete one short simulation in a different IT direction.")
                        : (vi
                                ? "Hoàn thành một nhiệm vụ có trọng tâm về " + gap.name() + "."
                                : "Complete one focused task involving " + gap.name() + ".");
        SynMessageResponse.PlanDraft plan =
                new SynMessageResponse.PlanDraft(
                        vi ? "Thu thập thêm một bằng chứng" : "Build one more piece of evidence",
                        List.of(
                                practice,
                                vi
                                        ? "Ghi lại điều gì khiến bạn hứng thú và điều gì gây khó khăn."
                                        : "Note what felt engaging and what felt difficult.",
                                comparison));
        return new DraftReply(
                vi
                        ? "Syn đã phác thảo một kế hoạch khám phá nhỏ từ dữ liệu hiện có. Hãy xem lại từng bước trước khi quyết định lưu."
                        : "I drafted a small exploration plan from the evidence available. Review every step before choosing whether to save it.",
                null,
                List.of(),
                plan);
    }

    private DraftReply safetyBoundary(boolean vi) {
        return new DraftReply(
                vi
                        ? "Syn không thể tiết lộ nội dung đánh giá được bảo vệ, bỏ qua quy tắc hệ thống, đảm bảo thành công hoặc chọn nghề thay bạn. Syn có thể giúp bạn nhìn lại bằng chứng do chính bạn tạo ra."
                        : "I cannot reveal protected evaluation material, override system rules, guarantee success, or choose a career for you. I can help you reflect on your own completed evidence.",
                null,
                vi
                        ? List.of("Giải thích sở thích của tôi", "Xem kết quả gần nhất")
                        : List.of("Explain my interests", "Review my latest result"),
                null);
    }

    private DraftReply generalHelp(boolean vi) {
        return new DraftReply(
                vi
                        ? "Syn có thể giúp bạn hiểu tín hiệu RIASEC, xem lại bằng chứng từ mô phỏng, nhận ra phần cần trải nghiệm thêm hoặc phác thảo một kế hoạch khám phá nhỏ."
                        : "I can help you understand your RIASEC signals, review completed simulation evidence, identify where more practice may help, or draft a small exploration plan.",
                null,
                vi
                        ? List.of(
                                "Giải thích tín hiệu RIASEC",
                                "Xem kết quả gần nhất",
                                "Phác thảo kế hoạch khám phá")
                        : List.of(
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
