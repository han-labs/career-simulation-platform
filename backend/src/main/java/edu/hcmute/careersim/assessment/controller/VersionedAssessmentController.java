package edu.hcmute.careersim.assessment.controller;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.assessment.service.*;
import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class VersionedAssessmentController {
    private final VersionedAssessmentService service;
    private final AssessmentAuthoringService authoring;
    private final ActorAccess actors;

    @GetMapping("/assessment-types")
    public ApiResponse<?> catalog() {
        return ApiResponse.success(service.catalog());
    }

    @PostMapping("/assessment-versions/{id}/attempts")
    public ApiResponse<?> start(Principal p, @PathVariable String id, @Valid @RequestBody Start r) {
        return ApiResponse.success(
                service.start(actors.student(p).id(), CoreValues.id(id), r.requestId()));
    }

    @GetMapping("/assessment-attempts")
    public ApiResponse<?> history(Principal p) {
        return ApiResponse.success(service.history(actors.student(p).id()));
    }

    @GetMapping("/assessment-attempts/{id}")
    public ApiResponse<?> get(Principal p, @PathVariable String id) {
        return ApiResponse.success(service.get(actors.student(p).id(), CoreValues.id(id)));
    }

    @PutMapping("/assessment-attempts/{id}/responses")
    public ApiResponse<?> save(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String rev,
            @Valid @RequestBody Responses r) {
        return ApiResponse.success(
                service.save(
                        actors.student(p).id(), CoreValues.id(id), CoreValues.revision(rev), r));
    }

    @PostMapping("/assessment-attempts/{id}/submit")
    public ApiResponse<?> submit(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String rev,
            @Valid @RequestBody Responses r) {
        return ApiResponse.success(
                service.submit(
                        actors.student(p).id(), CoreValues.id(id), CoreValues.revision(rev), r));
    }

    @GetMapping("/assessment-attempts/{id}/result")
    public ApiResponse<?> result(Principal p, @PathVariable String id) {
        return ApiResponse.success(service.result(actors.student(p).id(), CoreValues.id(id)));
    }

    @PostMapping("/admin/assessment-types")
    public ApiResponse<?> type(Principal p, @Valid @RequestBody TypeRequest r) {
        return ApiResponse.success(authoring.createType(actors.admin(p).id(), r));
    }

    @GetMapping("/admin/assessment-types")
    public ApiResponse<?> administrationTypes(Principal p) {
        return ApiResponse.success(authoring.administrationTypes(actors.admin(p).id()));
    }

    @GetMapping("/admin/assessment-types/{id}/versions")
    public ApiResponse<?> versions(Principal p, @PathVariable String id) {
        return ApiResponse.success(authoring.versions(actors.admin(p).id(), CoreValues.id(id)));
    }

    @PostMapping("/admin/assessment-types/{id}/versions")
    public ApiResponse<?> version(
            Principal p, @PathVariable String id, @Valid @RequestBody VersionDraft r) {
        return ApiResponse.success(
                authoring.createVersion(actors.admin(p).id(), CoreValues.id(id), r));
    }

    @GetMapping("/admin/assessment-versions/{id}")
    public ApiResponse<?> version(Principal p, @PathVariable String id) {
        return ApiResponse.success(authoring.getVersion(actors.admin(p).id(), CoreValues.id(id)));
    }

    @PutMapping("/admin/assessment-versions/{id}")
    public ApiResponse<?> update(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String rev,
            @Valid @RequestBody VersionDraft r) {
        return ApiResponse.success(
                authoring.update(
                        actors.admin(p).id(), CoreValues.id(id), CoreValues.revision(rev), r));
    }

    @PostMapping("/admin/assessment-versions/{id}/publish")
    public ApiResponse<?> publish(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String rev) {
        return ApiResponse.success(
                authoring.publish(
                        actors.admin(p).id(), CoreValues.id(id), CoreValues.revision(rev)));
    }
}
