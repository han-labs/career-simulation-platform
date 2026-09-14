// Exposes published simulation task definitions without evaluation keys.
package edu.hcmute.careersim.simulation.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.simulation.dto.SimulationDetailResponse;
import edu.hcmute.careersim.simulation.service.SimulationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/simulations")
@RequiredArgsConstructor
public class SimulationQueryController {

    private final SimulationQueryService simulationQueryService;

    @GetMapping("/{slug}/tasks")
    public ApiResponse<SimulationDetailResponse> getSimulationWithTasks(
            @PathVariable String slug) {
        return ApiResponse.success(simulationQueryService.getSimulationWithTasks(slug));
    }
}
