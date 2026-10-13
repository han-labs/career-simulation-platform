package edu.hcmute.careersim.guidance.dto;

import java.util.UUID;

public record SynTurnResponse(UUID id, String state, SynMessageResponse response) {}
