package edu.hcmute.careersim.catalog.controller;

import edu.hcmute.careersim.catalog.service.VersionedCatalogService;
import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/scenarios")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class VersionedCatalogController {
    private final VersionedCatalogService service;

    @GetMapping(params = "paged=true")
    public ApiResponse<?> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String careerTrack,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String industry,
            @RequestParam(defaultValue = "ID_ASC") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(
                service.search(
                        new edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery(
                                q, careerTrack, difficulty, sort, cursor, limit, industry)));
    }

    @GetMapping(params = "!paged")
    public ApiResponse<?> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String industry,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(
                service.list(
                        cursor == null ? null : CoreValues.id(cursor), limit, industry));
    }

    @GetMapping("/{slug}")
    public ApiResponse<?> get(@PathVariable String slug) {
        return ApiResponse.success(service.find(slug));
    }
}
