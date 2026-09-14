package edu.hcmute.careersim.assessment.dao;

import edu.hcmute.careersim.assessment.entity.AssessmentAttempt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
    Optional<AssessmentAttempt> findByIdAndStudentId(Long id, Long studentId);
}
