package edu.hcmute.careersim.catalog.mapper;

import edu.hcmute.careersim.catalog.dto.SimulationSummaryResponse;
import edu.hcmute.careersim.catalog.entity.CareerSimulation;
import org.springframework.stereotype.Component;

@Component
public class CareerSimulationMapper {

    public SimulationSummaryResponse toSummary(CareerSimulation simulation) {
        return new SimulationSummaryResponse(
                simulation.getId(),
                simulation.getSlug(),
                simulation.getTitle(),
                simulation.getCareerTrack(),
                simulation.getSummary(),
                simulation.getDifficulty().name(),
                simulation.getEstimatedMinutes());
    }
}
