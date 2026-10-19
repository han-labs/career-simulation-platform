package edu.hcmute.careersim.assessment.controller;

import edu.hcmute.careersim.assessment.dto.AssessmentAttemptDto;
import edu.hcmute.careersim.assessment.dto.AssessmentResultDto;
import edu.hcmute.careersim.assessment.dto.CareerQuizQuestionDto;
import edu.hcmute.careersim.assessment.dto.CareerQuizResultDto;
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
    private final edu.hcmute.careersim.identity.access.ActorAccess actors;

    @org.springframework.beans.factory.annotation.Value("${app.legacy-write-enabled:false}")
    private boolean legacyWrites;

    @GetMapping("/questions")
    public ApiResponse<List<RiasecQuestionDto>> getQuestions(java.security.Principal principal) {
        actors.student(principal);
        return ApiResponse.success(assessmentService.getActiveQuestions());
    }

    @PostMapping("/attempts")
    public ApiResponse<AssessmentAttemptDto> startAttempt(
            java.security.Principal principal,
            @RequestHeader(value = "X-Student-Id", required = false) Long studentId) {
        long owner = owner(principal, studentId);
        if (!legacyWrites)
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.WORKFLOW_UPGRADE_REQUIRED);
        return ApiResponse.success(assessmentService.startAttempt(owner));
    }

    @PostMapping("/attempts/{id}/submit")
    public ApiResponse<AssessmentResultDto> submitAnswers(
            @PathVariable Long id,
            java.security.Principal principal,
            @RequestHeader(value = "X-Student-Id", required = false) Long studentId,
            @Valid @RequestBody SubmitAnswersRequest request) {
        long owner = owner(principal, studentId);
        if (!legacyWrites)
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.WORKFLOW_UPGRADE_REQUIRED);
        return ApiResponse.success(assessmentService.submitAnswers(id, owner, request));
    }

    @GetMapping("/attempts/{id}/result")
    public ApiResponse<AssessmentResultDto> getResult(
            @PathVariable Long id,
            java.security.Principal principal,
            @RequestHeader(value = "X-Student-Id", required = false) Long studentId) {
        return ApiResponse.success(
                assessmentService.getAttemptResult(id, owner(principal, studentId)));
    }

    private long owner(java.security.Principal principal, Long header) {
        long id = actors.student(principal).id();
        if (header != null && header != id)
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.FORBIDDEN);
        return id;
    }

    @GetMapping("/quizzes/{type}/questions")
    public ApiResponse<List<CareerQuizQuestionDto>> getQuizQuestions(
            @PathVariable String type, java.security.Principal principal) {
        actors.student(principal);
        return ApiResponse.success(assessmentService.getQuizQuestions(type));
    }

    @PostMapping("/quizzes/{type}/attempts")
    public ApiResponse<AssessmentAttemptDto> startQuizAttempt(
            @PathVariable String type,
            java.security.Principal principal,
            @RequestHeader(value = "X-Student-Id", required = false) Long studentId) {
        return ApiResponse.success(
                assessmentService.startQuizAttempt(type, owner(principal, studentId)));
    }

    @PostMapping("/quizzes/attempts/{id}/submit")
    public ApiResponse<CareerQuizResultDto> submitQuizAnswers(
            @PathVariable Long id,
            java.security.Principal principal,
            @RequestHeader(value = "X-Student-Id", required = false) Long studentId,
            @Valid @RequestBody SubmitAnswersRequest request) {
        return ApiResponse.success(
                assessmentService.submitQuizAnswers(id, owner(principal, studentId), request));
    }

    @GetMapping("/quizzes/attempts/{id}/result")
    public ApiResponse<CareerQuizResultDto> getQuizResult(
            @PathVariable Long id,
            java.security.Principal principal,
            @RequestHeader(value = "X-Student-Id", required = false) Long studentId) {
        return ApiResponse.success(
                assessmentService.getQuizAttemptResult(id, owner(principal, studentId)));
    }
}
