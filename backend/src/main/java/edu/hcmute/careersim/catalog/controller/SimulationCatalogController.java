package edu.hcmute.careersim.catalog.controller;

import edu.hcmute.careersim.catalog.dto.SimulationSummaryResponse;
import edu.hcmute.careersim.catalog.service.SimulationCatalogService;
import edu.hcmute.careersim.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/simulations")
@RequiredArgsConstructor
public class SimulationCatalogController {

    private final SimulationCatalogService simulationCatalogService;

    @GetMapping
    public ApiResponse<List<SimulationSummaryResponse>> getPublishedSimulations() {
        return ApiResponse.success(simulationCatalogService.getPublishedSimulations());
    }

    @GetMapping("/{slug}")
    public ApiResponse<SimulationSummaryResponse> getPublishedSimulation(
            @PathVariable String slug) {
        return ApiResponse.success(simulationCatalogService.getPublishedSimulation(slug));
    }
}
