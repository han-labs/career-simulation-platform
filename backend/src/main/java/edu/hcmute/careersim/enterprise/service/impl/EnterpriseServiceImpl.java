package edu.hcmute.careersim.enterprise.service.impl;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.enterprise.access.EnterpriseAccess;
import edu.hcmute.careersim.enterprise.dao.EnterpriseDao;
import edu.hcmute.careersim.enterprise.service.EnterpriseService;
import edu.hcmute.careersim.identity.access.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnterpriseServiceImpl
        implements EnterpriseService, EnterpriseAccess, EnterpriseRegistration {
    private final EnterpriseDao dao;
    private final ActorAccess actors;
    private final AuditWriter audit;

    @Transactional
    public void createFor(long account, String name) {
        var a = actors.requireId(account);
        ActorAccess.requireRole(a, "ENTERPRISE");
        long id = dao.create(CoreValues.text(name, 240).strip());
        dao.representative(account, id);
        audit.record(
                account, "ENTERPRISE_REGISTERED", "enterprise", id, Map.of("status", "DISABLED"));
    }

    @Transactional(readOnly = true)
    public Membership mine(long actor) {
        var a = actors.requireId(actor);
        ActorAccess.requireRole(a, "ENTERPRISE");
        return dao.membership(actor).orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
    }

    @Transactional(readOnly = true)
    public Membership requireMember(long actor, long enterprise) {
        var m = mine(actor);
        if (!m.enterpriseId().equals(Long.toString(enterprise)))
            throw new NotFoundException("Enterprise not found.");
        if (!"ACTIVE".equals(m.membershipStatus())) throw new ApiException(ErrorCode.FORBIDDEN);
        return m;
    }

    @Transactional(readOnly = true)
    public Membership requirePublisher(long actor, long enterprise) {
        var m = requireMember(actor, enterprise);
        if (!m.publishingPermission()
                || !"VERIFIED".equals(m.verificationStatus())
                || !"ENABLED".equals(m.publishingStatus()))
            throw new ApiException(ErrorCode.FORBIDDEN, "Publication permission is required.");
        return m;
    }

    @Transactional
    public void verification(long actor, long id, long revision, String status) {
        ActorAccess.requireRole(actors.requireId(actor), "ADMIN");
        if (!Set.of("PENDING", "VERIFIED", "REJECTED").contains(status))
            CoreValues.invalid("Invalid verification status.");
        CoreValues.match(revision, dao.lock(id));
        dao.verification(id, status);
        audit.record(actor, "ENTERPRISE_VERIFIED", "enterprise", id, Map.of("status", status));
    }

    @Transactional
    public void publishing(long actor, long id, long revision, String status) {
        ActorAccess.requireRole(actors.requireId(actor), "ADMIN");
        if (!Set.of("DISABLED", "ENABLED", "SUSPENDED").contains(status))
            CoreValues.invalid("Invalid publishing status.");
        CoreValues.match(revision, dao.lock(id));
        dao.publishing(id, status);
        audit.record(
                actor, "ENTERPRISE_PUBLISHING_CHANGED", "enterprise", id, Map.of("status", status));
    }

    @Transactional
    public void member(
            long actor,
            long enterprise,
            long user,
            long revision,
            String status,
            boolean permission) {
        ActorAccess.requireRole(actors.requireId(actor), "ADMIN");
        if (!Set.of("ACTIVE", "SUSPENDED").contains(status))
            CoreValues.invalid("Invalid membership status.");
        if (!"ENTERPRISE".equals(dao.accountRole(user)))
            CoreValues.invalid("Enterprise account required.");
        dao.lock(enterprise);
        var previous = dao.memberEnterprise(user);
        if (previous.isPresent() && previous.get() != enterprise)
            throw new ApiException(
                    ErrorCode.CONFLICT, "Membership transfer requires separate review.");
        if (previous.isEmpty()) {
            CoreValues.match(revision, 0);
            dao.representative(user, enterprise);
        } else CoreValues.match(revision, dao.memberRevision(user));
        dao.membership(user, status, permission);
        audit.record(
                actor,
                "MEMBERSHIP_CHANGED",
                "enterprise_representative",
                user,
                Map.of("status", status));
    }
}
