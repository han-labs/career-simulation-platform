package edu.hcmute.careersim.guidance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SavePlanRequest(
        @NotBlank(message = "Plan title is required.")
                @Size(max = 120, message = "Plan title must not exceed 120 characters.")
                String title,
        @NotEmpty(message = "At least one plan step is required.")
                @Size(max = 5, message = "A plan may contain at most 5 steps.")
                List<
                                @NotBlank(message = "Plan steps must not be blank.")
                                @Size(
                                        max = 240,
                                        message = "Each plan step must not exceed 240 characters.")
                                String>
                        steps) {}
