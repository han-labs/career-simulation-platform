package edu.hcmute.careersim.catalog.entity;

import edu.hcmute.careersim.catalog.enumeration.DifficultyLevel;
import edu.hcmute.careersim.catalog.enumeration.SimulationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "career_simulations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareerSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "career_track", nullable = false, length = 60)
    private String careerTrack;

    @Column(nullable = false, length = 600)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DifficultyLevel difficulty;

    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SimulationStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    CareerSimulation(
            String slug,
            String title,
            String careerTrack,
            String summary,
            DifficultyLevel difficulty,
            Integer estimatedMinutes,
            SimulationStatus status) {
        this.slug = slug;
        this.title = title;
        this.careerTrack = careerTrack;
        this.summary = summary;
        this.difficulty = difficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.status = status;
    }
}
