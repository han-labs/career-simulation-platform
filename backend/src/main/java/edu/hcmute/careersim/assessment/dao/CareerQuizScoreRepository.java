package edu.hcmute.careersim.assessment.dao;

import edu.hcmute.careersim.assessment.entity.CareerQuizScore;
import edu.hcmute.careersim.assessment.entity.CareerQuizScoreId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerQuizScoreRepository
        extends JpaRepository<CareerQuizScore, CareerQuizScoreId> {
    List<CareerQuizScore> findByIdAttemptId(Long attemptId);
}
