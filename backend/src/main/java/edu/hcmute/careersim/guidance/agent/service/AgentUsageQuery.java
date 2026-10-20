package edu.hcmute.careersim.guidance.agent.service;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Aggregate projections only. No Student identifiers, prompts, reports, arguments or transcripts.
 */
@Service
@RequiredArgsConstructor
public class AgentUsageQuery implements AgentUsageService {
    private final edu.hcmute.careersim.guidance.agent.dao.AgentUsageDao usage;
    private final ActorAccess actors;

    public Map<String, Object> query(long admin, Instant from, Instant to, String mode) {
        ActorAccess.requireRole(actors.requireId(admin), "ADMIN");
        if (from == null
                || to == null
                || !to.isAfter(from)
                || Duration.between(from, to).toDays() > 366
                || mode != null
                        && !Set.of("STUDENT_CHAT", "ADMIN_PREVIEW", "KB_TEST").contains(mode))
            CoreValues.invalid("Invalid usage range or mode.");
        return usage.aggregate(from, to, mode);
    }
}
