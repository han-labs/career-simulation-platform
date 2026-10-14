package edu.hcmute.careersim.identity.service.impl;

import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.dao.IdentityDao;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SharedProfileService implements SharedProfileAccess {
    private final ActorAccess actors;
    private final SharingRecipientAccess recipient;
    private final ConsentAccess consent;
    private final IdentityDao dao;

    @Transactional(readOnly = true)
    public Object readShared(long actor, long student) {
        long tenant = recipient.requireEnterprise(actor);
        ActorAccess.requireRole(actors.requireId(student), "STUDENT");
        consent.require(student, "PROFILE_SHARE", "enterprise:" + tenant);
        var a = dao.account(student, java.util.List.of());
        return Map.of(
                "studentId",
                Long.toString(student),
                "displayName",
                a.displayName(),
                "profile",
                a.profile());
    }
}
