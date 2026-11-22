package edu.hcmute.careersim.simulation.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.domain.AnalyticsFilter;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Analytics;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.AnalyticsEvidencePage;
import edu.hcmute.careersim.simulation.service.ScenarioAnalyticsService;
import java.security.Principal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/enterprises/{id}/analytics")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class ScenarioAnalyticsController {
    private final ActorAccess actors;
    private final ScenarioAnalyticsService service;

    @GetMapping
    public ApiResponse<Analytics> analytics(
            Principal principal,
            @PathVariable String id,
            @RequestParam(required = false) String scenarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(
                service.analytics(
                        actors.require(principal).id(),
                        CoreValues.id(id),
                        AnalyticsFilter.of(scenarioId, from, to, status)));
    }

    @GetMapping("/evidence")
    public ApiResponse<AnalyticsEvidencePage> evidence(
            Principal principal,
            @PathVariable String id,
            @RequestParam(required = false) String scenarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(
                service.evidence(
                        actors.require(principal).id(),
                        CoreValues.id(id),
                        AnalyticsFilter.of(scenarioId, from, to, status),
                        before,
                        limit));
    }
}
