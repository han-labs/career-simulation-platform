package edu.hcmute.careersim.simulation.service.impl;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.access.ParticipationEvidenceAccess;
import edu.hcmute.careersim.simulation.dao.*;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.EnrollmentService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService, ParticipationEvidenceAccess {
    @Transactional(readOnly = true)
    public boolean ownsFinal(long actor, String submission, String version, String skill) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
        return dao.ownsFinal(actor, CoreValues.id(submission), CoreValues.id(version), skill);
    }

    private final EnrollmentDao dao;
    private final ScenarioDao scenarios;
    private final CompletionDao completions;
    private final ActorAccess actors;
    private final AuditWriter audit;

    @Transactional
    public Enrollment enroll(long actor, long version) {
        ActorAccess.requireRole(actors.lock(actor), "STUDENT");
        var existing = dao.existing(actor, version);
        if (existing.isPresent()) return read(actor, existing.get());
        var v = scenarios.version(version, false);
        var root = scenarios.root(CoreValues.id(v.scenarioId()), true);
        if (!"PUBLISHED".equals(v.status()) || !"AVAILABLE".equals(root.status()))
            throw new NotFoundException("Scenario version unavailable for enrollment.");
        long id = dao.enroll(actor, version);
        audit.record(actor, "ENROLLED", "simulation_enrollment", id, Map.of());
        return read(actor, id);
    }

    @Transactional(readOnly = true)
    public Enrollment read(long actor, long id) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
        var e = dao.owned(actor, id, false);
        var v = scenarios.version(e.version(), false);
        if ("SUSPENDED".equals(v.availabilityStatus()))
            throw new ApiException(ErrorCode.FORBIDDEN, "Scenario access is suspended.");
        Set<String> done = dao.finalTasks(id);
        var tasks =
                v.tasks().stream()
                        .map(
                                t ->
                                        new Task(
                                                t.id(),
                                                t.position(),
                                                t.title(),
                                                t.workplaceContext(),
                                                t.instructions(),
                                                t.required(),
                                                t.translations(),
                                                t.definitions(),
                                                t.resources().stream()
                                                        .filter(
                                                                r ->
                                                                        "TASK_ACCESS"
                                                                                        .equals(
                                                                                                r
                                                                                                        .releasePolicy())
                                                                                || done.contains(
                                                                                        t.id()))
                                                        .toList(),
                                                List.of(),
                                                null))
                        .toList();
        return new Enrollment(
                Long.toString(id),
                v.id(),
                e.status(),
                e.revision(),
                e.enrolled(),
                e.started(),
                e.completed(),
                dao.completionId(id).orElse(null),
                scenarios.publicMetadata(v.metadata()),
                tasks,
                dao.required(e.version()),
                dao.finalRequired(id, e.version()));
    }

    @Transactional(readOnly = true)
    public List<Enrollment> list(long actor, long after, int limit) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
        return dao.list(actor, after, Math.max(1, Math.min(limit, 100))).stream()
                .map(id -> read(actor, id))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Participation> readOwned(long actor, int limit) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
        return dao.participation(actor, Math.max(1, Math.min(limit, 100)));
    }

    @Transactional(readOnly = true)
    public List<Completion> completions(long actor) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
        return completions.list(actor);
    }
}
