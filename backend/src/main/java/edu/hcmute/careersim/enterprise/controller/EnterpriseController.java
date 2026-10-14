package edu.hcmute.careersim.enterprise.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.enterprise.service.EnterpriseService;
import edu.hcmute.careersim.identity.access.ActorAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
public class EnterpriseController {
    private final EnterpriseService service;
    private final ActorAccess actors;

    public record State(@NotBlank String status) {}

    public record Member(@NotBlank String membershipStatus, boolean publishingPermission) {}

    @GetMapping("/enterprises/me")
    public ApiResponse<?> mine(Principal p) {
        return ApiResponse.success(service.mine(actors.require(p).id()));
    }

    @PostMapping("/admin/enterprises/{id}/verification")
    public ApiResponse<Boolean> verify(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @Valid @RequestBody State r) {
        service.verification(
                actors.admin(p).id(), CoreValues.id(id), CoreValues.revision(revision), r.status());
        return ApiResponse.success(true);
    }

    @PostMapping("/admin/enterprises/{id}/publishing")
    public ApiResponse<Boolean> publish(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @Valid @RequestBody State r) {
        service.publishing(
                actors.admin(p).id(), CoreValues.id(id), CoreValues.revision(revision), r.status());
        return ApiResponse.success(true);
    }

    @PutMapping("/admin/enterprises/{id}/representatives/{user}")
    public ApiResponse<Boolean> member(
            Principal p,
            @PathVariable String id,
            @PathVariable String user,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @Valid @RequestBody Member r) {
        service.member(
                actors.admin(p).id(),
                CoreValues.id(id),
                CoreValues.id(user),
                CoreValues.revision(revision),
                r.membershipStatus(),
                r.publishingPermission());
        return ApiResponse.success(true);
    }
}
