package edu.hcmute.careersim.assessment.dao;

import edu.hcmute.careersim.assessment.entity.AssessmentScore;
import edu.hcmute.careersim.assessment.entity.AssessmentScoreId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentScoreRepository
        extends JpaRepository<AssessmentScore, AssessmentScoreId> {
    List<AssessmentScore> findByAttemptId(Long attemptId);
}
