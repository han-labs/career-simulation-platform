package edu.hcmute.careersim.assessment.domain;

import edu.hcmute.careersim.assessment.dto.AnswerItemDto;
import edu.hcmute.careersim.assessment.entity.CareerQuizQuestion;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Validates active quiz configuration and submitted answers against backend-owned questions. */
@Component
public class CareerQuizSubmissionPolicy {
    public void requireValidQuestions(String quizType, List<CareerQuizQuestion> questions) {
        Map<String, List<String>> expectedCategories =
                Map.of(
                        "INTERESTS",
                        List.of("ANALYTICAL", "CREATIVE", "COLLABORATIVE", "ORGANIZING"),
                        "ABILITIES",
                        List.of(
                                "PROBLEM_SOLVING",
                                "COMMUNICATION",
                                "ORGANIZATION",
                                "TECHNICAL_LEARNING"),
                        "WORK_ACTIVITIES",
                        List.of("INFORMATION", "PEOPLE", "THINGS"));
        List<String> expected = expectedCategories.get(quizType);
        if (expected == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Unsupported assessment type.");
        }
        Map<String, Long> categoryCounts =
                questions.stream()
                        .collect(
                                Collectors.groupingBy(
                                        CareerQuizQuestion::getCategory, Collectors.counting()));
        if (questions.size() != expected.size() * 3
                || categoryCounts.size() != expected.size()
                || expected.stream()
                        .anyMatch(category -> categoryCounts.getOrDefault(category, 0L) != 3L)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "This assessment is not available.");
        }
    }

    public Map<Long, Short> validateAnswers(
            List<CareerQuizQuestion> questions, List<AnswerItemDto> submitted) {
        Map<Long, CareerQuizQuestion> byId =
                questions.stream()
                        .collect(Collectors.toMap(CareerQuizQuestion::getId, question -> question));
        Map<Long, Short> answers = new LinkedHashMap<>();
        for (AnswerItemDto item : submitted) {
            if (item.questionId() == null
                    || item.score() == null
                    || item.score() < 1
                    || item.score() > 5) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR, "Each response must be between 1 and 5.");
            }
            if (!byId.containsKey(item.questionId())) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR, "Invalid question ID: " + item.questionId());
            }
            if (answers.putIfAbsent(item.questionId(), item.score()) != null) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        "Duplicate question response: " + item.questionId());
            }
        }
        if (answers.size() != questions.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "You must answer all active questions.");
        }
        return answers;
    }
}
