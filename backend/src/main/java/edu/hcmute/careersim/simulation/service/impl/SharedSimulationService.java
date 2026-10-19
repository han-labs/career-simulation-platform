package edu.hcmute.careersim.simulation.service.impl;

import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.storage.FileUseAuthorizer;
import edu.hcmute.careersim.enterprise.access.EnterpriseAccess;
import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.simulation.access.SharedSimulationAccess;
import edu.hcmute.careersim.simulation.dao.*;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SharedSimulationService implements SharedSimulationAccess, FileUseAuthorizer {
    private final SharingRecipientAccess recipient;
    private final ActorAccess actors;
    private final ConsentAccess consent;
    private final EnterpriseAccess enterprises;
    private final SimulationAccessDao access;
    private final EnrollmentDao enrollments;
    private final LearningPolicyDao learning;
    private final CompletionDao completions;

    @Transactional(readOnly = true)
    public List<Completion> readCompletions(long actor, long student) {
        long tenant = recipient.requireEnterprise(actor);
        ActorAccess.requireRole(actors.requireId(student), "STUDENT");
        consent.require(student, "COMPLETION_SHARE", "enterprise:" + tenant);
        return completions.list(student);
    }

    private void shared(long actor, SimulationAccessDao.Work w) {
        long tenant = recipient.requireEnterprise(actor);
        if (tenant != w.enterprise()) throw new NotFoundException("Submission not found.");
        consent.require(w.owner(), "SUBMISSION_SHARE", "enterprise:" + tenant);
        if (!"FINAL".equals(w.status()) || "SUSPENDED".equals(w.availability()))
            throw new NotFoundException("Submission unavailable.");
    }

    @Transactional(readOnly = true)
    public Submission readSubmission(long actor, long id) {
        var w = access.work(id);
        shared(actor, w);
        var s = enrollments.submission(w.enrollment(), w.task(), false).orElseThrow();
        return new Submission(
                Long.toString(s.id()),
                Long.toString(s.enrollment()),
                Long.toString(s.task()),
                Long.toString(s.version()),
                s.status(),
                s.revision(),
                s.submitted(),
                null,
                enrollments.items(id));
    }

    public boolean supports(String type) {
        return Set.of("TASK_RESOURCE", "SUBMISSION").contains(type);
    }

    @Transactional(readOnly = true)
    public void authorize(long actor, long asset, String type, long use) {
        var a = actors.requireId(actor);
        if ("SUBMISSION".equals(type)) {
            var w = access.work(use);
            if (!access.attached(use, asset))
                throw new NotFoundException("File association not found.");
            if (a.id() != w.owner()) shared(actor, w);
            else {
                ActorAccess.requireRole(a, "STUDENT");
                if ("SUSPENDED".equals(w.availability()))
                    throw new ApiException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        var r = access.resource(use);
        if (r.asset() != asset) throw new NotFoundException("File association not found.");
        if ("ENTERPRISE".equals(a.role())) {
            enterprises.requireMember(actor, r.enterprise());
            return;
        }
        ActorAccess.requireRole(a, "STUDENT");
        if ("SUSPENDED".equals(r.availability())) throw new ApiException(ErrorCode.FORBIDDEN);
        long e =
                access.enrollment(actor, r.version())
                        .orElseThrow(() -> new NotFoundException("Resource not found."));
        learning.requireAccessible(e, r.version(), r.task());
        if ("AFTER_SUBMISSION".equals(r.release())
                && enrollments
                        .submission(e, r.task(), false)
                        .filter(s -> "FINAL".equals(s.status()))
                        .isEmpty()) throw new ApiException(ErrorCode.FORBIDDEN);
    }
}
