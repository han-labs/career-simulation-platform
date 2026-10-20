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

    @GetMapping("/v2/admin/agent-usage")
    public ApiResponse<?> usage(
            Principal p,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) String mode) {
        return ApiResponse.success(usage.query(actors.admin(p).id(), from, to, mode));
    }
}
