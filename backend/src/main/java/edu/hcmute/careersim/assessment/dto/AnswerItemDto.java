package edu.hcmute.careersim.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AnswerItemDto(@NotNull Long questionId, @NotNull @Min(1) @Max(5) Short score) {}
