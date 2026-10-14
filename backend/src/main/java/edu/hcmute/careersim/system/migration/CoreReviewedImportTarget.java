package edu.hcmute.careersim.system.migration;

import static edu.hcmute.careersim.system.migration.MigrationContracts.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.assessment.domain.RiasecV1Evaluator;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.assessment.service.AssessmentAuthoringService;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.dto.PlanContracts.Confirm;
import edu.hcmute.careersim.guidance.service.*;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.ScenarioAuthoringService;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reviewed copies stay drafts; no historical participation or publication is inferred. */
@Component
@RequiredArgsConstructor
public class CoreReviewedImportTarget implements ReviewedImportTarget {
    private final ObjectMapper json;
    private final AssessmentAuthoringService assessments;
    private final RiasecV1Evaluator evaluator;
    private final ScenarioAuthoringService scenarios;
    private final PlanCommands plans;
    private final ExplorationPlanService planReads;
    private final MigrationRecordDao sources;

    public void review(Entry e) {
        var m = e.reviewedMapping();
        CoreValues.object(
                m,
                Set.of(
                        "actorId",
                        "enterpriseId",
                        "type",
                        "version",
                        "scenario",
                        "confirmation",
                        "confirmationEvidence"));
        long actor = CoreValues.id(m.path("actorId").asText());
        switch (e.targetKind()) {
            case "ASSESSMENT_CURRENT" -> {
                if (!"riasec_questions".equals(e.sourceTable())
                        || !"snapshot".equals(e.sourceKey())
                        || actor != CoreValues.id(e.reviewerId()))
                    CoreValues.invalid("Reviewed admin question snapshot required.");
                var v = json.convertValue(m.path("version"), VersionDraft.class);
                evaluator.publication(v);
                var old = sources.currentQuestions();
                if (old.size() != v.questions().size())
                    CoreValues.invalid("Question snapshot count mismatch.");
                for (int i = 0; i < old.size(); i++) {
                    var q = v.questions().get(i);
                    var source = old.get(i);
                    if (!q.prompt().equals(source.get("prompt"))
                            || !q.responseConfig()
                                    .path("dimension")
                                    .asText()
                                    .equals(source.get("dimension"))
                            || q.position() != ((Number) source.get("display_order")).intValue())
                        CoreValues.invalid("Reviewed question snapshot differs from source.");
                }
            }
            case "SCENARIO_CURRENT" -> {
                if (!"career_simulations".equals(e.sourceTable())
                        || !m.path("enterpriseId").isTextual())
                    CoreValues.invalid("Explicit reviewed enterprise mapping required.");
                CoreValues.id(m.path("enterpriseId").asText());
                var request = json.convertValue(m.path("scenario"), Create.class);
                if (request == null || request.draft() == null)
                    CoreValues.invalid("Reviewed reauthored scenario required.");
            }
            case "PLAN_CONFIRMED" -> {
                if (!"exploration_plans".equals(e.sourceTable()))
                    CoreValues.invalid("Legacy plan source required.");
                var old = sources.legacyPlan(CoreValues.id(e.sourceKey()));
                var request = json.convertValue(m.path("confirmation"), Confirm.class);
                var proof = m.path("confirmationEvidence");
                CoreValues.object(
                        proof, Set.of("reference", "studentId", "confirmedAt", "sourceHash"));
                CoreValues.text(proof.path("reference").asText(), 500);
                if (old.owner() != actor
                        || !Long.toString(actor).equals(proof.path("studentId").asText())
                        || !e.sourceHash().equals(proof.path("sourceHash").asText())
                        || !"ACTIVE".equals(old.status())
                        || request == null
                        || !old.title().equals(request.title())
                        || !old.steps().equals(CoreValues.tree(request.steps()))
                        || request.expectedActivePlanId() != null
                        || request.expectedRevision() != null)
                    CoreValues.invalid("Plan ownership/content/confirmation evidence mismatch.");
                Instant at = Instant.parse(proof.path("confirmedAt").asText());
                if (at.isAfter(Instant.now()))
                    CoreValues.invalid("Confirmation evidence cannot be in the future.");
            }
            default -> CoreValues.invalid("Historical or unsupported import kind.");
        }
    }

    public Target copy(Entry e) {
        var m = e.reviewedMapping();
        long actor = CoreValues.id(m.path("actorId").asText());
        long reviewer = CoreValues.id(e.reviewerId());
        return switch (e.targetKind()) {
            case "ASSESSMENT_CURRENT" -> {
                long type =
                        CoreValues.id(
                                assessments.createType(
                                        reviewer,
                                        json.convertValue(m.path("type"), TypeRequest.class)));
                var version =
                        assessments.createVersion(
                                reviewer,
                                type,
                                json.convertValue(m.path("version"), VersionDraft.class));
                yield new Target("assessment_version", version.id());
            }
            case "SCENARIO_CURRENT" -> {
                var v =
                        scenarios.create(
                                actor, json.convertValue(m.path("scenario"), Create.class));
                if (!v.enterpriseId().equals(m.path("enterpriseId").asText()))
                    CoreValues.invalid("Enterprise mapping mismatch.");
                yield new Target("scenario_version", v.id());
            }
            case "PLAN_CONFIRMED" -> {
                var c = json.convertValue(m.path("confirmation"), Confirm.class);
                var p =
                        plans.importConfirmed(
                                reviewer,
                                actor,
                                c.title(),
                                c.steps(),
                                Instant.parse(
                                        m.path("confirmationEvidence")
                                                .path("confirmedAt")
                                                .asText()));
                yield new Target("exploration_plan", p.id());
            }
            default -> throw new IllegalArgumentException("Unsupported import kind");
        };
    }

    public void validate(Target t, Entry e) {
        if (t.key() == null || t.table() == null)
            throw new IllegalStateException("Missing destination");
        var m = e.reviewedMapping();
        boolean same = false;
        switch (e.targetKind()) {
            case "ASSESSMENT_CURRENT" -> {
                if (!"assessment_version".equals(t.table())) break;
                var v =
                        assessments.getVersion(
                                CoreValues.id(e.reviewerId()), CoreValues.id(t.key()));
                var actual =
                        new VersionDraft(
                                v.evaluatorKey(),
                                v.evaluatorVersion(),
                                v.scoringConfig(),
                                v.questions().stream()
                                        .map(
                                                q ->
                                                        new QuestionDraft(
                                                                q.position(),
                                                                q.prompt(),
                                                                q.responseType(),
                                                                q.responseConfig(),
                                                                q.translations(),
                                                                q.required()))
                                        .toList());
                same =
                        CoreValues.hash(actual)
                                .equals(
                                        CoreValues.hash(
                                                json.convertValue(
                                                        m.path("version"), VersionDraft.class)));
            }
            case "SCENARIO_CURRENT" -> {
                if (!"scenario_version".equals(t.table())) break;
                var v =
                        scenarios.get(
                                CoreValues.id(m.path("actorId").asText()), CoreValues.id(t.key()));
                var tasks =
                        v.tasks().stream()
                                .map(
                                        q ->
                                                new Task(
                                                        null,
                                                        q.position(),
                                                        q.title(),
                                                        q.workplaceContext(),
                                                        q.instructions(),
                                                        q.required(),
                                                        q.translations(),
                                                        q.definitions().stream()
                                                                .map(
                                                                        d ->
                                                                                new Definition(
                                                                                        null,
                                                                                        d
                                                                                                .position(),
                                                                                        d.name(),
                                                                                        d.format(),
                                                                                        d
                                                                                                .required(),
                                                                                        d
                                                                                                .validationConfig()))
                                                                .toList(),
                                                        q.resources().stream()
                                                                .map(
                                                                        r ->
                                                                                new Resource(
                                                                                        null,
                                                                                        r
                                                                                                .position(),
                                                                                        r.title(),
                                                                                        r
                                                                                                .fileAssetId(),
                                                                                        r
                                                                                                .externalUrl(),
                                                                                        r
                                                                                                .releasePolicy()))
                                                                .toList(),
                                                        q.skillIds(),
                                                        q.modelAnswer()))
                                .toList();
                same =
                        v.enterpriseId().equals(m.path("enterpriseId").asText())
                                && CoreValues.hash(
                                                new Create(
                                                        scenarios.slug(
                                                                CoreValues.id(
                                                                        m.path("actorId").asText()),
                                                                CoreValues.id(t.key())),
                                                        new Draft(v.metadata(), tasks)))
                                        .equals(
                                                CoreValues.hash(
                                                        json.convertValue(
                                                                m.path("scenario"), Create.class)));
            }
            case "PLAN_CONFIRMED" -> {
                if (!"exploration_plan".equals(t.table())) break;
                var c = json.convertValue(m.path("confirmation"), Confirm.class);
                same =
                        planReads.list(CoreValues.id(m.path("actorId").asText())).stream()
                                .anyMatch(
                                        p ->
                                                p.id().equals(t.key())
                                                        && p.title().equals(c.title())
                                                        && p.steps().equals(c.steps())
                                                        && p.confirmedAt()
                                                                .equals(
                                                                        Instant.parse(
                                                                                m.path(
                                                                                                "confirmationEvidence")
                                                                                        .path(
                                                                                                "confirmedAt")
                                                                                        .asText())));
            }
            default -> {}
        }
        if (!same)
            throw new IllegalStateException(
                    "Reviewed mapping or destination changed; manual review required.");
    }
}
