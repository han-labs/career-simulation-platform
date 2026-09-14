package edu.hcmute.careersim.simulation.access;

import java.util.List;

public interface SimulationEvidenceAccess {

    List<SimulationEvidence> findRecentEvaluated(long studentId, int limit);

    record SimulationEvidence(
            long attemptId, String title, int score, List<TaskOutcome> outcomes) {}

    record TaskOutcome(String label, String status) {}
}
