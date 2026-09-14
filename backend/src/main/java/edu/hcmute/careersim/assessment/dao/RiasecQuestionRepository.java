package edu.hcmute.careersim.assessment.dao;

import edu.hcmute.careersim.assessment.entity.RiasecQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiasecQuestionRepository extends JpaRepository<RiasecQuestion, Long> {
    List<RiasecQuestion> findAllByActiveTrueOrderByDisplayOrderAsc();
}
