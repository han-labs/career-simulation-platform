package edu.hcmute.careersim.simulation.service.impl;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.enterprise.access.EnterpriseDirectoryAccess;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.dao.*;
import edu.hcmute.careersim.simulation.domain.CompletionArtifacts;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Completion;
import edu.hcmute.careersim.simulation.service.CompletionService;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
public class CompletionServiceImpl implements CompletionService {
    private final EnrollmentDao enrollments;
    private final CompletionDao dao;
    private final ScenarioDao scenarios;
    private final LearningPolicyDao learning;
    private final ActorAccess actors;
    private final EnterpriseDirectoryAccess enterprises;
    private final AuditWriter audit;

    @Transactional(propagation = Propagation.MANDATORY)
    public void completeIfEligible(long actor, long enrollment) {
        var e = enrollments.owned(actor, enrollment, true);
        if (enrollments.completionId(enrollment).isPresent()) return;
        int required = enrollments.required(e.version());
        if (required == 0 || enrollments.finalRequired(enrollment, e.version()) != required) return;
        var v = scenarios.version(e.version(), false);
        if (learning.ordered(e.version())) {
            var requiredIds =
                    v.tasks().stream().filter(t -> t.required()).map(t -> t.id()).toList();
            var progress = learning.progress(enrollment, e.version());
            if (requiredIds.stream()
                    .anyMatch(
                            id ->
                                    progress.stream()
                                            .noneMatch(
                                                    p ->
                                                            id.equals(p.taskId())
                                                                    && p.finalized()
                                                                    && p.reviewed()))) return;
        }
        var a = actors.requireId(actor);
        Instant now = Instant.now();
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("schemaVersion", 1);
        snapshot.put("studentDisplayName", a.displayName());
        snapshot.put("scenarioId", v.scenarioId());
        snapshot.put("scenarioVersionId", v.id());
        snapshot.put("scenarioTitle", v.metadata().title());
        snapshot.put("enterpriseId", v.enterpriseId());
        snapshot.put("enterpriseName", enterprises.name(CoreValues.id(v.enterpriseId())));
        snapshot.put("completedAt", now.toString());
        snapshot.put("issuer", "CareerSim");
        snapshot.put("statementCode", "SIMULATION_COMPLETION_ONLY");
        long id =
                dao.insert(
                        enrollment,
                        now,
                        CompletionArtifacts.snippet(
                                v.metadata().title(), dao.requiredSkills(enrollment)),
                        UUID.randomUUID().toString(),
                        snapshot);
        dao.complete(enrollment, now);
        audit.record(actor, "SIMULATION_COMPLETED", "simulation_completion", id, Map.of());
    }

    @Transactional(readOnly = true)
    public Completion read(long actor, long id) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
        return dao.read(actor, id);
    }

    @Transactional(readOnly = true)
    public String renderCertificate(long actor, long id) {
        var c = read(actor, id);
        var s = c.certificateSnapshot();
        return "<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><title>CareerSim completion</title><h1>Simulation completion</h1><p>"
                + HtmlUtils.htmlEscape(s.path("studentDisplayName").asText())
                + " completed "
                + HtmlUtils.htmlEscape(s.path("scenarioTitle").asText())
                + ".</p><p>"
                + HtmlUtils.htmlEscape(s.path("enterpriseName").asText())
                + " — "
                + c.completedAt()
                + "</p><p>This certificate records simulation completion only. It does not certify professional competence.</p><p>CareerSim · "
                + c.certificateCode()
                + "</p></html>";
    }
}
