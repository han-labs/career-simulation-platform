package edu.hcmute.careersim.guidance.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class GuidancePersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("careersim_test")
                    .withUsername("careersim")
                    .withPassword("careersim_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private GuidanceDao guidanceDao;
    @Autowired private JdbcTemplate jdbcTemplate;

    private long studentId;
    private long assessmentAttemptId;

    @BeforeEach
    void insertSyntheticEvidence() {
        studentId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO app_users
                            (email, password_hash, display_name, role, status)
                        VALUES ('guidance-test@example.test', 'test-only',
                                'Guidance Test Student', 'STUDENT', 'ACTIVE')
                        RETURNING id
                        """,
                        Long.class);
        assessmentAttemptId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO assessment_attempts
                            (student_id, status, completed_at)
                        VALUES (?, 'COMPLETED', CURRENT_TIMESTAMP)
                        RETURNING id
                        """,
                        Long.class,
                        studentId);
    }

    @AfterEach
    void removeSyntheticEvidence() {
        jdbcTemplate.update("DELETE FROM guidance_reports WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM exploration_plans WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM assessment_attempts WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM app_users WHERE id = ?", studentId);
    }

    @Test
    void flywaySchemaSupportsPlanUpsertAndGuidanceAudit() {
        guidanceDao.saveCurrentPlan(
                studentId, "First plan", List.of("Complete one focused simulation"));
        GuidanceDao.CurrentPlan updated =
                guidanceDao.saveCurrentPlan(
                        studentId,
                        "Updated plan",
                        List.of("Review the evidence", "Write a reflection"));
        guidanceDao.recordGuidance(
                studentId,
                assessmentAttemptId,
                null,
                "EXPLAIN_RIASEC",
                "FALLBACK",
                "READY",
                null,
                Map.of("text", "Synthetic Standard guidance", "provenance", "STANDARD"),
                3);

        assertThat(updated.title()).isEqualTo("Updated plan");
        assertThat(updated.steps()).containsExactly("Review the evidence", "Write a reflection");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM exploration_plans WHERE student_id = ?",
                                Integer.class,
                                studentId))
                .isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                                SELECT source || ':' || action_type
                                FROM guidance_reports
                                WHERE student_id = ?
                                """,
                                String.class,
                                studentId))
                .isEqualTo("FALLBACK:EXPLAIN_RIASEC");
    }
}
