package edu.hcmute.careersim.guidance.agent.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Confirmation;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/admin")
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class AgentAdminController {
    private final AgentRegistryService registry;
    private final ExecutionService executions;
    private final ActorAccess actors;

    @GetMapping("/agent-platform/options")
    public ApiResponse<?> options(Principal p) {
        return ApiResponse.success(executions.options(actors.admin(p).id()));
    }

    @GetMapping("/agents")
    public ApiResponse<?> list(Principal p) {
        return ApiResponse.success(registry.list(actors.admin(p).id()));
    }

    @PostMapping("/agents")
    public ApiResponse<?> create(Principal p, @RequestBody AgentInput r) {
        return ApiResponse.success(registry.create(actors.admin(p).id(), r));
    }

    @GetMapping("/agents/{agent}")
    public ApiResponse<?> get(Principal p, @PathVariable UUID agent) {
        return ApiResponse.success(registry.get(actors.admin(p).id(), agent));
    }

    @PatchMapping("/agents/{agent}")
    public ApiResponse<?> update(
            Principal p,
            @PathVariable UUID agent,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @RequestBody AgentInput r) {
        return ApiResponse.success(
                registry.update(actors.admin(p).id(), agent, CoreValues.revision(revision), r));
    }

    @DeleteMapping("/agents/{agent}")
    public ApiResponse<?> archive(
            Principal p,
            @PathVariable UUID agent,
            @RequestHeader(value = "If-Match", required = false) String revision) {
        return ApiResponse.success(
                registry.archive(actors.admin(p).id(), agent, CoreValues.revision(revision)));
    }

    @GetMapping("/agents/{agent}/configurations")
    public ApiResponse<?> configurations(Principal p, @PathVariable UUID agent) {
        return ApiResponse.success(registry.configurations(actors.admin(p).id(), agent));
    }

    @PostMapping("/agents/{agent}/configurations")
    public ApiResponse<?> save(
            Principal p,
            @PathVariable UUID agent,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @RequestBody ConfigurationInput r) {
        return ApiResponse.success(
                registry.save(actors.admin(p).id(), agent, CoreValues.revision(revision), r));
    }

    @GetMapping("/agents/{agent}/configurations/{config}")
    public ApiResponse<?> config(Principal p, @PathVariable UUID agent, @PathVariable UUID config) {
        return ApiResponse.success(registry.configuration(actors.admin(p).id(), agent, config));
    }

    @PostMapping("/agents/{agent}/configurations/{config}/previews")
    public ResponseEntity<?> preview(
            Principal p,
            @PathVariable UUID agent,
            @PathVariable UUID config,
            @RequestBody Preview r) {
        return ResponseEntity.accepted()
                .body(
                        ApiResponse.success(
                                executions.preview(actors.admin(p).id(), agent, config, r)));
    }

    @PostMapping("/agents/{agent}/configurations/{config}/publish")
    public ApiResponse<?> publish(
            Principal p,
            @PathVariable UUID agent,
            @PathVariable UUID config,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @RequestBody Confirmation r) {
        return ApiResponse.success(
                registry.publish(
                        actors.admin(p).id(),
                        agent,
                        config,
                        CoreValues.revision(revision),
                        r.confirmed()));
    }

    @GetMapping("/agents/{agent}/assignments")
    public ApiResponse<?> assignments(Principal p, @PathVariable UUID agent) {
        return ApiResponse.success(registry.assignments(actors.admin(p).id(), agent));
    }

    @PostMapping("/agents/{agent}/assignments")
    public ApiResponse<?> assign(Principal p, @PathVariable UUID agent, @RequestBody Assign r) {
        return ApiResponse.success(
                registry.assign(actors.admin(p).id(), agent, CoreValues.id(r.studentUserId())));
    }

    @PostMapping("/agents/{agent}/assignments/{assignment}/revoke")
    public ApiResponse<?> revoke(
            Principal p, @PathVariable UUID agent, @PathVariable UUID assignment) {
        return ApiResponse.success(registry.revoke(actors.admin(p).id(), agent, assignment));
    }

    @GetMapping("/agent-executions/{id}")
    public ApiResponse<?> execution(Principal p, @PathVariable UUID id) {
        return ApiResponse.success(executions.read(actors.admin(p).id(), id));
    }

    @PostMapping("/agent-executions/{id}/cancel")
    public ApiResponse<?> cancel(Principal p, @PathVariable UUID id) {
        return ApiResponse.success(executions.cancel(actors.admin(p).id(), id));
    }
}
