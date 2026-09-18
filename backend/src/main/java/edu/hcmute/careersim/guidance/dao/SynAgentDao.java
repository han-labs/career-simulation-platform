package edu.hcmute.careersim.guidance.dao;

import java.util.List;
import java.util.UUID;

public interface SynAgentDao {

    SessionMemory loadOrCreateSession(long studentId, UUID requestedSessionId);

    void updateSession(
            long studentId, UUID sessionId, String summary, String intent, List<String> paths);

    boolean clearSession(long studentId, UUID sessionId);

    List<PathProfile> findPaths(List<String> pathCodes);

    List<LearningResource> findResources(List<String> pathCodes, int limit);

    List<SimulationOption> findSimulations(List<String> pathCodes, int limit);

    record SessionMemory(UUID id, String summary, String lastIntent, List<String> lastPaths) {}

    record PathProfile(String code, String title, String summary, List<String> activities) {}

    record LearningResource(
            String title,
            String provider,
            String url,
            String skill,
            String difficulty,
            int estimatedMinutes) {}

    record SimulationOption(String title, String careerTrack) {}
}
