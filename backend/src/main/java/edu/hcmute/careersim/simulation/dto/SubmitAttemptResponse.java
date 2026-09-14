// Represents the persisted simulation score and evaluated task outcomes.
package edu.hcmute.careersim.simulation.dto;

import java.util.List;

public record SubmitAttemptResponse(
        Long attemptId,
        Integer correctCount,
        Integer totalTasks,
        Integer percentage,
        List<TaskOutcomeDto> outcomes) {}
