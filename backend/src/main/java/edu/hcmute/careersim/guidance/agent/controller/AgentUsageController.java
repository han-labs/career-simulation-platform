package edu.hcmute.careersim.guidance.agent.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.guidance.agent.service.AgentUsageService;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.security.Principal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class AgentUsageController {
    private final AgentUsageService usage;
    private final ActorAccess actors;

    @GetMapping(value = "/v2/admin/agent-usage", params = "view=dashboard")
    public ApiResponse<?> dashboard(
            Principal p,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) java.util.UUID agent,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String mode,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        return ApiResponse.success(
                usage.dashboard(
                        actors.admin(p).id(),
                        new edu.hcmute.careersim.guidance.agent.dto.AgentUsageContracts.Filter(
                                from, to, agent, provider, model, outcome, mode, limit, cursor)));
    }

    @GetMapping(value = "/v2/admin/agent-usage", params = "!view")
    public ApiResponse<?> usage(
            Principal p,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) String mode) {
        return ApiResponse.success(usage.query(actors.admin(p).id(), from, to, mode));
    }
}
