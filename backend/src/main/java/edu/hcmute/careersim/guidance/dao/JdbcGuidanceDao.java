package edu.hcmute.careersim.guidance.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcGuidanceDao implements GuidanceDao {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<CurrentPlan> findCurrentPlan(long studentId) {
        List<CurrentPlan> plans =
                jdbcTemplate.query(
                        """
                        SELECT title, CAST(steps AS VARCHAR) AS steps, status
                        FROM exploration_plans
                        WHERE student_id = ? AND status = 'ACTIVE'
                        """,
                        (resultSet, rowNumber) ->
                                new CurrentPlan(
                                        resultSet.getString("title"),
                                        readSteps(resultSet.getString("steps")),
                                        resultSet.getString("status")),
                        studentId);
        return plans.stream().findFirst();
    }

    @Override
    public CurrentPlan saveCurrentPlan(long studentId, String title, List<String> steps) {
        jdbcTemplate.update(
                """
                INSERT INTO exploration_plans (student_id, title, steps, status)
                VALUES (?, ?, CAST(? AS JSONB), 'ACTIVE')
                ON CONFLICT (student_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    steps = EXCLUDED.steps,
                    status = 'ACTIVE',
                    updated_at = CURRENT_TIMESTAMP
                """,
                studentId,
                title,
                writeJson(steps));
        return findCurrentPlan(studentId)
                .orElseThrow(
                        () -> new IllegalStateException("Saved exploration plan was not found."));
    }

    @Override
    public void recordGuidance(
            long studentId,
            Long assessmentAttemptId,
            Long simulationAttemptId,
            String actionType,
            String source,
            String status,
            String modelName,
            Object content,
            int generationMs) {
        jdbcTemplate.update(
                """
                INSERT INTO guidance_reports
                    (student_id, assessment_attempt_id, simulation_attempt_id, source,
                     status, model_name, content, action_type, generation_ms)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?)
                """,
                studentId,
                assessmentAttemptId,
                simulationAttemptId,
                source,
                status,
                modelName,
                writeJson(content),
                actionType,
                generationMs);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Guidance content could not be serialized.", exception);
        }
    }

    private List<String> readSteps(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored exploration plan is invalid.", exception);
        }
    }
}
