package edu.hcmute.careersim.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.hcmute.careersim.assessment.dto.AnswerItemDto;
import edu.hcmute.careersim.assessment.dto.SubmitAnswersRequest;
import edu.hcmute.careersim.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
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
class CareerQuizPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:0.8.2-pg17")
                    .withDatabaseName("careersim_assessment_test")
                    .withUsername("careersim")
                    .withPassword("careersim_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private AssessmentService assessmentService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private long studentId;

    @BeforeEach
    void createSyntheticStudent() {
        studentId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO app_users
                            (email, password_hash, display_name, role, status)
                        VALUES (?, 'test-only', 'Assessment Test Student', 'STUDENT', 'ACTIVE')
                        RETURNING id
                        """,
                        Long.class,
                        "assessment-test-" + UUID.randomUUID() + "@example.test");
    }

    @AfterEach
    void removeSyntheticStudent() {
        jdbcTemplate.update("DELETE FROM assessment_attempts WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM app_users WHERE id = ?", studentId);
    }

    @Test
    void workActivitiesSubmissionPersistsAnswersScoresAndReloadableResult() {
        var questions = assessmentService.getQuizQuestions("WORK_ACTIVITIES");
        assertThat(questions).hasSize(9);
        var attempt = assessmentService.startQuizAttempt("WORK_ACTIVITIES", studentId);

        var answers =
                IntStream.range(0, questions.size())
                        .mapToObj(
                                index ->
                                        new AnswerItemDto(
                                                questions.get(index).id(),
                                                (short) (index < 3 ? 4 : index < 6 ? 5 : 2)))
                        .toList();
        var submitted =
                assessmentService.submitQuizAnswers(
                        attempt.id(), studentId, new SubmitAnswersRequest(answers));
        var loaded = assessmentService.getQuizAttemptResult(attempt.id(), studentId);

        assertThat(submitted).isEqualTo(loaded);
        assertThat(submitted.quizType()).isEqualTo("WORK_ACTIVITIES");
        assertThat(submitted.scores())
                .extracting(score -> score.category() + ":" + score.score())
                .containsExactly("INFORMATION:12", "PEOPLE:15", "THINGS:6");
        assertThat(submitted.topCategories()).containsExactly("PEOPLE");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM career_quiz_answers WHERE attempt_id = ?",
                                Integer.class,
                                attempt.id()))
                .isEqualTo(9);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM career_quiz_scores WHERE attempt_id = ?",
                                Integer.class,
                                attempt.id()))
                .isEqualTo(3);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT assessment_type || ':' || status FROM assessment_attempts WHERE id = ?",
                                String.class,
                                attempt.id()))
                .isEqualTo("WORK_ACTIVITIES:COMPLETED");
        assertThatThrownBy(
                        () ->
                                assessmentService.getQuizAttemptResult(
                                        attempt.id(), studentId + 1_000_000))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void incompleteSubmissionDoesNotPersistAnswersOrCompleteTheAttempt() {
        var questions = assessmentService.getQuizQuestions("WORK_ACTIVITIES");
        var attempt = assessmentService.startQuizAttempt("WORK_ACTIVITIES", studentId);
        List<AnswerItemDto> incomplete =
                questions.subList(0, 8).stream()
                        .map(question -> new AnswerItemDto(question.id(), (short) 3))
                        .toList();

        assertThatThrownBy(
                        () ->
                                assessmentService.submitQuizAnswers(
                                        attempt.id(),
                                        studentId,
                                        new SubmitAnswersRequest(incomplete)))
                .hasMessage("You must answer all active questions.");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM career_quiz_answers WHERE attempt_id = ?",
                                Integer.class,
                                attempt.id()))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT status FROM assessment_attempts WHERE id = ?",
                                String.class,
                                attempt.id()))
                .isEqualTo("IN_PROGRESS");
    }
}
