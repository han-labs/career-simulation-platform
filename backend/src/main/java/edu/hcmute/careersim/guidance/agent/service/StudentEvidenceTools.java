package edu.hcmute.careersim.guidance.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.assessment.access.VersionedAssessmentAccess;
import edu.hcmute.careersim.assessment.service.VersionedAssessmentService;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.*;
import edu.hcmute.careersim.simulation.access.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Read-only Core projections. Actor identity is supplied by the host, never by model arguments. */
@Component
@RequiredArgsConstructor
public class StudentEvidenceTools {
    private final VersionedAssessmentAccess assessments;
    private final VersionedAssessmentService catalogue;
    private final ScenarioCatalogAccess scenarios;
    private final ParticipationEvidenceAccess evidence;
    private final edu.hcmute.careersim.simulation.service.EnrollmentService enrollments;
    private final edu.hcmute.careersim.simulation.service.TaskSubmissionService submissions;
    public static final Set<String> NAMES =
            Set.of("assessment.lookup", "simulation.explore", "simulation.myEvidence", "kb.search");

    public static List<AgentModelProvider.Tool> definitions(Collection<String> enabled) {
        return enabled.stream()
                .sorted()
                .map(
                        n ->
                                new AgentModelProvider.Tool(
                                        n,
                                        switch (n) {
                                            case "assessment.lookup" ->
                                                    "Read career quizzes with scope PERSONAL (this Student: exact activity counts, latest saved results), CATALOG (public quiz details/counts/duration, no private data), or ALL (default). Use PERSONAL for any wording of I did a quiz, find/analyse my results, have I completed any, including Vietnamese paraphrases. Optional assessmentType code filter applies before result limits. Details are bounded; activity counts are totals, not the length of the detail list. No answers, grading keys or other users.";
                                            case "simulation.explore" ->
                                                    "Discover published simulations and explain public details, objectives, duration and task outlines. No enrollment or completion required. Use query:'' or {} for broad discovery; short literal search terms otherwise. No answers or private results.";
                                            case "simulation.myEvidence" ->
                                                    "Read this Student's exact accessible enrollment counts (not started/in progress/completed, distinct simulations) and bounded progress, final quiz results, practised skills and completions. Use for have I tried/completed simulations, how many and how did I do. ENROLLED is not completed. Never certify proficiency.";
                                            default ->
                                                    "Retrieve only explicitly granted published KB passages.";
                                        },
                                        parameters(n)))
                .toList();
    }

    private static JsonNode parameters(String name) {
        var fields = new LinkedHashMap<String, Object>();
        for (String k : argumentKeys(name))
            fields.put(
                    k,
                    k.equals("scope")
                            ? Map.of(
                                    "type", "string", "enum", List.of("ALL", "CATALOG", "PERSONAL"))
                            : k.equals("topK")
                                    ? Map.of("type", "integer", "minimum", 1, "maximum", 10)
                                    : Map.of(
                                            "type", "string", "maxLength", argumentLimit(name, k)));
        return CoreValues.tree(
                Map.of(
                        "type",
                        "object",
                        "properties",
                        fields,
                        "required",
                        name.equals("kb.search") ? List.of("query") : List.of(),
                        "additionalProperties",
                        false));
    }

    private static int argumentLimit(String name, String key) {
        if (name.equals("simulation.explore")) {
            if (key.equals("query")) return 200;
            if (key.equals("careerTrack")) return 60;
        }
        return key.equals("query") ? 2000 : 80;
    }

    private static Set<String> argumentKeys(String name) {
        return switch (name) {
            case "assessment.lookup" -> Set.of("assessmentType", "scope");
            case "simulation.explore" -> Set.of("query", "careerTrack", "difficulty");
            case "kb.search" -> Set.of("query", "topK");
            default -> Set.of();
        };
    }

    public static JsonNode arguments(String name, JsonNode args) {
        if (args == null || !args.isObject())
            CoreValues.invalid("Tool arguments must be an object.");
        var clean = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        args.fields()
                .forEachRemaining(
                        f -> {
                            if (!argumentKeys(name).contains(f.getKey()))
                                CoreValues.invalid("Unknown tool argument.");
                            if (f.getKey().equals("topK")) {
                                if (!f.getValue().isIntegralNumber()
                                        || !f.getValue().canConvertToInt())
                                    CoreValues.invalid("Invalid Top K.");
                                clean.put(
                                        "topK",
                                        edu.hcmute.careersim.guidance.knowledge.domain
                                                .KnowledgeSourcePolicy.topK(f.getValue().asInt()));
                            } else {
                                if (!f.getValue().isTextual())
                                    CoreValues.invalid("Tool filter must be text.");
                                if (name.equals("simulation.explore")
                                        && f.getValue().asText().isEmpty()) {
                                    // Empty optional catalogue filters mean broad public discovery.
                                    // KB search still requires a nonblank query.
                                    if (f.getKey().equals("query")) clean.put("query", "");
                                    return;
                                }
                                clean.put(
                                        f.getKey(),
                                        AgentInputPolicy.minimize(
                                                f.getValue().asText(),
                                                argumentLimit(name, f.getKey())));
                            }
                        });
        if (clean.has("scope")
                && !Set.of("ALL", "CATALOG", "PERSONAL").contains(clean.path("scope").asText()))
            CoreValues.invalid("Invalid assessment scope.");
        if (name.equals("kb.search") && !clean.has("query"))
            CoreValues.invalid("KB search query required.");
        return clean;
    }

    public List<DomainEvidence> preview(String name, JsonNode args) {
        if (!Set.of("assessment.lookup", "simulation.explore").contains(name))
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.FORBIDDEN,
                    "No Student context exists in Admin preview.");
        return read(0, name, args, false);
    }

    public List<DomainEvidence> invoke(long owner, String name, JsonNode args) {
        return read(owner, name, args, true);
    }

    private List<DomainEvidence> read(long owner, String name, JsonNode args, boolean personal) {
        args = arguments(name, args);
        String filter = args.has("assessmentType") ? args.path("assessmentType").asText() : null;
        var result = new ArrayList<DomainEvidence>();
        if (name.equals("assessment.lookup")) {
            String scope = args.path("scope").asText("ALL");
            if (!personal && scope.equals("PERSONAL"))
                throw new edu.hcmute.careersim.common.exception.ApiException(
                        edu.hcmute.careersim.common.exception.ErrorCode.FORBIDDEN,
                        "No Student context exists in Admin preview.");
            if (personal && !scope.equals("CATALOG")) {
                add(
                        result,
                        "OV:assessment:"
                                + (filter == null
                                        ? "all"
                                        : CoreValues.hash(filter).substring(0, 12)),
                        "OWN_ASSESSMENT_ACTIVITY",
                        "OWNER",
                        "CURRENT",
                        Objects.requireNonNull(assessments.activity(owner, filter)));
                for (var a : assessments.readOwned(owner, filter).stream().limit(20).toList())
                    add(
                            result,
                            "AR:" + a.attemptId(),
                            "ASSESSMENT_RESULT",
                            "OWNER",
                            a.assessmentVersionId(),
                            a);
            }
            if (!scope.equals("PERSONAL"))
                for (var t : catalogue.publicCatalog())
                    if (filter == null || t.code().equals(filter))
                        add(
                                result,
                                "AT:" + t.id(),
                                "ASSESSMENT_CATALOG",
                                "PUBLIC",
                                t.activeVersionId(),
                                t);
        } else if (name.equals("simulation.explore")) {
            if (args.has("query") && !args.path("query").isTextual())
                CoreValues.invalid("Query must be text.");
            String q = args.path("query").asText("");
            if (q.length() > 200) CoreValues.invalid("Query too long.");
            for (var s :
                    scenarios
                            .search(
                                    new edu.hcmute.careersim.simulation.dto.ScenarioContracts
                                            .CatalogQuery(
                                            q,
                                            args.path("careerTrack").asText(null),
                                            args.path("difficulty").asText(null),
                                            null,
                                            null,
                                            8))
                            .items())
                add(
                        result,
                        "SC:" + s.scenarioVersionId(),
                        "PUBLISHED_SCENARIO",
                        "PUBLIC",
                        s.scenarioVersionId(),
                        publicScenario(s));
        } else if (name.equals("simulation.myEvidence")) {
            add(
                    result,
                    "OV:simulations",
                    "OWN_SIMULATION_ACTIVITY",
                    "OWNER",
                    "CURRENT",
                    Objects.requireNonNull(enrollments.activity(owner)));
            int i = 0;
            for (var p : evidence.readOwned(owner, 20))
                add(
                        result,
                        "SE:" + p.submissionId() + ":" + (i++),
                        "PRACTICED_SKILL",
                        "OWNER",
                        p.scenarioVersionId(),
                        p);
            for (var c : evidence.completions(owner).stream().limit(10).toList())
                add(
                        result,
                        "CO:" + c.id(),
                        "COMPLETION_NOT_COMPETENCE",
                        "OWNER",
                        c.id(),
                        Map.of(
                                "completionId",
                                c.id(),
                                "enrollmentId",
                                c.enrollmentId(),
                                "completedAt",
                                c.completedAt(),
                                "resumeSnippet",
                                c.resumeSnippetText()));
            for (var e : enrollments.list(owner, 0, 5)) {
                add(
                        result,
                        "EN:" + e.id(),
                        "OWN_ENROLLMENT_PROGRESS",
                        "OWNER",
                        e.scenarioVersionId(),
                        Map.of(
                                "enrollmentId",
                                e.id(),
                                "scenarioVersionId",
                                e.scenarioVersionId(),
                                "title",
                                e.metadata().title(),
                                "status",
                                e.status(),
                                "requiredTasks",
                                e.requiredTasks(),
                                "finalRequiredTasks",
                                e.finalRequiredTasks(),
                                "progression",
                                e.progression()));
                for (var progress :
                        e.progression().stream()
                                .filter(p -> p.finalized() && p.accessible())
                                .limit(8)
                                .toList()) {
                    var submission =
                            submissions.read(
                                    owner,
                                    Long.parseLong(e.id()),
                                    Long.parseLong(progress.taskId()));
                    if (submission.quizResult() != null)
                        add(
                                result,
                                "QR:" + submission.submissionId(),
                                "FINAL_QUIZ_RESULT",
                                "OWNER",
                                e.scenarioVersionId(),
                                Map.of(
                                        "enrollmentId",
                                        e.id(),
                                        "submissionId",
                                        submission.submissionId(),
                                        "taskId",
                                        submission.taskId(),
                                        "quizResult",
                                        submission.quizResult(),
                                        "finalized",
                                        true,
                                        "reviewed",
                                        progress.reviewed()));
                }
            }
        } else CoreValues.invalid("Unknown Core tool.");
        int size = 0;
        var bounded = new ArrayList<DomainEvidence>();
        for (var d : result) {
            int length = CoreValues.write(d).length();
            if (size + length > 8000) continue;
            size += length;
            bounded.add(d);
        }
        return List.copyOf(bounded);
    }

    private Map<String, Object> publicScenario(
            edu.hcmute.careersim.simulation.dto.ScenarioContracts.Summary s) {
        // Explicit public allowlist, never serialize the full version/task (answers/rubrics).
        var fields = new LinkedHashMap<String, Object>();
        fields.put("scenarioVersionId", s.scenarioVersionId());
        fields.put("slug", s.slug());
        fields.put("title", s.metadata().title());
        fields.put("careerTrack", s.metadata().careerTrack());
        fields.put("difficulty", s.metadata().difficulty());
        fields.put("summary", s.metadata().summary());
        fields.put("targetRole", s.metadata().targetRole());
        fields.put("introduction", s.metadata().introduction());
        fields.put("learningObjectives", s.metadata().learningObjectives());
        fields.put("estimatedMinutes", s.metadata().estimatedMinutes());
        fields.put("taskOutline", s.taskOutline().stream().limit(12).toList());
        return fields;
    }

    /** Current private Core authority is checked again before publishing or replaying evidence. */
    public boolean available(long owner, Collection<DomainEvidence> delivered) {
        try {
            for (var d : delivered) {
                if (d.kind().equals("OWN_ASSESSMENT_ACTIVITY")) {
                    String filter =
                            d.data().path("assessmentType").isNull()
                                    ? null
                                    : d.data().path("assessmentType").asText();
                    // Recheck current owner authority. Counts are an execution-time snapshot:
                    // starting another quiz must not make earlier conversations unreadable.
                    Objects.requireNonNull(assessments.activity(owner, filter));
                }
                if (d.kind().equals("OWN_SIMULATION_ACTIVITY"))
                    Objects.requireNonNull(enrollments.activity(owner));
                if (d.kind().equals("PRACTICED_SKILL")
                        && !evidence.ownsFinal(
                                owner,
                                d.data().path("submissionId").asText(),
                                d.versionId(),
                                d.data().path("skillCode").asText())) return false;
                if (d.kind().equals("ASSESSMENT_RESULT")) {
                    var current =
                            catalogue.result(
                                    owner, CoreValues.id(d.data().path("attemptId").asText()));
                    if (!current.assessmentVersionId().equals(d.versionId())) return false;
                }
                if (d.kind().equals("OWN_ENROLLMENT_PROGRESS")) {
                    var current =
                            enrollments.read(
                                    owner, CoreValues.id(d.data().path("enrollmentId").asText()));
                    if (!current.scenarioVersionId().equals(d.versionId())) return false;
                }
                if (d.kind().equals("FINAL_QUIZ_RESULT")) {
                    var current =
                            submissions.read(
                                    owner,
                                    CoreValues.id(d.data().path("enrollmentId").asText()),
                                    CoreValues.id(d.data().path("taskId").asText()));
                    if (!"FINAL".equals(current.status())
                            || !current.submissionId()
                                    .equals(d.data().path("submissionId").asText())
                            || !d.data().path("quizResult").equals(current.quizResult()))
                        return false;
                }
            }
            return true;
        } catch (edu.hcmute.careersim.common.exception.ApiException denied) {
            return false;
        }
    }

    private void add(
            List<DomainEvidence> to,
            String id,
            String kind,
            String access,
            String version,
            Object value) {
        var data = CoreValues.tree(value);
        String text = AgentInputPolicy.minimize(CoreValues.write(data), 18000);
        to.add(new DomainEvidence(id, kind, access, version, text, data));
    }
}
