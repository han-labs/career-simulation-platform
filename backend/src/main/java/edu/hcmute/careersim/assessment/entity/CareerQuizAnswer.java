package edu.hcmute.careersim.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "career_quiz_answers")
@Getter
@Setter
public class CareerQuizAnswer {
    @EmbeddedId private CareerQuizAnswerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attemptId")
    @JoinColumn(name = "attempt_id")
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    private CareerQuizQuestion question;

    @Column(name = "score", nullable = false)
    private Short score;
}
