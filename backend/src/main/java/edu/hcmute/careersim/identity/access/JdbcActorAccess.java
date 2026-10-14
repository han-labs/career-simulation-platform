package edu.hcmute.careersim.identity.access;

import edu.hcmute.careersim.common.exception.*;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcActorAccess implements ActorAccess {
    private final JdbcTemplate jdbc;

    public Actor require(Principal p) {
        Object resolved =
                p instanceof org.springframework.security.core.Authentication authentication
                        ? authentication.getPrincipal()
                        : p;
        if (!(resolved instanceof AccountPrincipal a))
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        return requireId(a.id());
    }

    public Actor requireId(long id) {
        return read(id, false);
    }

    public Actor lock(long id) {
        return read(id, true);
    }

    private Actor read(long id, boolean lock) {
        var rows =
                jdbc.query(
                        "SELECT id,email,display_name,role,status,privacy_version,row_version,EXISTS(SELECT 1 FROM public.student_profiles p WHERE p.user_id=u.id) AS profile FROM public.app_users u WHERE id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        (r, n) -> {
                            if (!"ACTIVE".equals(r.getString("status")))
                                throw new ApiException(
                                        ErrorCode.FORBIDDEN, "Account is not active.");
                            String role = r.getString("role");
                            if ("STUDENT".equals(role) && !r.getBoolean("profile"))
                                throw new ApiException(
                                        ErrorCode.FORBIDDEN, "A student profile is required.");
                            return new Actor(
                                    r.getLong("id"),
                                    r.getString("email"),
                                    r.getString("display_name"),
                                    "ADMINISTRATOR".equals(role) ? "ADMIN" : role,
                                    r.getLong("privacy_version"),
                                    r.getLong("row_version"));
                        },
                        id);
        return rows.stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
