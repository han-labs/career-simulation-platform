package edu.hcmute.careersim.guidance.agent.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.guidance.agent.service.AgentRegistryService;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class AssignedAgentController {
    private final AgentRegistryService registry;
    private final ActorAccess actors;

    @GetMapping("/v2/agents")
    public ApiResponse<?> assigned(Principal p) {
        return ApiResponse.success(registry.assigned(actors.student(p).id()));
    }
}
