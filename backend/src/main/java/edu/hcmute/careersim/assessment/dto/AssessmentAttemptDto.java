package edu.hcmute.careersim.assessment.dto;

import java.time.Instant;

public record AssessmentAttemptDto(
        Long id, String status, Instant startedAt, Instant completedAt) {}
