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
            if (!existing.isEmpty()) return existing.get(0);
        }

        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO syn_sessions (id, student_id) VALUES (?, ?)", id, studentId);
        return new SessionMemory(id, "", null, List.of());
    }

    @Override
    public void updateSession(
            long studentId, UUID sessionId, String summary, String intent, List<String> paths) {
        jdbcTemplate.update(
                """
                UPDATE syn_sessions
                SET summary = ?, last_intent = ?, last_paths = CAST(? AS JSONB),
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
    public List<PathProfile> findPaths(List<String> pathCodes) {
        return jdbcTemplate
                .query(
                        """
                        SELECT code, title, summary, CAST(typical_activities AS VARCHAR) AS activities
                        FROM career_path_profiles
                        WHERE status = 'ACTIVE'
                        ORDER BY code
                        """,
                        (resultSet, rowNumber) ->
                                new PathProfile(
                                        resultSet.getString("code"),
                                        resultSet.getString("title"),
                                        resultSet.getString("summary"),
                                        readList(resultSet.getString("activities"))))
                .stream()
                .filter(path -> pathCodes.isEmpty() || pathCodes.contains(path.code()))
                .limit(pathCodes.isEmpty() ? 2 : 3)
                .toList();
    }

    @Override
    public List<LearningResource> findResources(List<String> pathCodes, int limit) {
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
                .filter(row -> pathCodes.isEmpty() || pathCodes.contains(row.careerTrack()))
                .limit(Math.max(1, Math.min(limit, 4)))
                .map(ResourceRow::resource)
                .toList();
    }

    @Override
    public List<SimulationOption> findSimulations(List<String> pathCodes, int limit) {
        return jdbcTemplate
                .query(
                        """
                        SELECT title, career_track
                        FROM career_simulations
                        WHERE status = 'PUBLISHED'
                        ORDER BY title
                        """,
                        (resultSet, rowNumber) ->
                                new SimulationOption(
                                        resultSet.getString("title"),
                                        resultSet.getString("career_track")))
                .stream()
                .filter(option -> pathCodes.isEmpty() || pathCodes.contains(option.careerTrack()))
                .limit(Math.max(1, Math.min(limit, 3)))
                .toList();
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
