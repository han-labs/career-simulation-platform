package edu.hcmute.careersim.guidance.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.guidance.dto.PlanContracts.Confirm;
import edu.hcmute.careersim.guidance.service.*;
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
public class CoreGuidanceController {
    private final GuidanceEvidenceQuery evidence;
    private final ExplorationPlanService plans;
    private final PlanCommands commands;
    private final ActorAccess actors;

    @GetMapping("/guidance/dashboard")
    public ApiResponse<?> dashboard(Principal p) {
        return ApiResponse.success(evidence.dashboard(actors.student(p).id()));
    }

    @GetMapping("/plans")
    public ApiResponse<?> plans(Principal p) {
        return ApiResponse.success(plans.list(actors.student(p).id()));
    }

    @PostMapping("/plans/confirm")
    public ApiResponse<?> confirm(Principal p, @Valid @RequestBody Confirm r) {
        return ApiResponse.success(commands.confirm(actors.student(p).id(), r));
    }
}
