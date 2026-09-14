package edu.hcmute.careersim.assessment.access;

import java.util.List;
import java.util.Optional;

public interface AssessmentEvidenceAccess {

    Optional<AssessmentEvidence> findLatestCompleted(long studentId);

    record AssessmentEvidence(long attemptId, List<DimensionScore> scores) {}

    record DimensionScore(String dimension, int score) {}
}
