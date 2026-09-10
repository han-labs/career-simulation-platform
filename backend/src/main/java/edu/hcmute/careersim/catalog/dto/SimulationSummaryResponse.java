package edu.hcmute.careersim.catalog.dto;

public record SimulationSummaryResponse(
        Long id,
        String slug,
        String title,
        String careerTrack,
        String summary,
        String difficulty,
        Integer estimatedMinutes) {}
