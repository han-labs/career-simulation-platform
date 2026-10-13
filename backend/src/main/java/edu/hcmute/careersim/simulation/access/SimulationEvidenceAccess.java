package edu.hcmute.careersim.simulation.access;

import java.time.Instant;
import java.util.List;

public interface SimulationEvidenceAccess {

    List<SimulationEvidence> findRecentEvaluated(long studentId, int limit);

    default java.util.Optional<SimulationEvidence> findOwnedEvaluated(
            long studentId, long attemptId) {
        return java.util.Optional.empty();
    }

    record SimulationEvidence(
            long attemptId,
            String title,
            int score,
            List<TaskOutcome> outcomes,
            Instant evaluatedAt,
            String careerTrack) {

        public SimulationEvidence(
                long attemptId,
                String title,
                int score,
                List<TaskOutcome> outcomes,
                Instant evaluatedAt) {
            this(attemptId, title, score, outcomes, evaluatedAt, null);
        }
    }

    record TaskOutcome(String label, String status) {}
}
