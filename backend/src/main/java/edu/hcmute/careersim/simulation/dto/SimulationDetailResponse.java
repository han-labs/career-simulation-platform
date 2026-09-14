// Represents a published simulation and its public task definitions.
package edu.hcmute.careersim.simulation.dto;

import java.util.List;

public record SimulationDetailResponse(
        String slug,
        String title,
        String summary,
        String difficulty,
        Integer estimatedMinutes,
        List<SimulationTaskDto> tasks) {}
