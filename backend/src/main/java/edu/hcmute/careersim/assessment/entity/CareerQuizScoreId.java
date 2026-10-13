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
public class CareerQuizScoreId implements Serializable {
    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "category", length = 40)
    private String category;

    public CareerQuizScoreId(Long attemptId, String category) {
        this.attemptId = attemptId;
        this.category = category;
    }
}
