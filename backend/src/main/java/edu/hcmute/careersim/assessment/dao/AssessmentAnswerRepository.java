package edu.hcmute.careersim.assessment.dao;

import edu.hcmute.careersim.assessment.entity.AssessmentAnswer;
import edu.hcmute.careersim.assessment.entity.AssessmentAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentAnswerRepository
        extends JpaRepository<AssessmentAnswer, AssessmentAnswerId> {}
