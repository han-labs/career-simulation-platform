package edu.hcmute.careersim.guidance.agent.service;

import java.time.Instant;
import java.util.Map;

public interface AgentUsageService {
    Map<String, Object> query(long actor, Instant from, Instant to, String mode);
}
