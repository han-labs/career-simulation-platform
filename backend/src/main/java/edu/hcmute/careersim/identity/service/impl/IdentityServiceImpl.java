package edu.hcmute.careersim.identity.service.impl;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.dao.IdentityDao;
import edu.hcmute.careersim.identity.dto.IdentityContracts.*;
import edu.hcmute.careersim.identity.service.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {
    private final IdentityDao dao;
    private final ActorAccess actors;
    private final ConsentService consents;
    private final EnterpriseRegistration enterprise;
    private final PasswordEncoder passwords;
    private final AuditWriter audit;

    @Transactional
    public Account register(Registration r) {
        CoreValues.text(r.displayName(), 150);
        CoreValues.text(r.email(), 255);
        if (r.password() == null
                || r.password().length() < 12
                || r.password().length() > 128
                || (!"STUDENT".equals(r.role()) && !"ENTERPRISE".equals(r.role())))
            CoreValues.invalid("Invalid registration.");
        if ("ENTERPRISE".equals(r.role())) CoreValues.text(r.enterpriseName(), 240);
        long id;
        try {
            id = dao.register(r, passwords.encode(r.password()));
        } catch (DuplicateKeyException e) {
            throw new ApiException(
                    ErrorCode.CONFLICT,
                    "An account with this email already exists. Please sign in.");
        }
        if ("STUDENT".equals(r.role())) dao.profile(id);
        else enterprise.createFor(id, r.enterpriseName());
        audit.record(id, "ACCOUNT_REGISTERED", "app_users", id, Map.of("status", "ACTIVE"));
        return dao.account(id, List.of());
    }

    @Transactional(readOnly = true)
    public AccountPrincipal authenticate(Login r) {
        var c = dao.credential(r.email().strip().toLowerCase(Locale.ROOT));
        boolean valid = false;
        if (c.isPresent() && r.password() != null && r.password().length() <= 128) {
            String hash = c.get().hash();
            try {
                valid =
                        hash != null
                                && (hash.startsWith("{pbkdf2@SpringSecurity_v5_8}")
                                        || hash.matches(
                                                "\\{bcrypt}\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"))
                                && passwords.matches(r.password(), hash);
            } catch (IllegalArgumentException ignored) {
                valid = false;
            }
        }
        if (!valid
                || !"ACTIVE"
                        .equals(
                                c.orElseThrow(
                                                () ->
                                                        new ApiException(
                                                                ErrorCode.UNAUTHORIZED,
                                                                "Invalid sign-in details."))
                                        .status()))
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid sign-in details.");
        var a = actors.requireId(c.get().id());
        return new AccountPrincipal(a.id(), a.email());
    }

    @Transactional(readOnly = true)
    public Account me(long actor) {
        actors.requireId(actor);
        return dao.account(actor, consents.list(actor));
    }

    @Transactional
    public Account profile(long actor, long revision, Profile p) {
        var a = actors.lock(actor);
        ActorAccess.requireRole(a, "STUDENT");
        CoreValues.match(revision, dao.profileRevision(actor));
        if (p.declaredInterests() == null
                || !p.declaredInterests().isArray()
                || p.declaredInterests().size() > 30) CoreValues.invalid("Invalid interests.");
        p.declaredInterests()
                .forEach(
                        v -> {
                            if (!v.isTextual()) CoreValues.invalid("Interest must be text.");
                            CoreValues.text(v.textValue(), 100);
                        });
        dao.updateProfile(actor, p);
        audit.record(
                actor,
                "PROFILE_UPDATED",
                "student_profiles",
                actor,
                Map.of("revision", revision + 1));
        return me(actor);
    }

    @Transactional
    public void status(long actor, long target, long revision, String status) {
        var a = actors.requireId(actor);
        ActorAccess.requireRole(a, "ADMIN");
        if (!Set.of("ACTIVE", "INACTIVE", "LOCKED").contains(status))
            CoreValues.invalid("Invalid account status.");
        CoreValues.match(revision, dao.lockRevision(target));
        dao.status(target, status);
        audit.record(
                actor, "ACCOUNT_STATUS_CHANGED", "app_users", target, Map.of("status", status));
    }
}
