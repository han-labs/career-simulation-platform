package edu.hcmute.careersim.guidance.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.dto.PlanResponse;
import edu.hcmute.careersim.guidance.dto.SavePlanRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import edu.hcmute.careersim.guidance.service.GuidanceService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/guidance")
@RequiredArgsConstructor
public class GuidanceController {

    private final GuidanceService guidanceService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> getDashboard(Principal principal) {
        return ApiResponse.success(guidanceService.getDashboard(principal.getName()));
    }

    @PostMapping("/syn/messages")
    public ApiResponse<SynMessageResponse> sendMessage(
            Principal principal, @Valid @RequestBody SynMessageRequest request) {
        return ApiResponse.success(guidanceService.sendMessage(principal.getName(), request));
    }

    @DeleteMapping("/syn/sessions/{sessionId}")
    public ApiResponse<Boolean> clearSession(Principal principal, @PathVariable UUID sessionId) {
        return ApiResponse.success(
                "Syn conversation memory cleared.",
                guidanceService.clearSynSession(principal.getName(), sessionId));
    }

    @PostMapping("/plans")
    public ApiResponse<PlanResponse> savePlan(
            Principal principal, @Valid @RequestBody SavePlanRequest request) {
        return ApiResponse.success(
                "Exploration plan saved.", guidanceService.savePlan(principal.getName(), request));
    }
}
