package edu.hcmute.careersim.guidance.dao;

import java.util.List;
import java.util.Optional;

public interface GuidanceDao {

    Optional<CurrentPlan> findCurrentPlan(long studentId);

    CurrentPlan saveCurrentPlan(long studentId, String title, List<String> steps);

    void recordGuidance(
            long studentId,
            Long assessmentAttemptId,
            Long simulationAttemptId,
            String actionType,
            String source,
            String status,
            String modelName,
            Object content,
            int generationMs);

    record CurrentPlan(String title, List<String> steps, String status) {}
}
