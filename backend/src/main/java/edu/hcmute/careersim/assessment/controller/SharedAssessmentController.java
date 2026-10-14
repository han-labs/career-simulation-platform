package edu.hcmute.careersim.assessment.controller;

import edu.hcmute.careersim.assessment.access.SharedAssessmentAccess;
import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/enterprise/students")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class SharedAssessmentController {
    private final SharedAssessmentAccess assessments;
    private final ActorAccess actors;

    @GetMapping("/{id}/assessments")
    public ApiResponse<?> read(Principal p, @PathVariable String id) {
        return ApiResponse.success(
                assessments.readShared(actors.require(p).id(), CoreValues.id(id)));
    }
}
