package edu.hcmute.careersim.identity.access;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcConsentAccess implements ConsentAccess {
    private final JdbcTemplate jdbc;

    public boolean effective(long user, String purpose, String recipient) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.consent_grant g JOIN public.app_users u ON u.id=g.user_id WHERE g.user_id=? AND u.status='ACTIVE' AND g.purpose=? AND g.recipient_key=? AND g.confirmed_at<=CURRENT_TIMESTAMP AND g.revoked_at IS NULL AND (g.expires_at IS NULL OR g.expires_at>CURRENT_TIMESTAMP))",
                        Boolean.class,
                        user,
                        purpose,
                        recipient));
    }
}
