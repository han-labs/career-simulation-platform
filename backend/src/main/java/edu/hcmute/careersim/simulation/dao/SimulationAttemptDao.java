// Provides persistence operations for simulation attempts.
package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.simulation.entity.SimulationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationAttemptDao extends JpaRepository<SimulationAttempt, Long> {
}
