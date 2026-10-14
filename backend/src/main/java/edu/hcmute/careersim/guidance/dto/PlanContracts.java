package edu.hcmute.careersim.guidance.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class PlanContracts {
    private PlanContracts() {}

    public record Confirm(
            String expectedActivePlanId,
            Long expectedRevision,
            @NotBlank @Size(max = 120) String title,
            @NotEmpty @Size(max = 5) List<@NotBlank @Size(max = 240) String> steps) {}

    public record Plan(
            String id,
            String title,
            List<String> steps,
            String status,
            Instant confirmedAt,
            long revision) {}
}
