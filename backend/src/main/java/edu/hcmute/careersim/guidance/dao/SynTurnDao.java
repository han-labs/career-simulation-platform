package edu.hcmute.careersim.guidance.dao;

import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import java.util.Optional;
import java.util.UUID;

public interface SynTurnDao {
    void lockStudent(long studentId);

    Optional<Turn> findRequest(long studentId, UUID requestId);

    Optional<Turn> find(long studentId, UUID id);

    long sessionVersion(long studentId, UUID sessionId);

    boolean pending(UUID sessionId);

    int recentCount(long studentId);

    void insert(Turn turn);

    boolean sessionAlive(long studentId, UUID sessionId);

    void complete(
            long studentId,
            UUID id,
            SynMessageResponse response,
            String reason,
            String model,
            int generationMs,
            java.util.Map<String, Object> trace);

    void fail(long studentId, UUID id);

    void cancelSession(long studentId, UUID sessionId);

    void cancel(long studentId, UUID id);

    void cleanup();

    void recoverInterrupted(long owner);

    default java.util.Set<String> externalProviders(long owner, UUID turn) {
        return java.util.Set.of();
    }

    record Turn(
            UUID id,
            long studentId,
            UUID requestId,
            String requestHash,
            UUID sessionId,
            long sessionVersion,
            String state,
            SynMessageResponse response,
            boolean responseExpired) {}
}
