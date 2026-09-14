package edu.hcmute.careersim.assessment.mapper;

import edu.hcmute.careersim.assessment.dto.AssessmentAttemptDto;
import edu.hcmute.careersim.assessment.dto.RiasecQuestionDto;
import edu.hcmute.careersim.assessment.entity.AssessmentAttempt;
import edu.hcmute.careersim.assessment.entity.RiasecQuestion;
import org.springframework.stereotype.Component;

@Component
public class AssessmentMapper {

    public RiasecQuestionDto toDto(RiasecQuestion entity) {
        return new RiasecQuestionDto(
                entity.getId(),
                entity.getDimension(),
                entity.getPrompt(),
                entity.getDisplayOrder());
    }

    public AssessmentAttemptDto toDto(AssessmentAttempt entity) {
        return new AssessmentAttemptDto(
                entity.getId(), entity.getStatus(), entity.getStartedAt(), entity.getCompletedAt());
    }
}
