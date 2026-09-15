package edu.hcmute.careersim.simulation.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcSimulationEvidenceAccess implements SimulationEvidenceAccess {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("OBSERVED_STRENGTH", "PRACTISED", "NEEDS_MORE_EVIDENCE");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<SimulationEvidence> findRecentEvaluated(long studentId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        return jdbcTemplate.query(
                """
                SELECT sa.id AS attempt_id, cs.title, er.score, er.max_score,
                       CAST(er.task_outcomes AS VARCHAR) AS task_outcomes
                FROM simulation_attempts sa
                JOIN career_simulations cs ON cs.id = sa.simulation_id
                JOIN evaluation_results er ON er.attempt_id = sa.id
                WHERE sa.student_id = ? AND sa.status = 'EVALUATED'
                ORDER BY er.evaluated_at DESC, sa.id DESC
                LIMIT ?
                """,
                (resultSet, rowNumber) ->
                        new SimulationEvidence(
                                resultSet.getLong("attempt_id"),
                                resultSet.getString("title"),
                                toPercentage(
                                        resultSet.getBigDecimal("score"),
                                        resultSet.getBigDecimal("max_score")),
                                parseOutcomes(resultSet.getString("task_outcomes"))),
                studentId,
                safeLimit);
    }

    private int toPercentage(BigDecimal score, BigDecimal maxScore) {
        if (score == null || maxScore == null || maxScore.signum() <= 0) {
            return 0;
        }
        return score.multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    List<TaskOutcome> parseOutcomes(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode outcomes = root.isArray() ? root : root.path("outcomes");
            if (!outcomes.isArray()) {
                return List.of();
            }

            List<TaskOutcome> parsed = new ArrayList<>();
            for (JsonNode item : outcomes) {
                String label = firstText(item, "label", "skill", "name", "title");
                String status = resolveStatus(item);
                if (!label.isBlank() && ALLOWED_STATUSES.contains(status)) {
                    parsed.add(new TaskOutcome(label, status));
                }
            }
            return List.copyOf(parsed);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String resolveStatus(JsonNode item) {
        String explicitStatus = item.path("status").asText("").trim();
        if (!explicitStatus.isBlank()) {
            return explicitStatus;
        }
        if (item.has("isCorrect") && item.path("isCorrect").isBoolean()) {
            return item.path("isCorrect").asBoolean() ? "OBSERVED_STRENGTH" : "NEEDS_MORE_EVIDENCE";
        }
        return "PRACTISED";
    }

    private String firstText(JsonNode item, String... fields) {
        for (String field : fields) {
            String value = item.path(field).asText("").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
