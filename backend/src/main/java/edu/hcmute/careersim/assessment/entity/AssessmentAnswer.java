package edu.hcmute.careersim.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assessment_answers")
@Getter
@Setter
public class AssessmentAnswer {

    @EmbeddedId private AssessmentAnswerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attemptId")
    @JoinColumn(name = "attempt_id")
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    private RiasecQuestion question;

    @Column(name = "score", nullable = false)
    private Short score;
}
