package edu.hcmute.careersim.assessment.access;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcAssessmentEvidenceAccess implements AssessmentEvidenceAccess {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<AssessmentEvidence> findLatestCompleted(long studentId) {
        List<Long> attempts =
                jdbcTemplate.query(
                        """
                        SELECT id
                        FROM assessment_attempts
                        WHERE student_id = ? AND status = 'COMPLETED'
                        ORDER BY completed_at DESC, id DESC
                        LIMIT 1
                        """,
                        (resultSet, rowNumber) -> resultSet.getLong("id"),
                        studentId);

        if (attempts.isEmpty()) {
            return Optional.empty();
        }

        long attemptId = attempts.get(0);
        List<DimensionScore> scores =
                jdbcTemplate.query(
                        """
                        SELECT dimension, score
                        FROM assessment_scores
                        WHERE attempt_id = ?
                        ORDER BY score DESC, dimension ASC
                        """,
                        (resultSet, rowNumber) ->
                                new DimensionScore(
                                        resultSet.getString("dimension"),
                                        resultSet.getInt("score")),
                        attemptId);
        return Optional.of(new AssessmentEvidence(attemptId, List.copyOf(scores)));
    }
}
