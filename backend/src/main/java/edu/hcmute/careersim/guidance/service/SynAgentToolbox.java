package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dao.SynAgentDao;
import edu.hcmute.careersim.guidance.domain.StandardGuidancePolicy;
import edu.hcmute.careersim.guidance.domain.SynAgentPolicy;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Executes the small, server-owned tool set selected by {@link SynAgentPolicy}. */
@Component
public class SynAgentToolbox {

    private final SynAgentDao synAgentDao;
    private final StandardGuidancePolicy standardGuidancePolicy;

    public SynAgentToolbox(SynAgentDao synAgentDao, StandardGuidancePolicy standardGuidancePolicy) {
        this.synAgentDao = synAgentDao;
        this.standardGuidancePolicy = standardGuidancePolicy;
    }

    public ToolResult execute(
            SynAgentPolicy.Decision decision,
            String message,
            DashboardResponse dashboard,
            Long requestedAttemptId) {
        List<String> paths = resolvedPaths(decision, dashboard);
        List<String> activity = new ArrayList<>();
        List<SynMessageResponse.EvidenceReference> references = new ArrayList<>();

        if (decision.tools().contains("READ_EVIDENCE")) {
            activity.add("Reviewed available assessment and simulation evidence");
            dashboard.interestSignals().stream()
                    .findFirst()
                    .ifPresent(
                            signal ->
                                    references.add(
                                            new SynMessageResponse.EvidenceReference(
                                                    signal.label() + " RIASEC signal",
                                                    "ASSESSMENT")));
            dashboard.recentResults().stream()
                    .findFirst()
                    .ifPresent(
                            result ->
                                    references.add(
                                            new SynMessageResponse.EvidenceReference(
                                                    result.title() + " result", "SIMULATION")));
        }

        SynMessageResponse.ComparisonCard comparison = null;
        if (decision.tools().contains("COMPARE_PATHS")) {
            List<SynAgentDao.PathProfile> profiles = synAgentDao.findPaths(paths);
            List<SynAgentDao.SimulationOption> simulations =
                    synAgentDao.findSimulations(
                            profiles.stream().map(SynAgentDao.PathProfile::code).toList(), 3);
            comparison =
                    new SynMessageResponse.ComparisonCard(
                            "Compare through evidence, not labels",
                            profiles.stream()
                                    .map(
                                            path ->
                                                    new SynMessageResponse.PathComparison(
                                                            path.code(),
                                                            path.title(),
                                                            evidenceFor(path, dashboard),
                                                            simulations.stream()
                                                                    .filter(
                                                                            simulation ->
                                                                                    simulation
                                                                                            .careerTrack()
                                                                                            .equals(
                                                                                                    path
                                                                                                            .code()))
                                                                    .findFirst()
                                                                    .map(
                                                                            simulation ->
                                                                                    "Try “"
                                                                                            + simulation
                                                                                                    .title()
                                                                                            + "” and note what felt engaging or difficult.")
                                                                    .orElse(
                                                                            "Try one short task in this path and record your experience.")))
                                    .toList());
            activity.add("Compared " + profiles.size() + " reviewed exploration paths");
        }

        List<SynMessageResponse.ResourceCard> resources = List.of();
        if (decision.tools().contains("FIND_RESOURCES")) {
            resources =
                    synAgentDao.findResources(paths, 3).stream()
                            .map(
                                    resource ->
                                            new SynMessageResponse.ResourceCard(
                                                    resource.title(),
                                                    resource.provider(),
                                                    resource.url(),
                                                    resource.skill(),
                                                    resource.difficulty(),
                                                    resource.estimatedMinutes()))
                            .toList();
            activity.add("Found " + resources.size() + " reviewed learning resources");
        }

        StandardGuidancePolicy.DraftReply standard =
                standardGuidancePolicy.createReply(
                        standardAction(decision.intent()),
                        message,
                        dashboard,
                        requestedAttemptId,
                        decision.responseLanguage());
        SynMessageResponse.PlanDraft plan = standard.planDraft();
        if (decision.tools().contains("DRAFT_PLAN") && plan == null) {
            plan = createPlan(paths, dashboard);
        }
        if (decision.tools().contains("FIND_SKILL_GAPS")) {
            activity.add("Checked where another piece of evidence may help");
        }
        if (decision.tools().contains("SUGGEST_SIMULATIONS")) {
            activity.add("Matched optional next simulations");
        }

        String text =
                specializedText(
                        decision.intent(),
                        decision.responseLanguage(),
                        standard.text(),
                        comparison,
                        resources);
        List<String> suggestions =
                specializedSuggestions(
                        decision.intent(), decision.responseLanguage(), standard.suggestions());
        return new ToolResult(
                paths,
                activity,
                references,
                comparison,
                resources,
                standard.resultCard(),
                suggestions,
                plan,
                text,
                observations(decision.intent(), dashboard, comparison, resources));
    }

    private List<String> resolvedPaths(
            SynAgentPolicy.Decision decision, DashboardResponse dashboard) {
        if (!decision.paths().isEmpty()) return decision.paths();
        String top =
                dashboard.interestSignals().stream()
                        .findFirst()
                        .map(DashboardResponse.InterestSignal::code)
                        .orElse("");
        if ("I".equals(top) || "C".equals(top)) return List.of("BACKEND_DEVELOPMENT");
        if ("A".equals(top)) return List.of("FRONTEND_DEVELOPMENT");
        return List.of();
    }

    private String evidenceFor(SynAgentDao.PathProfile path, DashboardResponse dashboard) {
        String token = path.title().split(" ")[0].toLowerCase();
        return dashboard.recentResults().stream()
                .filter(result -> result.title().toLowerCase().contains(token))
                .findFirst()
                .map(
                        result ->
                                "You have one completed result ("
                                        + result.score()
                                        + "/100); use its task outcomes as starting evidence.")
                .orElse("No completed result for this path yet; comparison is still exploratory.");
    }

    private SynMessageResponse.PlanDraft createPlan(
            List<String> paths, DashboardResponse dashboard) {
        String path = paths.isEmpty() ? "a different IT path" : friendlyPath(paths.get(0));
        String reflection =
                dashboard.skills().stream()
                        .filter(skill -> "NEEDS_MORE_EVIDENCE".equals(skill.status()))
                        .findFirst()
                        .map(skill -> "Repeat a focused task involving " + skill.name() + ".")
                        .orElse("Complete one short simulation in " + path + ".");
        return new SynMessageResponse.PlanDraft(
                "Gather one more comparable piece of evidence",
                List.of(
                        reflection,
                        "Record what felt engaging, difficult, and worth trying again.",
                        "Compare the new outcome with your current interests before deciding."));
    }

    private String specializedText(
            String intent,
            String responseLanguage,
            String fallback,
            SynMessageResponse.ComparisonCard comparison,
            List<SynMessageResponse.ResourceCard> resources) {
        boolean vi = "VI".equals(responseLanguage);
        return switch (intent) {
            case "GET_STARTED" ->
                    vi
                            ? "Bạn chưa cần có kết quả để bắt đầu. Syn đã chọn một vài hướng IT và trải nghiệm ngắn để bạn xem như các lựa chọn thử nghiệm. Chúng chưa phải nhận xét về mức độ phù hợp của bạn; hãy chọn một điểm bắt đầu khiến bạn tò mò."
                            : "You do not need existing results to begin. Syn selected a few reviewed IT paths and short experiences as starting options. They are not claims about your fit; choose one that makes you curious.";
            case "COMPARE_PATHS" ->
                    comparison == null || comparison.paths().isEmpty()
                            ? (vi
                                    ? "Syn cần hai hướng cụ thể để so sánh hữu ích. Bạn có thể hỏi về backend, frontend, dữ liệu, kiểm thử hoặc bảo mật."
                                    : "I need two named paths to make a useful comparison. Try asking about backend, frontend, data, testing, or security.")
                            : (vi
                                    ? "Syn đã so sánh các hướng bằng dữ liệu hiện có và hồ sơ nghề nghiệp đã được rà soát. Hãy xem các thử nghiệm tiếp theo là lựa chọn để trải nghiệm, không phải quyết định nghề nghiệp."
                                    : "I compared the paths using your available evidence and a small reviewed path profile. Treat the next experiments as options, not a career decision.");
            case "LEARNING_RESOURCES" ->
                    resources.isEmpty()
                            ? (vi
                                    ? "Syn chưa tìm thấy tài nguyên đã rà soát cho hướng này. Bạn vẫn có thể thu thập trải nghiệm qua một mô phỏng ngắn."
                                    : "I could not find a reviewed resource for that path yet. You can still gather evidence through a short simulation.")
                            : (vi
                                    ? "Các tài nguyên đã rà soát này có thể giúp bạn chuẩn bị trước mô phỏng tiếp theo. Hãy bắt đầu với một tài nguyên nhỏ rồi kiểm tra kỹ năng qua thực hành."
                                    : "These reviewed resources can strengthen a skill signal before your next simulation. Start with one small resource, then test the skill in practice.");
            case "CAREER_QUESTION" ->
                    vi
                            ? "Syn có thể liên hệ câu hỏi này với dữ liệu hiện có nhưng không chọn nghề thay bạn. Hãy xem phần so sánh như một giả thuyết để kiểm tra bằng trải nghiệm tiếp theo."
                            : "I can connect this question to your current evidence, but I cannot decide your career. Use the comparison below as a hypothesis to test through another experience.";
            default -> fallback;
        };
    }

    private List<String> specializedSuggestions(
            String intent, String responseLanguage, List<String> fallback) {
        boolean vi = "VI".equals(responseLanguage);
        return switch (intent) {
            case "GET_STARTED" ->
                    vi
                            ? List.of(
                                    "Tôi nên bắt đầu với RIASEC hay mô phỏng?",
                                    "So sánh backend và frontend")
                            : List.of(
                                    "Should I start with RIASEC or a simulation?",
                                    "Compare backend and frontend");
            case "COMPARE_PATHS" ->
                    vi
                            ? List.of(
                                    "Tôi nên thử hướng nào trước?",
                                    "Cho tôi tài nguyên học của hướng đầu tiên")
                            : List.of(
                                    "Which path should I test first?",
                                    "Show learning resources for the first path");
            case "LEARNING_RESOURCES" ->
                    vi
                            ? List.of(
                                    "Phác thảo kế hoạch luyện tập nhỏ",
                                    "Tài nguyên này hỗ trợ phần nào?")
                            : List.of(
                                    "Draft a small practice plan",
                                    "What evidence gap does this address?");
            case "CAREER_QUESTION" ->
                    vi
                            ? List.of(
                                    "So sánh backend và frontend", "Tôi đang thiếu bằng chứng gì?")
                            : List.of(
                                    "Compare backend and frontend", "What evidence am I missing?");
            default -> fallback;
        };
    }

    private String standardAction(String intent) {
        return switch (intent) {
            case "IDENTIFY_GAPS" -> "NEEDS_EVIDENCE";
            case "CAREER_QUESTION", "COMPARE_PATHS", "LEARNING_RESOURCES" -> "EXPLORE_NEXT";
            case "GET_STARTED" -> "GET_STARTED";
            default -> intent;
        };
    }

    private List<String> observations(
            String intent,
            DashboardResponse dashboard,
            SynMessageResponse.ComparisonCard comparison,
            List<SynMessageResponse.ResourceCard> resources) {
        List<String> values = new ArrayList<>();
        values.add("Intent: " + intent);
        dashboard.interestSignals().stream()
                .findFirst()
                .ifPresent(signal -> values.add("Top interest signal: " + signal.label()));
        dashboard.skills().stream()
                .filter(skill -> "NEEDS_MORE_EVIDENCE".equals(skill.status()))
                .findFirst()
                .ifPresent(skill -> values.add("Possible evidence gap: " + skill.name()));
        if (comparison != null) {
            values.add(
                    "Compared paths: "
                            + comparison.paths().stream()
                                    .map(SynMessageResponse.PathComparison::title)
                                    .toList());
        }
        if (!resources.isEmpty()) values.add("Reviewed resources available: " + resources.size());
        return values.stream().limit(5).toList();
    }

    private String friendlyPath(String code) {
        return code.toLowerCase().replace('_', ' ');
    }

    public record ToolResult(
            List<String> paths,
            List<String> activity,
            List<SynMessageResponse.EvidenceReference> evidenceReferences,
            SynMessageResponse.ComparisonCard comparisonCard,
            List<SynMessageResponse.ResourceCard> resourceCards,
            DashboardResponse.RecentResult resultCard,
            List<String> suggestions,
            SynMessageResponse.PlanDraft planDraft,
            String standardText,
            List<String> observations) {}
}
