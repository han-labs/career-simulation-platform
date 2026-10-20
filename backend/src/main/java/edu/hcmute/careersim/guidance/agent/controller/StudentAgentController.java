package edu.hcmute.careersim.guidance.agent.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.guidance.agent.dto.StudentAgentContracts.*;
import edu.hcmute.careersim.guidance.agent.service.StudentAgentService;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class StudentAgentController {
    private final StudentAgentService service;
    private final ActorAccess actors;

    @GetMapping("/v2/agent-sessions")
    public ApiResponse<?> sessions(
            Principal p,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) Instant before,
            @RequestParam(required = false) UUID beforeId,
            @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.success(
                service.sessions(actors.student(p).id(), agentId, before, beforeId, limit));
    }

    @PostMapping("/v2/agents/{agent}/sessions")
    public ApiResponse<?> create(
            Principal p, @PathVariable UUID agent, @RequestBody CreateSession r) {
        return ApiResponse.success(service.create(actors.student(p).id(), agent, r));
    }

    @GetMapping("/v2/agent-sessions/{id}")
    public ApiResponse<?> session(Principal p, @PathVariable UUID id) {
        return ApiResponse.success(service.session(actors.student(p).id(), id));
    }

    @DeleteMapping("/v2/agent-sessions/{id}")
    public ApiResponse<?> delete(Principal p, @PathVariable UUID id) {
        service.delete(actors.student(p).id(), id);
        return ApiResponse.success(Map.of("deleted", true));
    }

    @GetMapping("/v2/agent-sessions/{id}/interactions")
    public ApiResponse<?> history(
            Principal p,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") long afterSequence,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(
                service.history(actors.student(p).id(), id, afterSequence, limit));
    }

    @PostMapping("/v2/agent-sessions/{id}/messages")
    @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    public ApiResponse<?> message(Principal p, @PathVariable UUID id, @RequestBody Message r) {
        return ApiResponse.success(service.submit(actors.student(p).id(), id, "CHAT", r));
    }

    @PostMapping("/v2/agent-sessions/{id}/insight-reports")
    @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    public ApiResponse<?> report(Principal p, @PathVariable UUID id, @RequestBody Message r) {
        return ApiResponse.success(service.submit(actors.student(p).id(), id, "INSIGHT_REPORT", r));
    }

    @PostMapping("/v2/agent-sessions/{id}/plan-suggestions")
    @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    public ApiResponse<?> plan(Principal p, @PathVariable UUID id, @RequestBody Message r) {
        return ApiResponse.success(
                service.submit(actors.student(p).id(), id, "PLAN_SUGGESTION", r));
    }

    @GetMapping("/v2/agent-executions/{id}")
    public ApiResponse<?> run(Principal p, @PathVariable UUID id) {
        return ApiResponse.success(service.read(actors.student(p).id(), id));
    }

    @PostMapping("/v2/agent-executions/{id}/cancel")
    public ApiResponse<?> cancel(Principal p, @PathVariable UUID id) {
        return ApiResponse.success(service.cancel(actors.student(p).id(), id));
    }

    @GetMapping("/v2/insight-reports")
    public ApiResponse<?> reports(Principal p) {
        return ApiResponse.success(service.reports(actors.student(p).id()));
    }

    @GetMapping("/v2/insight-reports/{id}")
    public ApiResponse<?> getReport(Principal p, @PathVariable UUID id) {
        return ApiResponse.success(service.report(actors.student(p).id(), id));
    }

    @PostMapping("/v2/exploration-plans/confirm-suggestion")
    public ApiResponse<?> confirm(Principal p, @RequestBody ConfirmSuggestion r) {
        return ApiResponse.success(service.confirm(actors.student(p).id(), r));
    }
}
