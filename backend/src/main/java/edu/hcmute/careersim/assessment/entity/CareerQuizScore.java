package edu.hcmute.careersim.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "career_quiz_scores")
@Getter
@Setter
public class CareerQuizScore {
    @EmbeddedId private CareerQuizScoreId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attemptId")
    @JoinColumn(name = "attempt_id")
    private AssessmentAttempt attempt;

    @Column(name = "score", nullable = false)
    private Integer score;
}
