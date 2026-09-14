// Maps a simulation task while keeping its parent simulation as a Long ID.
package edu.hcmute.careersim.simulation.entity;

import edu.hcmute.careersim.simulation.enumeration.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "simulation_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimulationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "simulation_id", nullable = false)
    private Long simulationId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private TaskType taskType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_rule", nullable = false, columnDefinition = "jsonb")
    private String evaluationRule;

    SimulationTask(
            Long simulationId,
            String title,
            String instructions,
            TaskType taskType,
            Integer displayOrder,
            BigDecimal maxScore,
            String evaluationRule) {
        this.simulationId = simulationId;
        this.title = title;
        this.instructions = instructions;
        this.taskType = taskType;
        this.displayOrder = displayOrder;
        this.maxScore = maxScore;
        this.evaluationRule = evaluationRule;
    }
}
