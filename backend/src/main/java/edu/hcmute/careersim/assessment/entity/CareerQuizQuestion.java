package edu.hcmute.careersim.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "career_quiz_questions")
@Getter
@Setter
public class CareerQuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_type", length = 30, nullable = false)
    private String quizType;

    @Column(name = "category", length = 40, nullable = false)
    private String category;

    @Column(name = "prompt", length = 500, nullable = false)
    private String prompt;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
