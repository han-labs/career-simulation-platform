package edu.hcmute.careersim.identity.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

public final class IdentityContracts {
    private IdentityContracts() {}

    public record Registration(
            @NotBlank @Email @Size(max = 255) String email,
            @NotNull @Size(min = 12, max = 128) String password,
            @NotBlank @Size(max = 150) String displayName,
            @Pattern(regexp = "STUDENT|ENTERPRISE") @NotNull String role,
            @Size(max = 240) String enterpriseName) {}

    public record Login(@NotBlank String email, @NotNull String password) {}

    public record Profile(
            @Size(max = 255) String schoolName,
            @Size(max = 80) String educationLevel,
            Short graduationYear,
            @Size(max = 160) String major,
            @Size(max = 4000) String careerGoal,
            JsonNode declaredInterests) {}

    public record Status(@NotNull @Pattern(regexp = "ACTIVE|INACTIVE|LOCKED") String status) {}

    public record Grant(
            @NotBlank String purpose,
            String enterpriseId,
            @NotBlank String recipientKey,
            @NotBlank String policyVersion,
            boolean confirmed) {}

    public record Account(
            String id,
            String email,
            String displayName,
            String role,
            String status,
            long revision,
            JsonNode profile,
            Object consents) {}
}
