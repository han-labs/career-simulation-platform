// Defines read operations for published simulation task content.
package edu.hcmute.careersim.simulation.service;

import edu.hcmute.careersim.simulation.dto.SimulationDetailResponse;

public interface SimulationQueryService {

    SimulationDetailResponse getSimulationWithTasks(String slug);
}
