package edu.hcmute.careersim.guidance.agent.service.impl;

import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.dao.*;
import edu.hcmute.careersim.guidance.agent.domain.AgentExperience;
import edu.hcmute.careersim.guidance.agent.dto.AdminPreviewContracts.*;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class AdminPreviewConversationServiceImpl implements AdminPreviewConversationService {
    private final AdminPreviewConversationDao chats;
    private final AgentRegistryDao registry;
    private final AgentExecutionDao runs;
    private final ExecutionService executions;
    private final AgentModelProvider models;
    private final ActorAccess actors;

    private void admin(long id, boolean lock) {
        ActorAccess.requireRole(lock ? actors.lock(id) : actors.requireId(id), "ADMIN");
    }

    @Transactional(readOnly = true)
    public Page list(long owner, UUID agent, int limit, int offset) {
        admin(owner, false);
        registry.get(agent, false);
        if (limit < 1 || limit > 50 || offset < 0 || offset > 5000)
            CoreValues.invalid("Invalid conversation page.");
        var rows = chats.list(owner, agent, limit + 1, offset);
        return new Page(
                rows.stream().limit(limit).toList(), rows.size() > limit ? offset + limit : null);
    }

    @Transactional
    public Conversation create(long owner, UUID agent, NewChat input) {
        admin(owner, true);
        if (input == null || input.requestId() == null || input.configurationId() == null)
            CoreValues.invalid("Request and saved configuration are required.");
        var prior = chats.request(owner, input.requestId());
        if (prior.isPresent()) {
            var c = prior.get();
            if (!c.agentId().equals(agent.toString())
                    || !c.configurationId().equals(input.configurationId().toString()))
                throw new ApiException(ErrorCode.CONFLICT, "Chat request changed.");
            return chats.get(owner, agent, UUID.fromString(c.id()), false);
        }
        var a = registry.get(agent, true);
        var c = registry.configuration(agent, input.configurationId(), false);
        if ("ARCHIVED".equals(a.status()) || "RETIRED".equals(c.status()))
            throw new NotFoundException("Preview configuration unavailable.");
        return chats.create(owner, agent, input.configurationId(), input.requestId());
    }

    // Reading an interrupted run may finalize its persisted expiry through ExecutionService.
    @Transactional
    public Transcript read(long owner, UUID agent, UUID id, long before, int limit) {
        admin(owner, false);
        if (before < 1 || limit < 1 || limit > 100) CoreValues.invalid("Invalid transcript page.");
        var chat = chats.get(owner, agent, id, false);
        var a = registry.get(agent, false);
        var c = registry.configuration(agent, UUID.fromString(chat.configurationId()), false);
        var rows = chats.turns(id, chat.epoch(), before, limit + 1);
        var turns = new ArrayList<Turn>();
        for (var row : rows.stream().limit(limit).toList()) {
            try {
                turns.add(new Turn(row.sequence(), executions.read(owner, row.execution()), false));
            } catch (NotFoundException revoked) {
                turns.add(new Turn(row.sequence(), null, true));
            }
        }
        Collections.reverse(turns);
        var experience = c.experienceConfig();
        boolean usable = !"ARCHIVED".equals(a.status()) && !"RETIRED".equals(c.status());
        boolean available =
                models.models().stream()
                        .anyMatch(m -> m.key().equals(c.modelKey()) && m.available());
        String reason =
                !usable
                        ? "Preview configuration unavailable."
                        : !available ? "The selected model is unavailable." : null;
        return new Transcript(
                chat,
                c.versionNumber(),
                experience == null ? a.name() : experience.path("name").asText(),
                AgentExperience.greeting(experience),
                experience == null
                        ? CoreValues.tree(
                                Map.of(
                                        "includeHistory",
                                        false,
                                        "maxTurns",
                                        1,
                                        "showSourceExcerpts",
                                        true,
                                        "responseStyle",
                                        "balanced"))
                        : experience.path("chat"),
                usable && available,
                reason,
                turns,
                rows.size() > limit ? rows.get(limit - 1).sequence() : null);
    }

    @Transactional
    public Execution send(long owner, UUID agent, UUID id, Message input) {
        admin(owner, true);
        var chat = chats.get(owner, agent, id, true);
        if (input == null || input.requestId() == null)
            CoreValues.invalid("A request ID is required.");
        var prior = runs.request(owner, "ADMIN_PREVIEW", input.requestId());
        if (prior.isPresent()) {
            var e = prior.get();
            if (!chats.contains(id, chat.epoch(), UUID.fromString(e.id()))
                    || !e.inputSnapshot()
                            .path("requestFingerprint")
                            .asText()
                            .equals(CoreValues.hash(input.query())))
                throw new ApiException(
                        ErrorCode.CONFLICT,
                        "Message request changed or belongs to an old chat epoch.");
            return executions.read(owner, UUID.fromString(e.id()));
        }
        if (chats.running(id, chat.epoch()))
            throw new ApiException(
                    ErrorCode.CONFLICT, "A Preview run is already active in this chat.");
        var c = registry.configuration(agent, UUID.fromString(chat.configurationId()), false);
        var previous = new ArrayList<Object>();
        var exp = c.experienceConfig();
        if (exp != null && exp.path("chat").path("includeHistory").asBoolean()) {
            int max = exp.path("chat").path("maxTurns").asInt();
            int bytes = 0;
            for (var row : chats.turns(id, chat.epoch(), Long.MAX_VALUE, max)) {
                Execution e;
                try {
                    e = executions.read(owner, row.execution());
                } catch (ApiException revoked) {
                    continue;
                }
                if (!"SUCCEEDED".equals(e.status())
                        || e.outputSnapshot() == null
                        || !"ANSWER".equals(e.outputSnapshot().path("outcome").asText())) continue;
                var pair =
                        Map.of(
                                "user",
                                e.inputSnapshot().path("query").asText(),
                                "assistant",
                                e.outputSnapshot().path("text").asText());
                int size =
                        CoreValues.write(pair)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                                .length;
                if (bytes + size > 12000) break;
                bytes += size;
                previous.add(pair);
            }
            Collections.reverse(previous);
        }
        var execution =
                executions.previewConversation(
                        owner,
                        agent,
                        UUID.fromString(chat.configurationId()),
                        new Preview(input.requestId(), input.query()),
                        new Scope(id, chat.epoch(), CoreValues.write(previous)));
        chats.link(id, chat.epoch(), UUID.fromString(execution.id()));
        return execution;
    }

    @Transactional
    public Conversation change(
            long owner, UUID agent, UUID id, long revision, boolean confirmed, boolean delete) {
        admin(owner, true);
        var chat = chats.get(owner, agent, id, true);
        CoreValues.match(revision, chat.revision());
        if (!confirmed) CoreValues.invalid("Explicit chat lifecycle confirmation is required.");
        chats.change(id, chat.epoch(), delete);
        return delete
                ? new Conversation(
                        chat.id(),
                        chat.agentId(),
                        chat.configurationId(),
                        "DELETED",
                        chat.epoch() + 1,
                        chat.revision() + 1,
                        chat.createdAt(),
                        java.time.Instant.now())
                : chats.get(owner, agent, id, false);
    }
}
