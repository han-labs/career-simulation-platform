// Provides persistence operations for simulation tasks.
package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.simulation.entity.SimulationTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationTaskDao extends JpaRepository<SimulationTask, Long> {

    List<SimulationTask> findBySimulationIdOrderByDisplayOrderAsc(Long simulationId);
}
