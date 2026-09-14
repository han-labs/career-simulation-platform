package edu.hcmute.careersim.assessment.controller;

import edu.hcmute.careersim.assessment.dto.AssessmentAttemptDto;
import edu.hcmute.careersim.assessment.dto.AssessmentResultDto;
import edu.hcmute.careersim.assessment.dto.RiasecQuestionDto;
import edu.hcmute.careersim.assessment.dto.SubmitAnswersRequest;
import edu.hcmute.careersim.assessment.service.AssessmentService;
import edu.hcmute.careersim.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping("/questions")
    public ApiResponse<List<RiasecQuestionDto>> getQuestions() {
        return ApiResponse.success(assessmentService.getActiveQuestions());
    }

    @PostMapping("/attempts")
    public ApiResponse<AssessmentAttemptDto> startAttempt(
            @RequestHeader(value = "X-Student-Id", defaultValue = "1") Long studentId) {
        return ApiResponse.success(assessmentService.startAttempt(studentId));
    }

    @PostMapping("/attempts/{id}/submit")
    public ApiResponse<AssessmentResultDto> submitAnswers(
            @PathVariable Long id,
            @RequestHeader(value = "X-Student-Id", defaultValue = "1") Long studentId,
            @Valid @RequestBody SubmitAnswersRequest request) {
        return ApiResponse.success(assessmentService.submitAnswers(id, studentId, request));
    }

    @GetMapping("/attempts/{id}/result")
    public ApiResponse<AssessmentResultDto> getResult(
            @PathVariable Long id,
            @RequestHeader(value = "X-Student-Id", defaultValue = "1") Long studentId) {
        return ApiResponse.success(assessmentService.getAttemptResult(id, studentId));
    }
}
