// Accepts simulation answers and returns the persisted deterministic evaluation.
package edu.hcmute.careersim.simulation.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptRequest;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptResponse;
import jakarta.validation.Valid;
import edu.hcmute.careersim.simulation.service.SimulationAttemptService;
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

    @PostMapping("/{slug}/attempts")
    public ApiResponse<SubmitAttemptResponse> submitAttempt(
            @PathVariable String slug,
            @RequestHeader(value = "X-Student-Id", defaultValue = "1") Long studentId,
            @Valid @RequestBody SubmitAttemptRequest request) {
        // TODO(auth): replace header-based studentId with JWT claim when identity module is ready
        // Tracking: .agent/reports/ai/2026-09-11-us-02-frontend.md
        return ApiResponse.success(
                simulationAttemptService.submitAttempt(slug, studentId, request));
    }
}
