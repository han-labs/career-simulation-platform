// Provides persistence operations for evaluation results keyed by attempt ID.
package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.simulation.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationResultDao extends JpaRepository<EvaluationResult, Long> {}
