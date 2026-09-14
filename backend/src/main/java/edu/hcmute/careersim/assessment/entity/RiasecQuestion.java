package edu.hcmute.careersim.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "riasec_questions")
@Getter
@Setter
public class RiasecQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "dimension", columnDefinition = "CHAR(1)", nullable = false)
    private String dimension;

    @Column(name = "prompt", length = 500, nullable = false)
    private String prompt;

    @Column(name = "display_order", nullable = false, unique = true)
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
