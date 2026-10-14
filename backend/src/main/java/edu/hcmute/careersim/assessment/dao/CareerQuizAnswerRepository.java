package edu.hcmute.careersim.assessment.dao;

import edu.hcmute.careersim.assessment.entity.CareerQuizAnswer;
import edu.hcmute.careersim.assessment.entity.CareerQuizAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerQuizAnswerRepository
        extends JpaRepository<CareerQuizAnswer, CareerQuizAnswerId> {}
