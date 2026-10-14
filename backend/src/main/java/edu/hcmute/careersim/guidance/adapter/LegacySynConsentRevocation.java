package edu.hcmute.careersim.guidance.adapter;

import edu.hcmute.careersim.identity.access.ConsentRevocationListener;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Legacy operational payload redaction; immutable objective history is not touched. */
@Repository
@RequiredArgsConstructor
public class LegacySynConsentRevocation implements ConsentRevocationListener {
    private final JdbcTemplate jdbc;

    public void revoked(long owner, String purpose, String recipient) {
        if (!"EXTERNAL_AI".equals(purpose)) return;
        jdbc.update(
                "UPDATE public.syn_turns SET state=CASE WHEN state='PENDING' THEN 'CANCELLED' ELSE state END,response=NULL,reason='CONSENT_REVOKED',completed_at=COALESCE(completed_at,CURRENT_TIMESTAMP) WHERE student_id=?",
                owner);
    }
}
