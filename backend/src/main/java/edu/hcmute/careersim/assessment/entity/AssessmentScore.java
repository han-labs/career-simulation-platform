package edu.hcmute.careersim.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assessment_scores")
@Getter
@Setter
public class AssessmentScore {

    @EmbeddedId private AssessmentScoreId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attemptId")
    @JoinColumn(name = "attempt_id")
    private AssessmentAttempt attempt;

    @Column(name = "score", nullable = false)
    private Integer score;
}
