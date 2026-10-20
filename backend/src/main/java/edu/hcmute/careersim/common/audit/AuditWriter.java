package edu.hcmute.careersim.common.audit;

import edu.hcmute.careersim.common.value.CoreValues;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Only safe status/ID/hash fields; participates in the owning business transaction. */
@Repository
@RequiredArgsConstructor
public class AuditWriter {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actor, String action, String type, long id, Map<String, ?> detail) {
        recordKey(actor, action, type, Long.toString(id), detail);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordKey(
            Long actor, String action, String type, String id, Map<String, ?> detail) {
        CoreValues.text(id, 180);
        if (detail.keySet().stream()
                .anyMatch(
                        k ->
                                !java.util.Set.of(
                                                "status",
                                                "hash",
                                                "reason",
                                                "revision",
                                                "count",
                                                "purpose",
                                                "recipient",
                                                "grantId")
                                        .contains(k)))
            throw new IllegalArgumentException("Unsafe audit field");
        String data = CoreValues.write(detail);
        if (data.length() > 8192) throw new IllegalArgumentException("Audit too large");
        jdbc.update(
                "INSERT INTO public.audit_event(actor_user_id,action,target_type,target_key,detail) VALUES (?,?,?,?,CAST(? AS jsonb))",
                actor,
                action,
                type,
                id,
                data);
    }
}
