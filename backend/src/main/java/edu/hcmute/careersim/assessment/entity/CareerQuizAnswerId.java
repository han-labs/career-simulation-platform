package edu.hcmute.careersim.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CareerQuizAnswerId implements Serializable {
    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "question_id")
    private Long questionId;

    public CareerQuizAnswerId(Long attemptId, Long questionId) {
        this.attemptId = attemptId;
        this.questionId = questionId;
    }
}
