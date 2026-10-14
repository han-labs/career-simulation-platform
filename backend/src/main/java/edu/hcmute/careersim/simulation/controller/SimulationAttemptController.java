// Accepts simulation answers and returns the persisted deterministic evaluation.
package edu.hcmute.careersim.simulation.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptRequest;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptResponse;
import edu.hcmute.careersim.simulation.mapper.SimulationResultLocaleMapper;
import edu.hcmute.careersim.simulation.service.SimulationAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/simulations")
@RequiredArgsConstructor
public class SimulationAttemptController {

    private final SimulationAttemptService simulationAttemptService;
    private final SimulationResultLocaleMapper localeMapper;
    private final edu.hcmute.careersim.identity.access.ActorAccess actors;

    @org.springframework.beans.factory.annotation.Value("${app.legacy-write-enabled:false}")
    private boolean legacyWrites;

    @PostMapping("/{slug}/attempts")
    public ApiResponse<SubmitAttemptResponse> submitAttempt(
            @PathVariable String slug,
            java.security.Principal principal,
            @RequestHeader(value = "X-Student-Id", required = false) Long studentId,
            @Valid @RequestBody SubmitAttemptRequest request) {
        long owner = actors.student(principal).id();
        if (studentId != null && studentId != owner)
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.FORBIDDEN);
        if (!legacyWrites)
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.WORKFLOW_UPGRADE_REQUIRED);
        return ApiResponse.success(
                localeMapper.localize(
                        simulationAttemptService.submitAttempt(slug, owner, request)));
    }
}
