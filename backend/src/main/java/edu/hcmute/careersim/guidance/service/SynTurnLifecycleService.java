package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.guidance.dao.SynAgentDao;
import edu.hcmute.careersim.guidance.dao.SynTurnDao;
import edu.hcmute.careersim.guidance.dto.SynMessageRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import edu.hcmute.careersim.identity.access.StudentAccountAccess;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SynTurnLifecycleService {
    private final SynTurnDao turns;
    private final SynAgentDao sessions;
    private final StudentAccountAccess accounts;
    private final edu.hcmute.careersim.guidance.dao.KnowledgeDao knowledge;
    private final SynUsagePricing usagePricing;
    private final edu.hcmute.careersim.identity.access.ConsentAccess consents;

    @org.springframework.beans.factory.annotation.Autowired
    private edu.hcmute.careersim.simulation.access.ParticipationEvidenceAccess participation;

    @Transactional(
            readOnly = true,
            isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ,
            timeout = 3)
    public <T> T snapshot(java.util.function.Supplier<T> read) {
        return read.get();
    }

    @Transactional(readOnly = true, timeout = 3)
    public Long rememberedResult(long owner, UUID session, String message) {
        if (session == null
                || !edu.hcmute.careersim.guidance.domain.SynMemoryEvidence.refersToResult(message))
            return null;
        return sessions.findSession(owner, session)
                .map(
                        s ->
                                edu.hcmute.careersim.guidance.domain.SynMemoryEvidence
                                        .selectedResult(s.summary()))
                .orElse(null);
    }

    @Transactional(timeout = 3)
    public SynTurnDao.Turn begin(long owner, SynMessageRequest request) {
        turns.lockStudent(owner);
        turns.recoverInterrupted(owner);
        UUID requestId =
                request.clientMessageId() == null ? UUID.randomUUID() : request.clientMessageId();
        String hash =
                hash(
                        request.message()
                                + "\n"
                                + request.resolvedActionType()
                                + "\n"
                                + request.resolvedResponseLanguage()
                                + "\n"
                                + request.sessionId()
                                + "\n"
                                + request.context());
        var existing = turns.findRequest(owner, requestId);
        if (existing.isPresent()) {
            var turn = existing.get();
            if (!hash.equals(turn.requestHash()))
                conflict("Request ID was already used with different content.");
            if (!"COMPLETED".equals(turn.state())
                    || turn.responseExpired()
                    || turn.response() == null)
                conflict(
                        "The previous turn is pending, interrupted, cleared or expired. Check its status before retrying.");
            validateSources(turn.response(), owner);
            validateReplay(turn);
            return turn;
        }
        if (turns.recentCount(owner) >= 10)
            throw new ApiException(
                    ErrorCode.TOO_MANY_REQUESTS, "Please wait before sending another message.");
        var session = sessions.loadOrCreateSession(owner, request.sessionId());
        long version = turns.sessionVersion(owner, session.id());
        if (turns.pending(session.id()))
            conflict("A reply is already being prepared for this conversation.");
        var turn =
                new SynTurnDao.Turn(
                        UUID.randomUUID(),
                        owner,
                        requestId,
                        hash,
                        session.id(),
                        version,
                        "PENDING",
                        null,
                        false);
        turns.insert(turn);
        return turn;
    }

    @Transactional(timeout = 3)
    public SynMessageResponse finish(
            String email, SynTurnDao.Turn turn, SynAgentOrchestrator.AgentRun run, int ms) {
        return finish(email, turn, run, ms, null);
    }

    @Transactional(timeout = 3)
    public SynMessageResponse finish(
            String email,
            SynTurnDao.Turn turn,
            SynAgentOrchestrator.AgentRun run,
            int ms,
            Long expectedPrivacy) {
        turns.lockStudent(turn.studentId());
        var account =
                accounts.findByEmail(email)
                        .orElseThrow(() -> new NotFoundException("Student was not found."));
        if ((expectedPrivacy != null && account.privacyVersion() != expectedPrivacy)
                || account.id() != turn.studentId()
                || !"STUDENT".equals(account.role())
                || !"ACTIVE".equals(account.status())
                || (("AI".equals(run.auditSource())
                                || (run.response().webSearch() != null
                                        && !run.response().webSearch().sources().isEmpty()))
                        && account.consentedToAiAt() == null))
            throw new ApiException(ErrorCode.FORBIDDEN, "This reply is no longer authorized.");
        var stored =
                turns.find(turn.studentId(), turn.id())
                        .orElseThrow(() -> new NotFoundException("Turn was not found."));
        if (!"PENDING".equals(stored.state())
                || turns.sessionVersion(turn.studentId(), turn.sessionId())
                        != turn.sessionVersion())
            conflict("The conversation changed while this reply was being prepared.");
        validateSources(run.response(), turn.studentId());
        sessions.updateSession(
                turn.studentId(),
                turn.sessionId(),
                run.memorySummary(),
                run.response().intent(),
                run.memoryPaths());
        if (run.response().planDraft() != null)
            sessions.storePendingPlan(
                    turn.studentId(), turn.sessionId(), run.response().planDraft());
        SynMessageResponse response = run.response().withTurn(turn.id().toString(), run.reason());
        turns.complete(
                turn.studentId(),
                turn.id(),
                response,
                run.reason(),
                run.modelName(),
                ms,
                usagePricing.enrich(SynExecutionScope.trace()));
        return response;
    }

    @Transactional(timeout = 3)
    public boolean clear(long owner, UUID session) {
        turns.lockStudent(owner);
        turns.cancelSession(owner, session);
        sessions.clearSession(owner, session);
        return true;
    }

    @Transactional(timeout = 3)
    public void failed(long owner, UUID turn) {
        turns.fail(owner, turn);
    }

    @Transactional(readOnly = true, timeout = 3)
    public boolean externalAllowed(String email, long owner, UUID session, UUID turnId) {
        return accounts.findByEmail(email)
                        .filter(
                                a ->
                                        a.id() == owner
                                                && "STUDENT".equals(a.role())
                                                && "ACTIVE".equals(a.status())
                                                && (consents.effective(
                                                                owner,
                                                                "EXTERNAL_AI",
                                                                "provider:gemini")
                                                        || consents.effective(
                                                                owner,
                                                                "EXTERNAL_AI",
                                                                "provider:openai")))
                        .isPresent()
                && turns.sessionAlive(owner, session)
                && turns.find(owner, turnId).filter(t -> "PENDING".equals(t.state())).isPresent();
    }

    @Transactional(readOnly = true, timeout = 3)
    public boolean providerAllowed(
            String email, long owner, long expectedPrivacy, String provider) {
        return accounts.findByEmail(email)
                        .filter(
                                a ->
                                        a.id() == owner
                                                && "ACTIVE".equals(a.status())
                                                && "STUDENT".equals(a.role())
                                                && a.privacyVersion() == expectedPrivacy)
                        .isPresent()
                && consents.effective(owner, "EXTERNAL_AI", "provider:" + provider);
    }

    @Transactional(readOnly = true)
    public SynMessageResponse.PlanDraft pendingPlan(long owner, UUID session) {
        if (session == null) return null;
        return sessions.pendingPlan(owner, session);
    }

    @Transactional(timeout = 3)
    public SynTurnDao.Turn get(long owner, UUID id) {
        var turn =
                turns.find(owner, id)
                        .orElseThrow(() -> new NotFoundException("Turn was not found."));
        if (turn.response() != null && !turn.responseExpired())
            validateSources(turn.response(), owner);
        if (turn.response() != null && !turn.responseExpired()) validateReplay(turn);
        return turn;
    }

    @Transactional
    public void cancel(long owner, UUID id) {
        turns.lockStudent(owner);
        turns.find(owner, id).orElseThrow(() -> new NotFoundException("Turn was not found."));
        turns.cancel(owner, id);
    }

    @Transactional(timeout = 3)
    public SynTurnDao.Turn getRequest(long owner, UUID requestId) {
        turns.lockStudent(owner);
        turns.recoverInterrupted(owner);
        var turn =
                turns.findRequest(owner, requestId)
                        .orElseThrow(() -> new NotFoundException("Request was not found."));
        if (turn.response() != null && !turn.responseExpired())
            validateSources(turn.response(), owner);
        if (turn.response() != null && !turn.responseExpired()) validateReplay(turn);
        return turn;
    }

    @Transactional(timeout = 3)
    public edu.hcmute.careersim.guidance.dto.SynMessageResponse.EvidenceReference source(long id) {
        var c =
                knowledge
                        .published(id)
                        .orElseThrow(
                                () -> new NotFoundException("Published source was not found."));
        return new edu.hcmute.careersim.guidance.dto.SynMessageResponse.EvidenceReference(
                c.title(),
                "KNOWLEDGE",
                c.referenceId(),
                Long.toString(c.versionId()),
                c.section(),
                c.sourceUrl(),
                c.content());
    }

    private void validateSources(SynMessageResponse response, long owner) {
        for (var reference : response.evidenceReferences()) {
            if ("TASK_PARTICIPATION".equals(reference.sourceType())) {
                var ids =
                        reference.referenceId() == null
                                ? new String[0]
                                : reference.referenceId().split(":");
                if (ids.length != 3
                        || !"SUBMISSION".equals(ids[0])
                        || !ids[2].equals(reference.version())
                        || !participation.ownsFinal(owner, ids[1], ids[2], reference.section()))
                    conflict("Participation evidence is no longer accessible.");
            }
            if ("KNOWLEDGE".equals(reference.sourceType()) && reference.referenceId() != null) {
                long chunkId = Long.parseLong(reference.referenceId().substring(1));
                if (knowledge
                        .published(chunkId)
                        .filter(c -> Long.toString(c.versionId()).equals(reference.version()))
                        .isEmpty()) conflict("A knowledge source changed. Please ask again.");
            }
        }
    }

    private void validateReplay(SynTurnDao.Turn turn) {
        var response = turn.response();
        if (response == null) return;
        boolean external =
                "AI".equals(response.provenance())
                        || (response.webSearch() != null
                                && !response.webSearch().sources().isEmpty());
        if (!external) return;
        var providers = turns.externalProviders(turn.studentId(), turn.id());
        if (providers.isEmpty()
                || providers.stream()
                        .anyMatch(
                                p ->
                                        !consents.effective(
                                                turn.studentId(), "EXTERNAL_AI", "provider:" + p)))
            throw new ApiException(
                    ErrorCode.FORBIDDEN, "Stored external reply is no longer authorized.");
    }

    private static void conflict(String message) {
        throw new ApiException(ErrorCode.CONFLICT, message);
    }

    private static String hash(String text) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
