// Maps a student simulation attempt using IDs instead of cross-module relations.
package edu.hcmute.careersim.simulation.entity;

import edu.hcmute.careersim.simulation.enumeration.AttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "simulation_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimulationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "simulation_id", nullable = false)
    private Long simulationId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    SimulationAttempt(Long simulationId, Long studentId, AttemptStatus status) {
        this.simulationId = simulationId;
        this.studentId = studentId;
        this.status = status;
        this.startedAt = Instant.now();
    }

    // Creates an attempt that has completed server-side evaluation.
    public static SimulationAttempt createEvaluated(Long simulationId, Long studentId) {
        SimulationAttempt attempt =
                new SimulationAttempt(simulationId, studentId, AttemptStatus.EVALUATED);
        Instant now = Instant.now();
        attempt.submittedAt = now;
        attempt.evaluatedAt = now;
        return attempt;
    }
}
