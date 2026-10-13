package edu.hcmute.careersim.guidance.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcSynAgentDao implements SynAgentDao {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public SessionMemory loadOrCreateSession(long studentId, UUID requestedSessionId) {
        if (requestedSessionId != null) {
            var existing = findSession(studentId, requestedSessionId);
            if (existing.isPresent()) return existing.get();
        }

        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO syn_sessions (id, student_id) VALUES (?, ?)", id, studentId);
        return new SessionMemory(id, "", null, List.of());
    }

    @Override
    public java.util.Optional<SessionMemory> findSession(long studentId, UUID requestedSessionId) {
        List<SessionMemory> existing =
                jdbcTemplate.query(
                        """
                            SELECT id, summary, last_intent, CAST(last_paths AS VARCHAR) AS last_paths
                            FROM syn_sessions
                            WHERE id = ? AND student_id = ? AND expires_at > CURRENT_TIMESTAMP
                            """,
                        (resultSet, rowNumber) ->
                                new SessionMemory(
                                        resultSet.getObject("id", UUID.class),
                                        resultSet.getString("summary"),
                                        resultSet.getString("last_intent"),
                                        readList(resultSet.getString("last_paths"))),
                        requestedSessionId,
                        studentId);
        return existing.stream().findFirst();
    }

    @Override
    public void updateSession(
            long studentId, UUID sessionId, String summary, String intent, List<String> paths) {
        jdbcTemplate.update(
                """
                UPDATE syn_sessions
                SET version = version + 1, summary = ?, last_intent = ?, last_paths = CAST(? AS JSONB),
                    updated_at = CURRENT_TIMESTAMP,
                    expires_at = CURRENT_TIMESTAMP + INTERVAL '30 days'
                WHERE id = ? AND student_id = ?
                """,
                summary,
                intent,
                writeJson(paths),
                sessionId,
                studentId);
    }

    @Override
    public boolean clearSession(long studentId, UUID sessionId) {
        return jdbcTemplate.update(
                        "DELETE FROM syn_sessions WHERE id = ? AND student_id = ?",
                        sessionId,
                        studentId)
                > 0;
    }

    @Override
    public void storePendingPlan(
            long studentId,
            UUID sessionId,
            edu.hcmute.careersim.guidance.dto.SynMessageResponse.PlanDraft plan) {
        if (plan != null)
            jdbcTemplate.update(
                    "UPDATE syn_sessions SET pending_plan=CAST(? AS JSONB) WHERE id=? AND student_id=?",
                    writeJson(plan),
                    sessionId,
                    studentId);
    }

    @Override
    public edu.hcmute.careersim.guidance.dto.SynMessageResponse.PlanDraft pendingPlan(
            long studentId, UUID sessionId) {
        var values =
                jdbcTemplate.query(
                        "SELECT CAST(pending_plan AS VARCHAR) FROM syn_sessions WHERE id=? AND student_id=? AND expires_at>CURRENT_TIMESTAMP",
                        (rs, i) -> rs.getString(1),
                        sessionId,
                        studentId);
        if (values.isEmpty() || values.get(0) == null) return null;
        try {
            return objectMapper.readValue(
                    values.get(0),
                    edu.hcmute.careersim.guidance.dto.SynMessageResponse.PlanDraft.class);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Invalid stored plan", e);
        }
    }

    @Override
    public List<PathProfile> findPaths(List<String> pathCodes) {
        return jdbcTemplate
                .query(
                        """
                        SELECT code, title, summary, riasec_codes,
                               CAST(typical_activities AS VARCHAR) AS activities,
                               CAST(skill_codes AS VARCHAR) AS skill_codes
                        FROM career_path_profiles
                        WHERE status = 'ACTIVE'
                        ORDER BY code
                        """,
                        (resultSet, rowNumber) ->
                                new PathProfile(
                                        resultSet.getString("code"),
                                        resultSet.getString("title"),
                                        resultSet.getString("summary"),
                                        resultSet.getString("riasec_codes"),
                                        readList(resultSet.getString("activities")),
                                        readList(resultSet.getString("skill_codes"))))
                .stream()
                .filter(path -> pathCodes.isEmpty() || pathCodes.contains(path.code()))
                .limit(pathCodes.isEmpty() ? 2 : 3)
                .toList();
    }

    @Override
    public List<LearningResource> findResources(
            List<String> pathCodes, List<String> skillCodes, int limit) {
        return jdbcTemplate
                .query(
                        """
                        SELECT title, provider, url, skill_code, difficulty, estimated_minutes,
                               career_track
                        FROM learning_resources
                        WHERE status = 'REVIEWED'
                        ORDER BY career_track, id
                        """,
                        (resultSet, rowNumber) ->
                                new ResourceRow(
                                        new LearningResource(
                                                resultSet.getString("title"),
                                                resultSet.getString("provider"),
                                                resultSet.getString("url"),
                                                resultSet.getString("skill_code"),
                                                resultSet.getString("difficulty"),
                                                resultSet.getInt("estimated_minutes")),
                                        resultSet.getString("career_track")))
                .stream()
                .filter(
                        row ->
                                (pathCodes.isEmpty() || pathCodes.contains(row.careerTrack()))
                                        && (skillCodes.isEmpty()
                                                || skillCodes.contains(row.resource().skill())))
                .limit(Math.max(1, Math.min(limit, 4)))
                .map(ResourceRow::resource)
                .toList();
    }

    @Override
    public List<SimulationOption> findSimulations(List<String> pathCodes, int limit) {
        return jdbcTemplate
                .query(
                        """
                        SELECT title, career_track, summary, difficulty, estimated_minutes
                        FROM career_simulations
                        WHERE status = 'PUBLISHED'
                        ORDER BY title
                        """,
                        (resultSet, rowNumber) ->
                                new SimulationOption(
                                        resultSet.getString("title"),
                                        resultSet.getString("career_track"),
                                        resultSet.getString("summary"),
                                        resultSet.getString("difficulty"),
                                        resultSet.getInt("estimated_minutes")))
                .stream()
                .filter(option -> pathCodes.isEmpty() || pathCodes.contains(option.careerTrack()))
                .limit(Math.max(1, Math.min(limit, 3)))
                .toList();
    }

    @Override
    public List<SkillEvidence> findSkillEvidence(List<Long> attemptIds) {
        if (attemptIds == null || attemptIds.isEmpty()) return List.of();
        List<Long> boundedIds = attemptIds.stream().distinct().limit(10).toList();
        String placeholders =
                String.join(",", java.util.Collections.nCopies(boundedIds.size(), "?"));
        String sql =
                """
                SELECT er.attempt_id, simulation.career_track,
                       skill.skill_code, skill.skill_name,
                       CASE
                           WHEN outcome ->> 'status' IN
                                ('OBSERVED_STRENGTH', 'PRACTISED', 'NEEDS_MORE_EVIDENCE')
                               THEN outcome ->> 'status'
                           WHEN LOWER(COALESCE(outcome ->> 'isCorrect', '')) = 'true'
                               THEN 'OBSERVED_STRENGTH'
                           WHEN jsonb_exists(outcome, 'isCorrect')
                               THEN 'NEEDS_MORE_EVIDENCE'
                           ELSE 'PRACTISED'
                       END AS evidence_status
                FROM evaluation_results er
                JOIN simulation_attempts attempt ON attempt.id = er.attempt_id
                JOIN career_simulations simulation ON simulation.id = attempt.simulation_id
                CROSS JOIN LATERAL jsonb_array_elements(
                    CASE
                        WHEN jsonb_typeof(er.task_outcomes) = 'array' THEN er.task_outcomes
                        WHEN jsonb_typeof(er.task_outcomes -> 'outcomes') = 'array'
                            THEN er.task_outcomes -> 'outcomes'
                        ELSE '[]'::jsonb
                    END
                ) outcome
                JOIN simulation_task_skills skill
                  ON skill.task_id = CASE
                      WHEN COALESCE(outcome ->> 'taskId', '') ~ '^[0-9]+$'
                          THEN (outcome ->> 'taskId')::BIGINT
                      ELSE NULL
                  END
                WHERE er.attempt_id IN (%s)
                ORDER BY er.attempt_id, skill.skill_code
                """
                        .formatted(placeholders);
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) ->
                        new SkillEvidence(
                                resultSet.getLong("attempt_id"),
                                resultSet.getString("career_track"),
                                resultSet.getString("skill_code"),
                                resultSet.getString("skill_name"),
                                resultSet.getString("evidence_status")),
                boundedIds.toArray());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Syn session context could not be serialized.", exception);
        }
    }

    private List<String> readList(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored Syn session context is invalid.", exception);
        }
    }

    private record ResourceRow(LearningResource resource, String careerTrack) {}
}
