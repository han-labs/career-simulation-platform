// Maps the persisted evaluation result whose primary key is the attempt ID.
package edu.hcmute.careersim.simulation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "evaluation_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationResult {

    @Id
    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "task_outcomes", nullable = false, columnDefinition = "jsonb")
    private String taskOutcomes;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt;

    EvaluationResult(
            Long attemptId,
            BigDecimal score,
            BigDecimal maxScore,
            String taskOutcomes) {
        this.attemptId = attemptId;
        this.score = score;
        this.maxScore = maxScore;
        this.taskOutcomes = taskOutcomes;
        this.evaluatedAt = Instant.now();
    }

    // Creates an evaluation result while preserving constructor timestamp rules.
    public static EvaluationResult create(
            Long attemptId,
            BigDecimal score,
            BigDecimal maxScore,
            String taskOutcomes) {
        return new EvaluationResult(attemptId, score, maxScore, taskOutcomes);
    }
}
