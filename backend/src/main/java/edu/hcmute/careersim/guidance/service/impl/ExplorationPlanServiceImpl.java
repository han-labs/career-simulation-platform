package edu.hcmute.careersim.guidance.service.impl;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.dao.ConfirmedPlanDao;
import edu.hcmute.careersim.guidance.dto.PlanContracts.*;
import edu.hcmute.careersim.guidance.service.*;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExplorationPlanServiceImpl implements ExplorationPlanService, PlanCommands {
    /**
     * Operator-only reviewed history copy; proof is checked by migration tooling, never exposed
     * over HTTP.
     */
    @Transactional
    public Plan importConfirmed(
            long reviewer, long student, String title, List<String> steps, java.time.Instant at) {
        ActorAccess.requireRole(actors.requireId(reviewer), "ADMIN");
        ActorAccess.requireRole(actors.lock(student), "STUDENT");
        CoreValues.text(title, 120);
        if (steps == null
                || steps.isEmpty()
                || steps.size() > 5
                || at == null
                || at.isAfter(java.time.Instant.now()))
            CoreValues.invalid("Verified historical confirmation required.");
        steps.forEach(s -> CoreValues.text(s, 240));
        if (current(student).isPresent())
            throw new ApiException(
                    ErrorCode.CONFLICT,
                    "Resolve the existing active plan before importing history.");
        long id = dao.importConfirmed(student, title, steps, at);
        audit.record(reviewer, "PLAN_HISTORY_IMPORTED", "exploration_plan", id, Map.of());
        return dao.list(student).stream()
                .filter(p -> p.id().equals(Long.toString(id)))
                .findFirst()
                .orElseThrow();
    }

    private final ConfirmedPlanDao dao;
    private final ActorAccess actors;
    private final AuditWriter audit;

    @Transactional(readOnly = true)
    public List<Plan> list(long actor) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
        return dao.list(actor);
    }

    @Transactional(readOnly = true)
    public Optional<Plan> current(long actor) {
        return list(actor).stream().filter(p -> "ACTIVE".equals(p.status())).findFirst();
    }

    @Transactional
    public Plan confirm(long actor, Confirm r) {
        ActorAccess.requireRole(actors.lock(actor), "STUDENT");
        CoreValues.text(r.title(), 120);
        if (r.steps() == null || r.steps().isEmpty() || r.steps().size() > 5)
            CoreValues.invalid("Plan requires one to five steps.");
        r.steps().forEach(s -> CoreValues.text(s, 240));
        var old = current(actor);
        if (old.isPresent()) {
            if (!old.get().id().equals(r.expectedActivePlanId()) || r.expectedRevision() == null)
                throw new ApiException(ErrorCode.CONFLICT, "The active plan changed.");
            CoreValues.match(r.expectedRevision(), old.get().revision());
        } else if (r.expectedActivePlanId() != null || r.expectedRevision() != null)
            throw new ApiException(ErrorCode.CONFLICT, "There is no active plan to replace.");
        dao.archive(actor);
        long id =
                dao.insert(
                        actor,
                        new Confirm(
                                r.expectedActivePlanId(),
                                r.expectedRevision(),
                                r.title().strip(),
                                r.steps().stream().map(String::strip).toList()));
        audit.record(actor, "PLAN_CONFIRMED", "exploration_plan", id, Map.of());
        return current(actor).orElseThrow();
    }
}
