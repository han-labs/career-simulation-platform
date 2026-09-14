package edu.hcmute.careersim.guidance.dto;

import java.util.List;

public record PlanResponse(String title, List<String> steps, String status, boolean saved) {}
