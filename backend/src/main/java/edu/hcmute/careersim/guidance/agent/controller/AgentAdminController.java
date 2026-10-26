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
    private final AdminPreviewConversationService previewChats;

    @GetMapping("/agent-role-bindings")
    public ApiResponse<?> roleBindings(Principal p) {
        return ApiResponse.success(registry.roleBindings(actors.admin(p).id()));
    }

    @PutMapping("/agents/{agent}/roles")
    public ApiResponse<?> activateRoles(
            Principal p,
            @PathVariable UUID agent,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @RequestBody RoleActivation input) {
        return ApiResponse.success(
                registry.activateRoles(
                        actors.admin(p).id(), agent, CoreValues.revision(revision), input));
    }

    @GetMapping("/agents/{agent}/preview-conversations")
    public ApiResponse<?> chats(
            Principal p,
            @PathVariable UUID agent,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ApiResponse.success(previewChats.list(actors.admin(p).id(), agent, limit, offset));
    }

    @PostMapping("/agents/{agent}/preview-conversations")
    public ApiResponse<?> newChat(
            Principal p,
            @PathVariable UUID agent,
            @RequestBody
                    edu.hcmute.careersim.guidance.agent.dto.AdminPreviewContracts.NewChat input) {
        return ApiResponse.success(previewChats.create(actors.admin(p).id(), agent, input));
    }

    @GetMapping("/agents/{agent}/preview-conversations/{chat}")
    public ApiResponse<?> chat(
            Principal p,
            @PathVariable UUID agent,
            @PathVariable UUID chat,
            @RequestParam(defaultValue = "9223372036854775807") long before,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(
                previewChats.read(actors.admin(p).id(), agent, chat, before, limit));
    }

    @PostMapping("/agents/{agent}/preview-conversations/{chat}/messages")
    public ResponseEntity<?> message(
            Principal p,
            @PathVariable UUID agent,
            @PathVariable UUID chat,
            @RequestBody
                    edu.hcmute.careersim.guidance.agent.dto.AdminPreviewContracts.Message input) {
        return ResponseEntity.accepted()
                .body(
                        ApiResponse.success(
                                previewChats.send(actors.admin(p).id(), agent, chat, input)));
    }

    @PostMapping("/agents/{agent}/preview-conversations/{chat}/clear")
    public ApiResponse<?> clearChat(
            Principal p,
            @PathVariable UUID agent,
            @PathVariable UUID chat,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @RequestBody
                    edu.hcmute.careersim.guidance.agent.dto.AdminPreviewContracts.Change input) {
        return ApiResponse.success(
                previewChats.change(
                        actors.admin(p).id(),
                        agent,
                        chat,
                        CoreValues.revision(revision),
                        input.confirmed(),
                        false));
    }

    @DeleteMapping("/agents/{agent}/preview-conversations/{chat}")
    public ApiResponse<?> deleteChat(
            Principal p,
            @PathVariable UUID agent,
            @PathVariable UUID chat,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @RequestBody
                    edu.hcmute.careersim.guidance.agent.dto.AdminPreviewContracts.Change input) {
        return ApiResponse.success(
                previewChats.change(
                        actors.admin(p).id(),
                        agent,
                        chat,
                        CoreValues.revision(revision),
                        input.confirmed(),
                        true));
    }

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
        actors.admin(p);
        return ApiResponse.success(retiredAssignment());
    }

    @GetMapping("/agents/{agent}/eligible-students")
    public ApiResponse<?> eligibleStudents(
            Principal p,
            @PathVariable UUID agent,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") long afterId,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(
                registry.eligibleStudents(actors.admin(p).id(), agent, q, afterId, limit));
    }

    @GetMapping("/agents/{agent}/configurations/{config}/publication-readiness")
    public ApiResponse<?> publicationReadiness(
            Principal p, @PathVariable UUID agent, @PathVariable UUID config) {
        return ApiResponse.success(
                registry.publicationReadiness(actors.admin(p).id(), agent, config));
    }

    @PostMapping("/agents/{agent}/assignments/{assignment}/revoke")
    public ApiResponse<?> revoke(
            Principal p, @PathVariable UUID agent, @PathVariable UUID assignment) {
        actors.admin(p);
        return ApiResponse.success(retiredAssignment());
    }

    @GetMapping("/agent-executions/{id}")
    public ApiResponse<?> execution(Principal p, @PathVariable UUID id) {
        return ApiResponse.success(executions.read(actors.admin(p).id(), id));
    }

    @PostMapping("/agent-executions/{id}/cancel")
    public ApiResponse<?> cancel(Principal p, @PathVariable UUID id) {
        return ApiResponse.success(executions.cancel(actors.admin(p).id(), id));
    }

    private Object retiredAssignment() {
        throw new edu.hcmute.careersim.common.exception.ApiException(
                edu.hcmute.careersim.common.exception.ErrorCode.CONFLICT,
                "Per-Student assignments are retired. Use role publication.");
    }
}
