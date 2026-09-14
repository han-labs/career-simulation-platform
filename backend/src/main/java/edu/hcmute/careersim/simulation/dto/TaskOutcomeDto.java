// Represents one evaluated task outcome returned after submission.
package edu.hcmute.careersim.simulation.dto;

public record TaskOutcomeDto(
        Long taskId,
        String title,
        String selectedOption,
        Boolean isCorrect,
        String explanation) {
}
