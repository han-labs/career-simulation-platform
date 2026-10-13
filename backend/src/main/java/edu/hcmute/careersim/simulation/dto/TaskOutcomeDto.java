// Represents one evaluated task outcome returned after submission.
package edu.hcmute.careersim.simulation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record TaskOutcomeDto(
        Long taskId,
        String title,
        String selectedOption,
        Boolean isCorrect,
        String explanation,
        @JsonInclude(JsonInclude.Include.NON_NULL) String explanationVi) {
    public TaskOutcomeDto(
            Long taskId,
            String title,
            String selectedOption,
            Boolean isCorrect,
            String explanation) {
        this(taskId, title, selectedOption, isCorrect, explanation, null);
    }
}
