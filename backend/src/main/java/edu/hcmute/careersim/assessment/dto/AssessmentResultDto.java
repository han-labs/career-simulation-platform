package edu.hcmute.careersim.assessment.dto;

import java.util.List;

public record AssessmentResultDto(
        Long attemptId, List<DimensionScoreDto> scores, List<String> topDimensions) {}
