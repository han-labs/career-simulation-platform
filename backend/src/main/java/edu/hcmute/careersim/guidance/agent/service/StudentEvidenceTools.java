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
                                                    "Read public assessment choices and current student's multiple own results; optional assessment type filter. No answer keys or diagnosis.";
                                            case "simulation.explore" ->
                                                    "Search published accessible simulation catalogue with public task outlines; no model answers.";
                                            case "simulation.myEvidence" ->
                                                    "Read own enrollment progress, valid final quiz results, practiced skills and completion; never certify proficiency.";
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
                    k.equals("topK")
                            ? Map.of("type", "integer", "minimum", 1, "maximum", 10)
                            : Map.of("type", "string", "maxLength", k.equals("query") ? 2000 : 80));
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

    private static Set<String> argumentKeys(String name) {
        return switch (name) {
            case "assessment.lookup" -> Set.of("assessmentType");
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
                                clean.put(
                                        f.getKey(),
                                        AgentInputPolicy.minimize(
                                                f.getValue().asText(),
                                                f.getKey().equals("query") ? 2000 : 80));
                            }
                        });
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
            for (var t : catalogue.catalog())
                if (!args.has("assessmentType")
                        || t.code().equals(args.path("assessmentType").asText()))
                    add(
                            result,
                            "AT:" + t.id(),
                            "ASSESSMENT_CATALOG",
                            "PUBLIC",
                            t.activeVersionId(),
                            t);
            if (personal)
                for (var a :
                        assessments.readOwned(owner).stream()
                                .filter(
                                        a ->
                                                filter == null
                                                        || a.assessmentTypeCode().equals(filter))
                                .limit(20)
                                .toList())
                    add(
                            result,
                            "AR:" + a.attemptId(),
                            "ASSESSMENT_RESULT",
                            "OWNER",
                            a.assessmentVersionId(),
                            a);
        } else if (name.equals("simulation.explore")) {
            if (args.has("query") && !args.path("query").isTextual())
                CoreValues.invalid("Query must be text.");
            String q = args.path("query").asText("");
            if (q.length() > 500) CoreValues.invalid("Query too long.");
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
                        Map.of(
                                "scenarioVersionId",
                                s.scenarioVersionId(),
                                "slug",
                                s.slug(),
                                "title",
                                s.metadata().title(),
                                "careerTrack",
                                s.metadata().careerTrack(),
                                "difficulty",
                                s.metadata().difficulty(),
                                "summary",
                                s.metadata().summary(),
                                "taskOutline",
                                s.taskOutline().stream().limit(12).toList()));
        } else if (name.equals("simulation.myEvidence")) {
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
            if ((size += CoreValues.write(d).length()) > 8000) break;
            bounded.add(d);
        }
        return List.copyOf(bounded);
    }

    /** Current private Core authority is checked again before publishing or replaying evidence. */
    public boolean available(long owner, Collection<DomainEvidence> delivered) {
        try {
            for (var d : delivered) {
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
