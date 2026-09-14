// Carries submitted option IDs keyed by simulation task ID.
package edu.hcmute.careersim.simulation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SubmitAttemptRequest(
        @NotNull
        @NotEmpty
        Map<Long, String> answers) {
}
