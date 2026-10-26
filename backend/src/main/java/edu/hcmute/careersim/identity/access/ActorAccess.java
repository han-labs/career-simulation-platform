package edu.hcmute.careersim.identity.access;

import java.security.Principal;

public interface ActorAccess {
    Actor require(Principal principal);

    Actor requireId(long id);

    Actor lock(long id);

    /** Read-only selection for the isolated local development principal. Never provisions users. */
    java.util.Optional<Actor> findActiveAdministrator();

    /** Minimal active Student candidates; excludes accounts without the required profile. */
    java.util.List<StudentIdentity> eligibleStudents(String query, long afterId, int limit);

    record StudentIdentity(String id, String displayName) {}

    default Actor student(Principal p) {
        Actor a = require(p);
        requireRole(a, "STUDENT");
        return a;
    }

    default Actor admin(Principal p) {
        Actor a = require(p);
        requireRole(a, "ADMIN");
        return a;
    }

    static void requireRole(Actor a, String role) {
        if (!role.equals(a.role()))
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.FORBIDDEN);
    }

    record Actor(
            long id,
            String email,
            String displayName,
            String role,
            long privacyVersion,
            long revision) {}
}
