package edu.hcmute.careersim.assessment.service.impl;

import edu.hcmute.careersim.assessment.access.VersionedAssessmentAccess;
import edu.hcmute.careersim.assessment.dao.VersionedAssessmentDao;
import edu.hcmute.careersim.assessment.domain.AssessmentEvaluatorRegistry;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.assessment.service.*;
import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VersionedAssessmentServiceImpl
        implements VersionedAssessmentService,
                AssessmentAuthoringService,
                VersionedAssessmentAccess {
    private final VersionedAssessmentDao dao;
    private final AssessmentEvaluatorRegistry evaluator;
    private final ActorAccess actors;
    private final AuditWriter audit;

    @Transactional(readOnly = true)
    public List<Type> administrationTypes(long actor) {
        ActorAccess.requireRole(actors.requireId(actor), "ADMIN");
        return dao.administrationTypes();
    }

    @Transactional(readOnly = true)
    public List<Version> versions(long actor, long type) {
        ActorAccess.requireRole(actors.requireId(actor), "ADMIN");
        return dao.versions(type);
    }

    private void student(long actor) {
        ActorAccess.requireRole(actors.requireId(actor), "STUDENT");
    }

    @Transactional(readOnly = true)
    public Version getVersion(long actor, long id) {
        ActorAccess.requireRole(actors.requireId(actor), "ADMIN");
        return dao.version(id, false);
    }

    @Transactional(readOnly = true)
    public List<Type> catalog() {
        return dao.catalog();
    }

    @Transactional
    public String createType(long actor, TypeRequest r) {
        ActorAccess.requireRole(actors.lock(actor), "ADMIN");
        CoreValues.text(r.code(), 80);
        CoreValues.text(r.name(), 160);
        return Long.toString(dao.type(r));
    }

    @Transactional
    public Version createVersion(long actor, long type, VersionDraft r) {
        ActorAccess.requireRole(actors.lock(actor), "ADMIN");
        dao.lockType(type);
        evaluator.publication(r);
        long id = dao.version(type, r);
        dao.questions(id, r);
        return dao.version(id, false);
    }

    @Transactional
    public Version update(long actor, long id, long revision, VersionDraft r) {
        ActorAccess.requireRole(actors.lock(actor), "ADMIN");
        var v = dao.version(id, true);
        draft(v);
        CoreValues.match(revision, v.revision());
        evaluator.publication(r);
        dao.draft(id, r);
        return dao.version(id, false);
    }

    @Transactional
    public Version publish(long actor, long id, long revision) {
        ActorAccess.requireRole(actors.lock(actor), "ADMIN");
        var preliminary = dao.version(id, false);
        dao.lockType(CoreValues.id(preliminary.typeId()));
        var v = dao.version(id, true);
        draft(v);
        CoreValues.match(revision, v.revision());
        evaluator.publication(toDraft(v));
        String hash = CoreValues.hash(toDraft(v));
        dao.publish(id, hash, CoreValues.id(v.typeId()));
        audit.record(actor, "ASSESSMENT_PUBLISHED", "assessment_version", id, Map.of("hash", hash));
        return dao.version(id, false);
    }

    private VersionDraft toDraft(Version v) {
        return new VersionDraft(
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
    }

    private void draft(Version v) {
        if (!"DRAFT".equals(v.status()))
            throw new ApiException(ErrorCode.CONFLICT, "Published content is immutable.");
    }

    @Transactional
    public Attempt start(long actor, long version, UUID request) {
        ActorAccess.requireRole(actors.lock(actor), "STUDENT");
        if (request == null) CoreValues.invalid("Request UUID required.");
        var existing = dao.request(actor, request);
        if (existing.isPresent()) {
            if (existing.get().version() != version)
                throw new ApiException(
                        ErrorCode.CONFLICT, "Request UUID belongs to another version.");
            return dao.view(actor, existing.get().id());
        }
        var v = dao.version(version, false);
        if (!"PUBLISHED".equals(v.status()) || !dao.typeActive(CoreValues.id(v.typeId())))
            throw new NotFoundException("Published assessment unavailable.");
        long id = dao.start(actor, version, request);
        audit.record(actor, "ASSESSMENT_STARTED", "assessment_attempt", id, Map.of());
        return dao.view(actor, id);
    }

    @Transactional(readOnly = true)
    public Attempt get(long actor, long id) {
        student(actor);
        return dao.view(actor, id);
    }

    @Transactional
    public Attempt save(long actor, long id, long revision, Responses r) {
        ActorAccess.requireRole(actors.lock(actor), "STUDENT");
        var a = dao.owned(actor, id, true);
        if (!"IN_PROGRESS".equals(a.status()))
            throw new ApiException(ErrorCode.CONFLICT, "Submitted work is immutable.");
        CoreValues.match(revision, a.revision());
        dao.responses(a, evaluator.validate(dao.questions(a.version()), r.items(), false));
        return dao.view(actor, id);
    }

    @Transactional
    public Result submit(long actor, long id, long revision, Responses r) {
        ActorAccess.requireRole(actors.lock(actor), "STUDENT");
        var a = dao.owned(actor, id, true);
        var items = evaluator.validate(dao.questions(a.version()), r.items(), true);
        String hash = CoreValues.hash(items);
        if ("SUBMITTED".equals(a.status())) {
            if (!hash.equals(a.hash()))
                throw new ApiException(ErrorCode.CONFLICT, "Submitted payload differs.");
            return dao.result(actor, id);
        }
        CoreValues.match(revision, a.revision());
        var v = dao.version(a.version(), false);
        var result = evaluator.evaluate(v, dao.typeCode(CoreValues.id(v.typeId())), items);
        dao.responses(a, items);
        dao.result(a, v, hash, result);
        audit.record(actor, "ASSESSMENT_SUBMITTED", "assessment_attempt", id, Map.of("hash", hash));
        return dao.result(actor, id);
    }

    @Transactional(readOnly = true)
    public Result result(long actor, long id) {
        student(actor);
        return dao.result(actor, id);
    }

    @Transactional(readOnly = true)
    public List<Result> readOwned(long actor) {
        student(actor);
        return dao.results(actor);
    }
}
