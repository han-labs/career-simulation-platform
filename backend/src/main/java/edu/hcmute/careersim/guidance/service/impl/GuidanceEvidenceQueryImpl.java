package edu.hcmute.careersim.guidance.service.impl;

import edu.hcmute.careersim.assessment.access.*;
import edu.hcmute.careersim.guidance.service.*;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.access.*;
import edu.hcmute.careersim.simulation.service.EnrollmentService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuidanceEvidenceQueryImpl implements GuidanceEvidenceQuery {
    private final ActorAccess actors;
    private final VersionedAssessmentAccess assessments;
    private final AssessmentEvidenceAccess legacyAssessments;
    private final SimulationEvidenceAccess legacySimulations;
    private final ParticipationEvidenceAccess participation;
    private final EnrollmentService enrollments;
    private final ExplorationPlanService plans;

    @Transactional(readOnly = true)
    public Object dashboard(long actor) {
        var a = actors.requireId(actor);
        ActorAccess.requireRole(a, "STUDENT");
        var result = new LinkedHashMap<String, Object>();
        result.put("studentName", a.displayName());
        result.put("source", "LIVE");
        result.put("assessments", assessments.readOwned(actor));
        result.put("enrollments", enrollments.list(actor, 0, 100));
        result.put("completions", participation.completions(actor));
        result.put("participation", participation.readOwned(actor, 100));
        result.put(
                "legacyAssessment",
                legacyAssessments
                        .findLatestCompleted(actor)
                        .map(
                                legacy ->
                                        Map.of(
                                                "sourceKind",
                                                "LEGACY_ASSESSMENT",
                                                "contentVersion",
                                                "UNKNOWN",
                                                "data",
                                                legacy))
                        .orElse(null));
        result.put(
                "legacyEvaluations",
                legacySimulations.findRecentEvaluated(actor, 10).stream()
                        .map(e -> Map.of("sourceKind", "LEGACY_EVALUATION", "data", e))
                        .toList());
        result.put("activePlan", plans.current(actor).orElse(null));
        return result;
    }
}
