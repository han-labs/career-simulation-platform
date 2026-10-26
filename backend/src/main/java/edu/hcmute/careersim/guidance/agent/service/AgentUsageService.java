package edu.hcmute.careersim.guidance.agent.service;

import java.time.Instant;
import java.util.Map;

public interface AgentUsageService {
    Map<String, Object> query(long actor, Instant from, Instant to, String mode);

    edu.hcmute.careersim.guidance.agent.dto.AgentUsageContracts.Dashboard dashboard(
            long actor, edu.hcmute.careersim.guidance.agent.dto.AgentUsageContracts.Filter filter);
}
