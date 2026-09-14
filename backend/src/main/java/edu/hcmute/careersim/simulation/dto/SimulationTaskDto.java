// Exposes safe task data without answer keys or post-submit explanations.
package edu.hcmute.careersim.simulation.dto;

import java.util.List;

public record SimulationTaskDto(
        Long id,
        Integer displayOrder,
        String title,
        String instructions,
        String taskType,
        List<OptionDto> options) {
}
