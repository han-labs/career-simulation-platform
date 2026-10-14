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

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(
                service.list(cursor == null ? null : CoreValues.id(cursor), limit));
    }

    @GetMapping("/{slug}")
    public ApiResponse<?> get(@PathVariable String slug) {
        return ApiResponse.success(service.find(slug));
    }
}
