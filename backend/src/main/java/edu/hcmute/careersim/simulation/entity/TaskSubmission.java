// Maps one submitted task answer with its attempt and task IDs.
package edu.hcmute.careersim.simulation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "task_submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_payload", nullable = false, columnDefinition = "jsonb")
    private String answerPayload;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    TaskSubmission(Long attemptId, Long taskId, String answerPayload) {
        this.attemptId = attemptId;
        this.taskId = taskId;
        this.answerPayload = answerPayload;
        this.submittedAt = Instant.now();
    }

    // Creates a persisted answer submission with its submission timestamp.
    public static TaskSubmission create(Long attemptId, Long taskId, String answerPayload) {
        return new TaskSubmission(attemptId, taskId, answerPayload);
    }
}
