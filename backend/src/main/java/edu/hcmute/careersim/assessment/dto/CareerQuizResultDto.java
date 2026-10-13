package edu.hcmute.careersim.assessment.dto;

import java.util.List;

public record CareerQuizResultDto(
        Long attemptId,
        String quizType,
        List<CareerQuizScoreDto> scores,
        List<String> topCategories) {}
