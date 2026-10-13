package edu.hcmute.careersim.guidance.dao;

import java.util.List;
import java.util.UUID;

public interface SynAgentDao {

    SessionMemory loadOrCreateSession(long studentId, UUID requestedSessionId);

    java.util.Optional<SessionMemory> findSession(long studentId, UUID sessionId);

    void updateSession(
            long studentId, UUID sessionId, String summary, String intent, List<String> paths);

    boolean clearSession(long studentId, UUID sessionId);

    default void storePendingPlan(
            long studentId,
            UUID sessionId,
            edu.hcmute.careersim.guidance.dto.SynMessageResponse.PlanDraft plan) {}

    default edu.hcmute.careersim.guidance.dto.SynMessageResponse.PlanDraft pendingPlan(
            long studentId, UUID sessionId) {
        return null;
    }

    List<PathProfile> findPaths(List<String> pathCodes);

    List<LearningResource> findResources(
            List<String> pathCodes, List<String> skillCodes, int limit);

    default List<LearningResource> findResources(List<String> pathCodes, int limit) {
        return findResources(pathCodes, List.of(), limit);
    }

    List<SimulationOption> findSimulations(List<String> pathCodes, int limit);

    List<SkillEvidence> findSkillEvidence(List<Long> attemptIds);

    record SessionMemory(UUID id, String summary, String lastIntent, List<String> lastPaths) {}

    record PathProfile(
            String code,
            String title,
            String summary,
            String riasecCodes,
            List<String> activities,
            List<String> skillCodes) {}

    record LearningResource(
            String title,
            String provider,
            String url,
            String skill,
            String difficulty,
            int estimatedMinutes) {}

    record SimulationOption(
            String title,
            String careerTrack,
            String summary,
            String difficulty,
            int estimatedMinutes) {}

    record SkillEvidence(
            long attemptId,
            String careerTrack,
            String skillCode,
            String skillName,
            String status) {}
}
