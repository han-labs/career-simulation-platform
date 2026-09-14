package edu.hcmute.careersim.assessment.service;

import edu.hcmute.careersim.assessment.dto.AssessmentAttemptDto;
import edu.hcmute.careersim.assessment.dto.AssessmentResultDto;
import edu.hcmute.careersim.assessment.dto.RiasecQuestionDto;
import edu.hcmute.careersim.assessment.dto.SubmitAnswersRequest;
import java.util.List;

public interface AssessmentService {
    List<RiasecQuestionDto> getActiveQuestions();

    AssessmentAttemptDto startAttempt(Long studentId);

    AssessmentResultDto submitAnswers(Long attemptId, Long studentId, SubmitAnswersRequest request);

    AssessmentResultDto getAttemptResult(Long attemptId, Long studentId);
}
