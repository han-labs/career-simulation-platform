package edu.hcmute.careersim.identity.service.impl;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.dao.IdentityDao;
import edu.hcmute.careersim.identity.dto.IdentityContracts.Grant;
import edu.hcmute.careersim.identity.service.ConsentService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsentServiceImpl implements ConsentService {
    public static final String POLICY = "careersim-purpose-v1";
    private static final Set<String> SHARE =
            Set.of("PROFILE_SHARE", "COMPLETION_SHARE", "SUBMISSION_SHARE", "ASSESSMENT_SHARE");
    private final IdentityDao dao;
    private final ActorAccess actors;
    private final AuditWriter audit;
    private final List<ConsentRevocationListener> listeners;
    private final PrivacyAdmission admission;
    private final org.springframework.transaction.PlatformTransactionManager transactions;

    private <T> T change(long owner, java.util.function.Supplier<T> work) {
        return admission.serialized(
                owner,
                () -> {
                    var transaction =
                            new org.springframework.transaction.support.TransactionTemplate(
                                    transactions);
                    transaction.setPropagationBehavior(
                            org.springframework.transaction.TransactionDefinition
                                    .PROPAGATION_REQUIRES_NEW);
                    return transaction.execute(status -> work.get());
                });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(long owner) {
        actors.requireId(owner);
        return dao.grants(owner);
    }

    public String grant(long owner, Grant g) {
        return change(owner, () -> grantLocked(owner, g));
    }

    private String grantLocked(long owner, Grant g) {
        var a = actors.lock(owner);
        if (!g.confirmed() || !POLICY.equals(g.policyVersion()))
            CoreValues.invalid("Explicit confirmation of the current policy is required.");
        Long enterprise = null;
        if (SHARE.contains(g.purpose())) {
            ActorAccess.requireRole(a, "STUDENT");
            enterprise = CoreValues.id(g.enterpriseId());
            if (!dao.enterpriseExists(enterprise)
                    || !g.recipientKey().equals("enterprise:" + enterprise))
                CoreValues.invalid("Invalid enterprise recipient.");
        } else if ("EXTERNAL_AI".equals(g.purpose())) {
            if (g.enterpriseId() != null
                    || !Set.of("provider:gemini", "provider:openai").contains(g.recipientKey()))
                CoreValues.invalid("Unsupported provider recipient.");
        } else if ("LONG_TERM_MEMORY".equals(g.purpose())) {
            if (g.enterpriseId() != null || !"platform-memory".equals(g.recipientKey()))
                CoreValues.invalid("Invalid memory recipient.");
        } else CoreValues.invalid("This consent purpose is not enabled.");
        dao.expire(owner, g.purpose(), g.recipientKey());
        var existing = dao.currentGrant(owner, g.purpose(), g.recipientKey());
        if (existing.isPresent()) return existing.get().toString();
        long id = dao.grant(owner, g, enterprise);
        dao.privacy(owner);
        audit.record(
                owner,
                "CONSENT_CONFIRMED",
                "consent_grant",
                id,
                Map.of("purpose", g.purpose(), "recipient", g.recipientKey()));
        return Long.toString(id);
    }

    public void revoke(long owner, long id) {
        change(
                owner,
                () -> {
                    revokeLocked(owner, id);
                    return null;
                });
    }

    private void revokeLocked(long owner, long id) {
        actors.lock(owner);
        var scope =
                dao.grantIdentity(owner, id)
                        .orElseThrow(() -> new NotFoundException("Grant not found."));
        if (dao.revoke(owner, id)) {
            dao.privacy(owner);
            listeners.forEach(l -> l.revoked(owner, scope[0], scope[1]));
            audit.record(
                    owner,
                    "CONSENT_REVOKED",
                    "consent_grant",
                    id,
                    Map.of("purpose", scope[0], "recipient", scope[1]));
        }
    }
}
