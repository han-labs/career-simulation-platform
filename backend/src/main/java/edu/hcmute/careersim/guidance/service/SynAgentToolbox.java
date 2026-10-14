package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dao.SynAgentDao;
import edu.hcmute.careersim.guidance.domain.StandardGuidancePolicy;
import edu.hcmute.careersim.guidance.domain.SynAgentPolicy;
import edu.hcmute.careersim.guidance.domain.SynIntent;
import edu.hcmute.careersim.guidance.domain.SynToolIdentifier;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Executes the small, server-owned tool set selected by {@link SynAgentPolicy}. */
@Component
@Slf4j
public class SynAgentToolbox {

    private final SynAgentDao synAgentDao;
    private KnowledgeRetrievalService knowledge;
    private WebSearchProvider webSearch;

    @org.springframework.beans.factory.annotation.Autowired
    public void setWebSearch(WebSearchProvider value) {
        webSearch = value;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setKnowledge(KnowledgeRetrievalService knowledge) {
        this.knowledge = knowledge;
    }

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
        return execute(decision, message, dashboard, requestedAttemptId, "");
    }

    public ToolResult execute(
            SynAgentPolicy.Decision decision,
            String message,
            DashboardResponse dashboard,
            Long requestedAttemptId,
            String memory) {
        List<String> paths = resolvedPaths(decision, dashboard);
        List<DashboardResponse.RecentResult> selectedResults =
                selectResults(decision, dashboard, requestedAttemptId, paths);
        List<String> activity = new ArrayList<>();
        List<SynMessageResponse.EvidenceReference> references = new ArrayList<>();
        List<SynToolIdentifier> degradedTools = new ArrayList<>();
        List<SynAgentDao.SkillEvidence> skillEvidence = new ArrayList<>();

        if (decision.tools().contains(SynToolIdentifier.READ_EVIDENCE)) {
            try {
                for (var p : SynExecutionScope.participation().stream().limit(6).toList())
                    references.add(
                            new SynMessageResponse.EvidenceReference(
                                    p.skillNameSnapshot()
                                            + " — finalized task participation; no grade or competence claim",
                                    "TASK_PARTICIPATION",
                                    "SUBMISSION:" + p.submissionId() + ":" + p.scenarioVersionId(),
                                    p.scenarioVersionId(),
                                    p.skillCode(),
                                    null,
                                    null));
                activity.add("Reviewed available assessment and simulation evidence");
                dashboard.interestSignals().stream()
                        .findFirst()
                        .ifPresent(
                                signal ->
                                        references.add(
                                                new SynMessageResponse.EvidenceReference(
                                                        signal.label() + " RIASEC signal",
                                                        "ASSESSMENT",
                                                        dashboard.assessmentAttemptId() == null
                                                                ? null
                                                                : "A"
                                                                        + dashboard
                                                                                .assessmentAttemptId(),
                                                        null,
                                                        signal.code(),
                                                        null,
                                                        null)));
                selectedResults.stream()
                        .limit(3)
                        .forEach(
                                result ->
                                        references.add(
                                                new SynMessageResponse.EvidenceReference(
                                                        result.title() + " result",
                                                        "SIMULATION",
                                                        "S" + result.id(),
                                                        null,
                                                        result.careerTrack(),
                                                        null,
                                                        null)));
            } catch (RuntimeException exception) {
                markDegraded(SynToolIdentifier.READ_EVIDENCE, activity, degradedTools, exception);
                references.clear();
            }
        }

        SynMessageResponse.ComparisonCard comparison = null;
        if (decision.tools().contains(SynToolIdentifier.COMPARE_PATHS)) {
            try {
                List<SynAgentDao.PathProfile> profiles =
                        synAgentDao.findPaths(paths).stream()
                                .sorted(
                                        java.util.Comparator.comparingInt(
                                                p -> paths.isEmpty() ? 0 : paths.indexOf(p.code())))
                                .toList();
                List<SynAgentDao.SkillEvidence> comparisonEvidence =
                        synAgentDao.findSkillEvidence(resultIds(selectedResults));
                skillEvidence.addAll(comparisonEvidence);
                List<SynAgentDao.SimulationOption> simulations =
                        decision.tools().contains(SynToolIdentifier.SUGGEST_SIMULATIONS)
                                ? synAgentDao.findSimulations(
                                        profiles.stream()
                                                .map(SynAgentDao.PathProfile::code)
                                                .toList(),
                                        3)
                                : List.of();
                comparison =
                        new SynMessageResponse.ComparisonCard(
                                "Evidence comparison",
                                profiles.stream()
                                        .map(
                                                path ->
                                                        new SynMessageResponse.PathComparison(
                                                                path.code(),
                                                                path.title(),
                                                                !decision.tools()
                                                                                .contains(
                                                                                        SynToolIdentifier
                                                                                                .READ_EVIDENCE)
                                                                        ? path.summary()
                                                                        : evidenceFor(
                                                                                path,
                                                                                selectedResults,
                                                                                comparisonEvidence,
                                                                                decision.tools()
                                                                                                .contains(
                                                                                                        SynToolIdentifier
                                                                                                                .READ_EVIDENCE)
                                                                                        ? dashboard
                                                                                                .interestSignals()
                                                                                        : List
                                                                                                .of()),
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
                                                                        .orElse("")))
                                        .toList());
                activity.add("Compared " + profiles.size() + " reviewed exploration paths");
            } catch (RuntimeException exception) {
                markDegraded(SynToolIdentifier.COMPARE_PATHS, activity, degradedTools, exception);
                comparison = null;
            }
        }

        List<SynMessageResponse.ResourceCard> resources = List.of();
        if (decision.tools().contains(SynToolIdentifier.FIND_RESOURCES)) {
            try {
                List<String> rememberedSkills =
                        edu.hcmute.careersim.guidance.domain.SynResourceMemory.resolve(
                                message, memory);
                List<String> requestedSkills =
                        rememberedSkills.isEmpty()
                                ? resourceSkillCodes(message, selectedResults)
                                : rememberedSkills;
                resources =
                        rankedResources(paths, requestedSkills).stream()
                                .limit(rememberedSkills.isEmpty() ? 3 : 1)
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
            } catch (RuntimeException exception) {
                markDegraded(SynToolIdentifier.FIND_RESOURCES, activity, degradedTools, exception);
                resources = List.of();
            }
        }

        List<SynMessageResponse.SimulationCard> simulations = List.of();
        if (decision.tools().contains(SynToolIdentifier.SUGGEST_SIMULATIONS)) {
            try {
                simulations =
                        synAgentDao.findSimulations(paths, 3).stream()
                                .map(
                                        simulation ->
                                                new SynMessageResponse.SimulationCard(
                                                        simulation.title(),
                                                        simulation.careerTrack(),
                                                        simulation.summary(),
                                                        simulation.difficulty(),
                                                        simulation.estimatedMinutes()))
                                .toList();
                activity.add("Matched " + simulations.size() + " optional next simulations");
            } catch (RuntimeException exception) {
                markDegraded(
                        SynToolIdentifier.SUGGEST_SIMULATIONS, activity, degradedTools, exception);
                simulations = List.of();
            }
        }

        if (decision.tools().contains(SynToolIdentifier.FIND_SKILL_GAPS)) {
            try {
                List<SynAgentDao.SkillEvidence> gapEvidence =
                        synAgentDao.findSkillEvidence(resultIds(selectedResults));
                skillEvidence.addAll(gapEvidence);
                List<String> gaps =
                        gapEvidence.stream()
                                .filter(evidence -> "NEEDS_MORE_EVIDENCE".equals(evidence.status()))
                                .map(SynAgentDao.SkillEvidence::skillName)
                                .distinct()
                                .limit(3)
                                .toList();
                activity.add(
                        gaps.isEmpty()
                                ? "Checked reviewed skill mappings for evidence gaps"
                                : "Checked reviewed skill mappings; more evidence may help with "
                                        + String.join(", ", gaps));
            } catch (RuntimeException exception) {
                markDegraded(SynToolIdentifier.FIND_SKILL_GAPS, activity, degradedTools, exception);
            }
        }

        KnowledgeRetrievalService.Retrieval retrieval =
                new KnowledgeRetrievalService.Retrieval("NOT_REQUESTED", List.of());
        if (decision.tools().contains(SynToolIdentifier.SEARCH_KNOWLEDGE)) {
            try {
                retrieval =
                        knowledge == null
                                ? new KnowledgeRetrievalService.Retrieval(
                                        "NO_RELEVANT_KNOWLEDGE", List.of())
                                : knowledge.retrieve(
                                        message, SynExecutionScope.mayUseProvider("gemini"));
                activity.add(
                        "Retrieved " + retrieval.chunks().size() + " reviewed knowledge passages");
                for (var c : retrieval.chunks())
                    references.add(
                            new SynMessageResponse.EvidenceReference(
                                    c.title(),
                                    "KNOWLEDGE",
                                    c.referenceId(),
                                    Long.toString(c.versionId()),
                                    c.section(),
                                    c.sourceUrl() == null
                                            ? "/api/v1/guidance/knowledge/chunks/" + c.id()
                                            : c.sourceUrl(),
                                    c.content()));
            } catch (RuntimeException failure) {
                markDegraded(SynToolIdentifier.SEARCH_KNOWLEDGE, activity, degradedTools, failure);
                retrieval =
                        new KnowledgeRetrievalService.Retrieval("KNOWLEDGE_UNAVAILABLE", List.of());
            }
        }

        WebSearchProvider.Result web = WebSearchProvider.Result.unavailable("NOT_REQUESTED");
        boolean resourceShortfall =
                decision.tools().contains(SynToolIdentifier.FIND_RESOURCES) && resources.isEmpty();
        if (decision.tools().contains(SynToolIdentifier.SEARCH_WEB) || resourceShortfall) {
            String query =
                    edu.hcmute.careersim.guidance.domain.PublicLearningQuery.build(message, paths);
            if (!SynExecutionScope.mayUseExternal())
                web = WebSearchProvider.Result.unavailable("NO_CONSENT");
            else if (webSearch == null) web = WebSearchProvider.Result.unavailable("DISABLED");
            else {
                web = SynExecutionScope.cachedSearch(query);
                if (web == null) {
                    web = webSearch.search(query);
                    SynExecutionScope.cacheSearch(query, web);
                }
            }
            activity.add(
                    "Web source discovery: "
                            + web.status()
                            + " ("
                            + web.sources().size()
                            + " sources)");
            if (List.of(
                            "UNAVAILABLE",
                            "RATE_LIMITED",
                            "ACCESS_DENIED",
                            "INCOMPLETE",
                            "INVALID_METADATA")
                    .contains(web.status())) degradedTools.add(SynToolIdentifier.SEARCH_WEB);
        }

        StandardGuidancePolicy.DraftReply standard =
                standardGuidancePolicy.createReply(
                        standardAction(decision.intent()),
                        scopedDashboard(dashboard, selectedResults, distinctSkills(skillEvidence)),
                        requestedAttemptId,
                        decision.responseLanguage());
        SynMessageResponse.PlanDraft plan = standard.planDraft();
        if (decision.tools().contains(SynToolIdentifier.DRAFT_PLAN)) {
            try {
                plan =
                        createPlan(
                                paths,
                                scopedDashboard(dashboard, selectedResults, skillEvidence),
                                resources,
                                simulations,
                                decision.responseLanguage(),
                                message);
            } catch (RuntimeException exception) {
                markDegraded(SynToolIdentifier.DRAFT_PLAN, activity, degradedTools, exception);
                plan = null;
            }
        }
        String text =
                specializedText(
                        decision.intent(),
                        decision.responseLanguage(),
                        standard.text(),
                        comparison,
                        resources);
        var practices =
                references.stream()
                        .filter(r -> "TASK_PARTICIPATION".equals(r.sourceType()))
                        .toList();
        if (!practices.isEmpty()
                && (decision.intent() == SynIntent.REVIEW_LATEST
                        || decision.intent() == SynIntent.IDENTIFY_GAPS))
            text =
                    ("VI".equals(decision.responseLanguage())
                                    ? "Các nhiệm vụ đã nộp cho thấy bạn đã thực hành: "
                                    : "Your finalized tasks record practice in: ")
                            + String.join(
                                    ", ",
                                    SynExecutionScope.participation().stream()
                                            .map(p -> p.skillNameSnapshot())
                                            .distinct()
                                            .limit(6)
                                            .toList())
                            + ("VI".equals(decision.responseLanguage())
                                    ? ". Đây là bằng chứng tham gia, không phải điểm hoặc chứng nhận năng lực."
                                    : ". This is participation evidence, without a grade or competence certification.");
        if (decision.intent() == SynIntent.COMPARE_PATHS
                && (comparison == null || comparison.paths().isEmpty())) {
            String topics = String.join(" / ", paths).replace('_', ' ');
            text =
                    "VI".equals(decision.responseLanguage())
                            ? "Mình chưa tạo được phần phân tích cho "
                                    + topics
                                    + ". Kho nội bộ chưa có hồ sơ các hướng này; mình không thay chúng bằng hướng khác. Bạn có thể gửi lại câu hỏi để thử phần phân tích AI."
                            : "I could not generate the comparison for "
                                    + topics
                                    + ". The local catalog has no matching profiles; I will not substitute other paths. Please retry the AI explanation.";
        }
        List<String> suggestions =
                specializedSuggestions(
                                decision.intent(),
                                decision.responseLanguage(),
                                standard.suggestions())
                        .stream()
                        .distinct()
                        .limit(2)
                        .toList();
        List<DashboardResponse.RecentResult> resultCards =
                resultCardsFor(decision, selectedResults);
        DashboardResponse.RecentResult primaryResult =
                resultCards.isEmpty() ? null : resultCards.get(0);
        List<SynToolIdentifier> executed = new ArrayList<>(decision.tools());
        if (!"NOT_REQUESTED".equals(web.status())
                && !executed.contains(SynToolIdentifier.SEARCH_WEB))
            executed.add(SynToolIdentifier.SEARCH_WEB);
        SynExecutionScope.recordTools(executed, degradedTools, retrieval);
        return new ToolResult(
                paths,
                activity,
                references,
                comparison,
                resources,
                simulations,
                primaryResult,
                resultCards,
                selectedResults,
                suggestions,
                plan,
                text,
                decision.intent() == SynIntent.COMPARE_PATHS
                                && !decision.tools().contains(SynToolIdentifier.READ_EVIDENCE)
                        ? List.of(
                                "General career comparison. Explain the requested paths; do not assess this student.")
                        : observations(
                                decision.intent(),
                                dashboard,
                                selectedResults,
                                comparison,
                                resources,
                                simulations,
                                plan,
                                distinctSkills(skillEvidence)),
                List.copyOf(degradedTools),
                retrieval,
                web);
    }

    private List<String> resolvedPaths(
            SynAgentPolicy.Decision decision, DashboardResponse dashboard) {
        if (!decision.paths().isEmpty()) return decision.paths();
        if (SynIntent.GET_STARTED != decision.intent()) return List.of();
        String top =
                dashboard.interestSignals().stream()
                        .findFirst()
                        .map(DashboardResponse.InterestSignal::code)
                        .orElse("");
        if ("I".equals(top) || "C".equals(top)) return List.of("BACKEND_DEVELOPMENT");
        if ("A".equals(top)) return List.of("FRONTEND_DEVELOPMENT");
        return List.of();
    }

    private String evidenceFor(
            SynAgentDao.PathProfile path,
            List<DashboardResponse.RecentResult> selectedResults,
            List<SynAgentDao.SkillEvidence> skillEvidence,
            List<DashboardResponse.InterestSignal> interestSignals) {
        List<Long> selectedIds = resultIds(selectedResults);
        List<SynAgentDao.SkillEvidence> matchedSkills =
                skillEvidence.stream()
                        .filter(evidence -> selectedIds.contains(evidence.attemptId()))
                        .filter(evidence -> path.skillCodes().contains(evidence.skillCode()))
                        .limit(3)
                        .toList();
        List<String> matchedInterests =
                interestSignals.stream()
                        .filter(signal -> path.riasecCodes().contains(signal.code()))
                        .map(DashboardResponse.InterestSignal::label)
                        .limit(2)
                        .toList();

        List<String> evidence = new ArrayList<>();
        if (!matchedInterests.isEmpty()) {
            evidence.add("Related interest signals: " + String.join(", ", matchedInterests));
        }
        if (!matchedSkills.isEmpty()) {
            evidence.add(
                    "Mapped task evidence: "
                            + matchedSkills.stream()
                                    .map(skill -> skill.skillName() + " (" + skill.status() + ")")
                                    .toList());
        }
        if (evidence.isEmpty()) {
            evidence.add("No completed mapped task evidence for this path yet");
        }
        return path.summary() + " " + String.join(". ", evidence) + ".";
    }

    private List<DashboardResponse.RecentResult> selectResults(
            SynAgentPolicy.Decision decision,
            DashboardResponse dashboard,
            Long requestedAttemptId,
            List<String> paths) {
        if (!decision.tools().contains(SynToolIdentifier.READ_EVIDENCE)) return List.of();
        if (requestedAttemptId != null) {
            return dashboard.recentResults().stream()
                    .filter(result -> result.id() == requestedAttemptId)
                    .findFirst()
                    .map(List::of)
                    .orElse(List.of());
        }
        if (!paths.isEmpty()) {
            List<DashboardResponse.RecentResult> matched =
                    paths.stream()
                            .map(
                                    path ->
                                            dashboard.recentResults().stream()
                                                    .filter(result -> matchesPath(result, path))
                                                    .findFirst()
                                                    .orElse(null))
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .limit(3)
                            .toList();
            return matched;
        }
        if (SynIntent.REVIEW_LATEST == decision.intent()
                || SynIntent.REVIEW_RESULT == decision.intent()) {
            return dashboard.recentResults().stream().limit(1).toList();
        }
        if (List.of(
                        SynIntent.CAREER_QUESTION,
                        SynIntent.IDENTIFY_GAPS,
                        SynIntent.LEARNING_RESOURCES,
                        SynIntent.DRAFT_PLAN,
                        SynIntent.EXPLORE_NEXT)
                .contains(decision.intent())) {
            return dashboard.recentResults().stream().limit(3).toList();
        }
        return List.of();
    }

    private boolean matchesPath(DashboardResponse.RecentResult result, String path) {
        return path.equals(result.careerTrack());
    }

    private List<DashboardResponse.RecentResult> resultCardsFor(
            SynAgentPolicy.Decision decision,
            List<DashboardResponse.RecentResult> selectedResults) {
        boolean comparison = decision.tools().contains(SynToolIdentifier.COMPARE_PATHS);
        boolean review =
                SynIntent.REVIEW_LATEST == decision.intent()
                        || SynIntent.REVIEW_RESULT == decision.intent();
        return comparison || review ? selectedResults.stream().limit(3).toList() : List.of();
    }

    private DashboardResponse scopedDashboard(
            DashboardResponse dashboard,
            List<DashboardResponse.RecentResult> selectedResults,
            List<SynAgentDao.SkillEvidence> skillEvidence) {
        List<DashboardResponse.SkillSummary> scopedSkills =
                !skillEvidence.isEmpty()
                        ? canonicalSkillSummaries(skillEvidence, selectedResults)
                        : selectedResults.isEmpty()
                                ? dashboard.skills()
                                : selectedResults.stream()
                                        .flatMap(
                                                result ->
                                                        result.outcomes().stream()
                                                                .map(
                                                                        outcome ->
                                                                                new DashboardResponse
                                                                                        .SkillSummary(
                                                                                        outcome
                                                                                                .label(),
                                                                                        outcome
                                                                                                .status(),
                                                                                        result
                                                                                                .title())))
                                        .toList();
        return new DashboardResponse(
                dashboard.source(),
                dashboard.studentName(),
                dashboard.progress(),
                dashboard.interestSignals(),
                scopedSkills,
                selectedResults,
                dashboard.plan());
    }

    private List<DashboardResponse.SkillSummary> canonicalSkillSummaries(
            List<SynAgentDao.SkillEvidence> skillEvidence,
            List<DashboardResponse.RecentResult> selectedResults) {
        Map<String, DashboardResponse.SkillSummary> summaries = new LinkedHashMap<>();
        for (SynAgentDao.SkillEvidence evidence : skillEvidence) {
            String source =
                    selectedResults.stream()
                            .filter(result -> result.id() == evidence.attemptId())
                            .map(DashboardResponse.RecentResult::title)
                            .findFirst()
                            .orElse("Completed simulation");
            summaries.putIfAbsent(
                    evidence.skillCode(),
                    new DashboardResponse.SkillSummary(
                            evidence.skillName(), evidence.status(), source));
        }
        return List.copyOf(summaries.values());
    }

    private List<Long> resultIds(List<DashboardResponse.RecentResult> selectedResults) {
        return selectedResults.stream().map(DashboardResponse.RecentResult::id).toList();
    }

    private List<SynAgentDao.SkillEvidence> distinctSkills(
            List<SynAgentDao.SkillEvidence> skillEvidence) {
        Map<String, SynAgentDao.SkillEvidence> unique = new LinkedHashMap<>();
        skillEvidence.forEach(
                evidence ->
                        unique.putIfAbsent(
                                evidence.attemptId() + ":" + evidence.skillCode(), evidence));
        return List.copyOf(unique.values());
    }

    private List<SynAgentDao.LearningResource> rankedResources(
            List<String> paths, List<String> skills) {
        Map<String, SynAgentDao.LearningResource> ranked = new LinkedHashMap<>();
        synAgentDao.findResources(paths, skills, 3).forEach(r -> ranked.put(r.url(), r));
        if (ranked.size() < 3 && !skills.isEmpty())
            synAgentDao
                    .findResources(paths, List.of(), 3)
                    .forEach(r -> ranked.putIfAbsent(r.url(), r));
        return ranked.values().stream().limit(3).toList();
    }

    private List<String> resourceSkillCodes(
            String message, List<DashboardResponse.RecentResult> selectedResults) {
        StringBuilder searchable = new StringBuilder(message.toLowerCase(Locale.ROOT));
        selectedResults.forEach(
                result ->
                        result.outcomes().stream()
                                .filter(outcome -> "NEEDS_MORE_EVIDENCE".equals(outcome.status()))
                                .forEach(
                                        outcome -> searchable.append(' ').append(outcome.label())));
        String value = searchable.toString();
        LinkedHashSet<String> skills = new LinkedHashSet<>();
        addSkill(skills, value, "deduplication", "duplicate", "dedup", "trùng lặp", "trung lap");
        addSkill(
                skills,
                value,
                "data_quality",
                "data clean",
                "data quality",
                "làm sạch",
                "lam sach");
        addSkill(skills, value, "data_validation", "validate data", "data validation");
        addSkill(skills, value, "sql", "sql", "query", "truy vấn", "truy van");
        addSkill(skills, value, "api_reasoning", "api", "rest");
        addSkill(skills, value, "accessibility", "accessibility", "keyboard", "a11y");
        addSkill(skills, value, "semantic_html", "semantic", "heading", "html");
        addSkill(skills, value, "boundary_testing", "boundary", "biên", "bien");
        addSkill(skills, value, "risk_testing", "risk test", "regression");
        addSkill(skills, value, "incident_triage", "incident", "security", "bảo mật", "bao mat");
        return List.copyOf(skills);
    }

    private void addSkill(
            LinkedHashSet<String> skills, String value, String code, String... phrases) {
        for (String phrase : phrases) {
            if (value.contains(phrase)) {
                skills.add(code);
                return;
            }
        }
    }

    private SynMessageResponse.PlanDraft createPlan(
            List<String> paths,
            DashboardResponse dashboard,
            List<SynMessageResponse.ResourceCard> resources,
            List<SynMessageResponse.SimulationCard> simulations,
            String language,
            String message) {
        boolean vi = "VI".equals(language);
        String gap =
                dashboard.skills().stream()
                        .filter(s -> "NEEDS_MORE_EVIDENCE".equals(s.status()))
                        .map(DashboardResponse.SkillSummary::name)
                        .findFirst()
                        .orElse(vi ? "một kỹ năng bạn muốn thử" : "one skill you want to explore");
        String resource =
                resources.stream()
                        .findFirst()
                        .map(SynMessageResponse.ResourceCard::title)
                        .orElse(
                                vi
                                        ? "tài liệu nền tảng liên quan"
                                        : "relevant introductory documentation");
        String simulation =
                simulations.stream()
                        .findFirst()
                        .map(SynMessageResponse.SimulationCard::title)
                        .orElse(vi ? "một mô phỏng phù hợp" : "one relevant simulation");
        String normalized =
                edu.hcmute.careersim.guidance.domain.KnowledgeChunker.normalize(message);
        resource = resource.substring(0, Math.min(resource.length(), 110));
        simulation = simulation.substring(0, Math.min(simulation.length(), 90));
        gap = gap.substring(0, Math.min(gap.length(), 90));
        String period =
                normalized.matches(".*(2 tuan|hai tuan|two weeks|2 weeks|14 ngay).*")
                        ? (vi ? "hai tuần" : "two weeks")
                        : normalized.matches(".*(1 tuan|mot tuan|one week|1 week|7 ngay).*")
                                ? (vi ? "một tuần" : "one week")
                                : (vi ? "đợt thực hành ngắn" : "a short practice cycle");
        return new SynMessageResponse.PlanDraft(
                vi ? "Kế hoạch trải nghiệm: " + period : "Exploration plan: " + period,
                vi
                        ? List.of(
                                "Đầu đợt: đọc một phần trong "
                                        + resource
                                        + "; ghi lại một câu hỏi muốn kiểm chứng.",
                                "Giữa đợt: thử " + simulation + ", chú ý " + gap + ".",
                                "Cuối đợt: ghi điều hứng thú, điều khó và bằng chứng mới; so sánh với kết quả trước khi chọn bước tiếp.")
                        : List.of(
                                "Start: study one section of "
                                        + resource
                                        + " and write one question to test.",
                                "Practise: try " + simulation + ", focusing on " + gap + ".",
                                "Reflect: record what engaged you, what was difficult and new evidence; compare with earlier results before choosing a next step."));
    }

    private String specializedText(
            SynIntent intent,
            String responseLanguage,
            String fallback,
            SynMessageResponse.ComparisonCard comparison,
            List<SynMessageResponse.ResourceCard> resources) {
        boolean vi = "VI".equals(responseLanguage);
        return switch (intent) {
            case GET_STARTED ->
                    vi
                            ? "Bạn chưa cần có kết quả để bắt đầu. Syn đã chọn một vài hướng IT và trải nghiệm ngắn để bạn xem như các lựa chọn thử nghiệm. Chúng chưa phải nhận xét về mức độ phù hợp của bạn; hãy chọn một điểm bắt đầu khiến bạn tò mò."
                            : "You do not need existing results to begin. Syn selected a few reviewed IT paths and short experiences as starting options. They are not claims about your fit; choose one that makes you curious.";
            case COMPARE_PATHS ->
                    comparison == null || comparison.paths().isEmpty()
                            ? (vi
                                    ? "Syn cần hai hướng cụ thể để so sánh hữu ích. Bạn có thể hỏi về backend, frontend, dữ liệu, kiểm thử hoặc bảo mật."
                                    : "I need two named paths to make a useful comparison. Try asking about backend, frontend, data, testing, or security.")
                            : (vi
                                    ? "Syn đã so sánh các hướng bằng dữ liệu hiện có và hồ sơ nghề nghiệp đã được rà soát. Hãy xem các thử nghiệm tiếp theo là lựa chọn để trải nghiệm, không phải quyết định nghề nghiệp."
                                    : "I compared the paths using your available evidence and a small reviewed path profile. Treat the next experiments as options, not a career decision.");
            case LEARNING_RESOURCES ->
                    resources.isEmpty()
                            ? (vi
                                    ? "Syn chưa tìm thấy tài nguyên đã rà soát cho hướng này. Bạn vẫn có thể thu thập trải nghiệm qua một mô phỏng ngắn."
                                    : "I could not find a reviewed resource for that path yet. You can still gather evidence through a short simulation.")
                            : (vi
                                    ? "Các tài nguyên đã rà soát này có thể giúp bạn chuẩn bị trước mô phỏng tiếp theo. Hãy bắt đầu với một tài nguyên nhỏ rồi kiểm tra kỹ năng qua thực hành."
                                    : "These reviewed resources can strengthen a skill signal before your next simulation. Start with one small resource, then test the skill in practice.");
            case CAREER_QUESTION ->
                    vi
                            ? "Syn có thể liên hệ câu hỏi này với dữ liệu hiện có nhưng không chọn nghề thay bạn. Hãy xem nhận định này như một giả thuyết để kiểm tra bằng trải nghiệm tiếp theo."
                            : "I can connect this question to your current evidence, but I cannot decide your career. Treat the interpretation as a hypothesis to test through another experience.";
            case CONFIRM_PLAN ->
                    vi
                            ? "Kế hoạch chưa được lưu. Hãy mở phần xem lại và bấm “Confirm and save”; Syn chỉ thông báo đã lưu sau khi hệ thống xác nhận thành công."
                            : "The plan has not been saved yet. Review the draft and choose “Confirm and save”; Syn only reports it as saved after the system confirms success.";
            default -> fallback;
        };
    }

    private List<String> specializedSuggestions(
            SynIntent intent, String responseLanguage, List<String> fallback) {
        boolean vi = "VI".equals(responseLanguage);
        return switch (intent) {
            case GET_STARTED ->
                    vi
                            ? List.of(
                                    "Tôi nên bắt đầu với RIASEC hay mô phỏng?",
                                    "So sánh backend và frontend")
                            : List.of(
                                    "Should I start with RIASEC or a simulation?",
                                    "Compare backend and frontend");
            case COMPARE_PATHS ->
                    vi
                            ? List.of(
                                    "Tôi nên thử hướng nào trước?",
                                    "Cho tôi tài nguyên học của hướng đầu tiên")
                            : List.of(
                                    "Which path should I test first?",
                                    "Show learning resources for the first path");
            case LEARNING_RESOURCES ->
                    vi
                            ? List.of(
                                    "Phác thảo kế hoạch luyện tập nhỏ",
                                    "Tài nguyên này hỗ trợ phần nào?")
                            : List.of(
                                    "Draft a small practice plan",
                                    "What evidence gap does this address?");
            case CAREER_QUESTION ->
                    vi
                            ? List.of(
                                    "So sánh backend và frontend", "Tôi đang thiếu bằng chứng gì?")
                            : List.of(
                                    "Compare backend and frontend", "What evidence am I missing?");
            case CONFIRM_PLAN -> List.of();
            default -> fallback;
        };
    }

    private SynIntent standardAction(SynIntent intent) {
        return switch (intent) {
            case CAREER_QUESTION, COMPARE_PATHS, LEARNING_RESOURCES -> SynIntent.EXPLORE_NEXT;
            case REVIEW_RESULT -> SynIntent.REVIEW_LATEST;
            default -> intent;
        };
    }

    private List<String> observations(
            SynIntent intent,
            DashboardResponse dashboard,
            List<DashboardResponse.RecentResult> selectedResults,
            SynMessageResponse.ComparisonCard comparison,
            List<SynMessageResponse.ResourceCard> resources,
            List<SynMessageResponse.SimulationCard> simulations,
            SynMessageResponse.PlanDraft plan,
            List<SynAgentDao.SkillEvidence> skillEvidence) {
        List<String> values = new ArrayList<>();
        values.add("Intent: " + intent.name());
        dashboard.interestSignals().stream()
                .findFirst()
                .ifPresent(signal -> values.add("Top interest signal: " + signal.label()));
        selectedResults.stream()
                .limit(3)
                .forEach(
                        result ->
                                values.add(
                                        "Selected objective result: "
                                                + result.title()
                                                + " ("
                                                + result.score()
                                                + "/100), outcomes="
                                                + result.outcomes().stream()
                                                        .map(
                                                                outcome ->
                                                                        outcome.label()
                                                                                + ":"
                                                                                + outcome.status())
                                                        .toList()));
        if (comparison != null) {
            values.add(
                    "Compared paths: "
                            + comparison.paths().stream()
                                    .map(SynMessageResponse.PathComparison::title)
                                    .toList());
        }
        if (!resources.isEmpty()) values.add("Reviewed resources available: " + resources.size());
        if (!simulations.isEmpty()) {
            values.add(
                    "Reviewed next simulations: "
                            + simulations.stream()
                                    .map(SynMessageResponse.SimulationCard::title)
                                    .toList());
        }
        List<String> mappedGaps =
                skillEvidence.stream()
                        .filter(evidence -> "NEEDS_MORE_EVIDENCE".equals(evidence.status()))
                        .map(SynAgentDao.SkillEvidence::skillName)
                        .distinct()
                        .limit(3)
                        .toList();
        if (!mappedGaps.isEmpty()) {
            values.add("Mapped skills needing more evidence: " + mappedGaps);
        }
        if (plan != null) {
            values.add("Server-owned plan draft: " + plan.title() + " | " + plan.steps());
        }
        return values.stream().limit(6).toList();
    }

    private String friendlyPath(String code) {
        return code.toLowerCase().replace('_', ' ');
    }

    private void markDegraded(
            SynToolIdentifier tool,
            List<String> activity,
            List<SynToolIdentifier> degradedTools,
            RuntimeException exception) {
        log.warn(
                "Syn tool degraded tool={} failure={}", tool, exception.getClass().getSimpleName());
        degradedTools.add(tool);
        activity.add(
                "Could not complete "
                        + tool.name().toLowerCase(Locale.ROOT).replace('_', ' ')
                        + "; continued with available evidence");
    }

    public record ToolResult(
            List<String> paths,
            List<String> activity,
            List<SynMessageResponse.EvidenceReference> evidenceReferences,
            SynMessageResponse.ComparisonCard comparisonCard,
            List<SynMessageResponse.ResourceCard> resourceCards,
            List<SynMessageResponse.SimulationCard> simulationCards,
            DashboardResponse.RecentResult resultCard,
            List<DashboardResponse.RecentResult> resultCards,
            List<DashboardResponse.RecentResult> selectedResults,
            List<String> suggestions,
            SynMessageResponse.PlanDraft planDraft,
            String standardText,
            List<String> observations,
            List<SynToolIdentifier> degradedTools,
            KnowledgeRetrievalService.Retrieval knowledge,
            WebSearchProvider.Result webSearch) {
        public Map<String, Object> providerData() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("REQUESTED_PATHS", paths);
            data.put(
                    "PERSONAL_EVIDENCE_SCOPE",
                    evidenceReferences.stream()
                                    .anyMatch(
                                            r ->
                                                    "ASSESSMENT".equals(r.sourceType())
                                                            || "TASK_PARTICIPATION"
                                                                    .equals(r.sourceType())
                                                            || "SIMULATION".equals(r.sourceType()))
                            ? "SELECTED_RELEVANT_EVIDENCE_ONLY"
                            : "NOT_SELECTED_FOR_THIS_REQUEST");
            data.put(
                    "EVIDENCE_REFERENCES",
                    evidenceReferences.stream()
                            .map(
                                    r ->
                                            Map.of(
                                                    "reference",
                                                    r.referenceId() == null ? "" : r.referenceId(),
                                                    "label",
                                                    r.label()))
                            .toList());
            data.put(
                    "TASK_PARTICIPATION",
                    evidenceReferences.stream()
                            .filter(r -> "TASK_PARTICIPATION".equals(r.sourceType()))
                            .toList());
            data.put(
                    "READ_EVIDENCE",
                    selectedResults.stream()
                            .map(
                                    r ->
                                            Map.of(
                                                    "title",
                                                    r.title(),
                                                    "score",
                                                    r.score(),
                                                    "outcomes",
                                                    r.outcomes()))
                            .toList());
            if (comparisonCard != null) data.put("COMPARE_PATHS", comparisonCard);
            data.put("FIND_RESOURCES", resourceCards);
            data.put(
                    "SEARCH_WEB",
                    Map.of(
                            "status",
                            webSearch.status(),
                            "sources",
                            webSearch.sources(),
                            "reviewed",
                            false));
            data.put("SUGGEST_SIMULATIONS", simulationCards);
            if (planDraft != null) data.put("DRAFT_PLAN", planDraft);
            data.put(
                    "FIND_SKILL_GAPS",
                    observations.stream().filter(v -> v.startsWith("Mapped skills")).toList());
            data.put(
                    "SEARCH_KNOWLEDGE",
                    Map.of(
                            "status",
                            knowledge.status(),
                            "passages",
                            knowledge.chunks().stream()
                                    .map(
                                            c ->
                                                    Map.of(
                                                            "reference",
                                                            c.referenceId(),
                                                            "title",
                                                            c.title(),
                                                            "section",
                                                            c.section(),
                                                            "text",
                                                            c.content()))
                                    .toList()));
            return Map.copyOf(data);
        }
    }
}
