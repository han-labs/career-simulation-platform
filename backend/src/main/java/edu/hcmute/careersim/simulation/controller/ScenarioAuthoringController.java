package edu.hcmute.careersim.simulation.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.ScenarioAuthoringService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class ScenarioAuthoringController {
    private final ScenarioAuthoringService service;
    private final ActorAccess actors;

    @PostMapping("/enterprise/scenarios")
    public ApiResponse<?> create(Principal p, @Valid @RequestBody Create r) {
        return ApiResponse.success(service.create(actors.require(p).id(), r));
    }

    @PostMapping("/enterprise/scenarios/{id}/versions")
    public ApiResponse<?> clone(Principal p, @PathVariable String id, @RequestBody Clone r) {
        return ApiResponse.success(
                service.cloneVersion(
                        actors.require(p).id(),
                        CoreValues.id(id),
                        r.sourceVersionId() == null ? null : CoreValues.id(r.sourceVersionId())));
    }

    @GetMapping("/enterprise/scenario-versions/{id}")
    public ApiResponse<?> get(Principal p, @PathVariable String id) {
        return ApiResponse.success(service.get(actors.require(p).id(), CoreValues.id(id)));
    }

    @PutMapping("/enterprise/scenario-versions/{id}")
    public ApiResponse<?> update(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String rev,
            @Valid @RequestBody Draft r) {
        return ApiResponse.success(
                service.update(
                        actors.require(p).id(), CoreValues.id(id), CoreValues.revision(rev), r));
    }

    @PostMapping("/enterprise/scenario-versions/{id}/publish")
    public ApiResponse<?> publish(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String rev) {
        return ApiResponse.success(
                service.publish(
                        actors.require(p).id(), CoreValues.id(id), CoreValues.revision(rev)));
    }

    @PostMapping("/enterprise/scenarios/{id}/availability")
    public ApiResponse<?> availability(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String rev,
            @Valid @RequestBody Availability r) {
        service.availability(
                actors.require(p).id(), CoreValues.id(id), CoreValues.revision(rev), r.status());
        return ApiResponse.success(true);
    }

    @GetMapping("/career-skills")
    public ApiResponse<?> skills(Principal p) {
        return ApiResponse.success(service.skills(actors.require(p).id()));
    }

    @PostMapping("/admin/career-skills")
    public ApiResponse<?> skill(Principal p, @Valid @RequestBody SkillRequest r) {
        return ApiResponse.success(service.skill(actors.admin(p).id(), r));
    }
}
