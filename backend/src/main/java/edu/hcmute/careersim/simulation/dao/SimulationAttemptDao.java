// Provides persistence operations for simulation attempts.
package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.simulation.entity.SimulationAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationAttemptDao extends JpaRepository<SimulationAttempt, Long> {

    List<SimulationAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);
}
