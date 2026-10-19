package edu.hcmute.careersim.simulation.service.impl;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.dao.*;
import edu.hcmute.careersim.simulation.domain.DeliverableValidator;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskSubmissionServiceImpl implements TaskSubmissionService, ModelAnswerAccess {
    private final EnrollmentDao dao;
    private final LearningPolicyDao learning;
    private final edu.hcmute.careersim.simulation.domain.QuizTaskEvaluator quiz;
    private final ScenarioDao scenarios;
    private final ActorAccess actors;
    private final DeliverableValidator validator;
    private final CompletionService completion;
    private final AuditWriter audit;

    private Task task(EnrollmentDao.Row e, long task) {
        var v = scenarios.version(e.version(), false);
        if ("SUSPENDED".equals(v.availabilityStatus()))
            throw new ApiException(ErrorCode.FORBIDDEN, "Scenario access is suspended.");
        return v.tasks().stream()
                .filter(t -> t.id().equals(Long.toString(task)))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Task not found in the pinned version."));
    }

    private void student(long actor) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
    }

    @Transactional(readOnly = true)
    public Submission read(long actor, long enrollment, long task) {
        student(actor);
        var e = dao.owned(actor, enrollment, false);
        task(e, task);
        learning.requireAccessible(enrollment, e.version(), task);
        var s = dao.submission(enrollment, task, false);
        return s.map(x -> view(x))
                .orElseGet(
                        () ->
                                new Submission(
                                        null,
                                        Long.toString(enrollment),
                                        Long.toString(task),
                                        Long.toString(e.version()),
                                        "DRAFT",
                                        0,
                                        null,
                                        dao.completionId(enrollment).orElse(null),
                                        List.of()));
    }

    private Submission view(EnrollmentDao.SubmissionRow s) {
        return new Submission(
                Long.toString(s.id()),
                Long.toString(s.enrollment()),
                Long.toString(s.task()),
                Long.toString(s.version()),
                s.status(),
                s.revision(),
                s.submitted(),
                dao.completionId(s.enrollment()).orElse(null),
                dao.items(s.id()),
                learning.score(s.id()),
                learning.reviewed(s.id()),
                learning.actor(s.id()));
    }

    @Transactional
    public Submission save(long actor, long enrollment, long task, long revision, Items r) {
        ActorAccess.requireRole(actors.lock(actor), "STUDENT");
        var e = dao.owned(actor, enrollment, true);
        var t = task(e, task);
        learning.requireAccessible(enrollment, e.version(), task);
        var s = dao.draft(e, task);
        if ("FINAL".equals(s.status())) throw new ApiException(ErrorCode.FINAL_SUBMISSION_LOCKED);
        CoreValues.match(revision, s.revision());
        var items = validator.validate(actor, t.definitions(), r.items(), false);
        dao.save(s, items);
        dao.started(enrollment);
        audit.record(
                actor,
                "SUBMISSION_DRAFT_SAVED",
                "task_submission",
                s.id(),
                Map.of("revision", s.revision() + 1));
        return view(dao.submission(enrollment, task, false).orElseThrow());
    }

    @Transactional
    public Submission finalizeTask(
            long actor, long enrollment, long task, long revision, Finalize r) {
        ActorAccess.requireRole(actors.lock(actor), "STUDENT");
        var e = dao.owned(actor, enrollment, true);
        var t = task(e, task);
        learning.requireAccessible(enrollment, e.version(), task);
        if (r.requestId() == null) CoreValues.invalid("Final request UUID required.");
        var s = dao.draft(e, task);
        var items = validator.validate(actor, t.definitions(), r.items(), true);
        String hash = validator.hash(actor, enrollment, task, e.version(), items);
        if ("FINAL".equals(s.status())) {
            if (r.requestId().equals(s.request()) && hash.equals(s.hash())) return view(s);
            throw new ApiException(ErrorCode.FINAL_SUBMISSION_LOCKED);
        }
        CoreValues.match(revision, s.revision());
        var used = dao.usedRequest(enrollment, r.requestId());
        if (used.isPresent())
            throw new ApiException(
                    ErrorCode.CONFLICT, "Request UUID is already used by another task.");
        dao.save(s, items);
        dao.started(enrollment);
        dao.finalizeTask(s.id(), r.requestId(), hash);
        learning.finalized(s.id(), actor, t, quiz.evaluate(t, items));
        completion.completeIfEligible(actor, enrollment);
        audit.record(actor, "TASK_FINALIZED", "task_submission", s.id(), Map.of("hash", hash));
        return view(dao.submission(enrollment, task, false).orElseThrow());
    }

    @Transactional
    public Submission selfReview(long actor, long enrollment, long task) {
        ActorAccess.requireRole(actors.lock(actor), "STUDENT");
        var e = dao.owned(actor, enrollment, true);
        task(e, task);
        learning.requireAccessible(enrollment, e.version(), task);
        var s =
                dao.submission(enrollment, task, true)
                        .filter(x -> "FINAL".equals(x.status()))
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ErrorCode.CONFLICT,
                                                "Finalize this task before self-review."));
        learning.review(s.id(), actor);
        audit.record(actor, "TASK_SELF_REVIEWED", "task_submission", s.id(), Map.of());
        return view(s);
    }

    @Transactional(readOnly = true)
    public Released readAnswer(long actor, long enrollment, long task) {
        student(actor);
        var e = dao.owned(actor, enrollment, false);
        var t = task(e, task);
        learning.requireAccessible(enrollment, e.version(), task);
        if (dao.submission(enrollment, task, false)
                .filter(s -> "FINAL".equals(s.status()))
                .isEmpty())
            throw new ApiException(
                    ErrorCode.FORBIDDEN, "Finalize this task before viewing its model answer.");
        return new Released(
                t.modelAnswer(),
                t.resources().stream()
                        .filter(r -> "AFTER_SUBMISSION".equals(r.releasePolicy()))
                        .toList());
    }
}
