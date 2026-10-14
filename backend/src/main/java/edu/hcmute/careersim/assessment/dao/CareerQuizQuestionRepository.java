package edu.hcmute.careersim.assessment.dao;

import edu.hcmute.careersim.assessment.entity.CareerQuizQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerQuizQuestionRepository extends JpaRepository<CareerQuizQuestion, Long> {
    List<CareerQuizQuestion> findAllByQuizTypeAndActiveTrueOrderByDisplayOrderAsc(String quizType);
}
