package edu.hcmute.careersim.identity.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.*;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/enterprise/students")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class SharedProfileController {
    private final SharedProfileAccess profiles;
    private final ActorAccess actors;

    @GetMapping("/{id}/profile")
    public ApiResponse<?> read(Principal p, @PathVariable String id) {
        return ApiResponse.success(profiles.readShared(actors.require(p).id(), CoreValues.id(id)));
    }
}
