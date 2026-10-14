package edu.hcmute.careersim.simulation.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.access.SharedSimulationAccess;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/enterprise")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class SharedSimulationController {
    private final SharedSimulationAccess simulations;
    private final ActorAccess actors;

    @GetMapping("/students/{id}/completions")
    public ApiResponse<?> completions(Principal p, @PathVariable String id) {
        return ApiResponse.success(
                simulations.readCompletions(actors.require(p).id(), CoreValues.id(id)));
    }

    @GetMapping("/submissions/{id}")
    public ApiResponse<?> submission(Principal p, @PathVariable String id) {
        return ApiResponse.success(
                simulations.readSubmission(actors.require(p).id(), CoreValues.id(id)));
    }
}
