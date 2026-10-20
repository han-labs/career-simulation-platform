package edu.hcmute.careersim.guidance.agent.service.impl;

import static edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.dao.AgentRegistryDao;
import edu.hcmute.careersim.guidance.agent.domain.AgentConfigurationPolicy;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.guidance.knowledge.service.KnowledgeStudioService;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class AgentRegistryServiceImpl implements AgentRegistryService {
    private final AgentRegistryDao dao;
    private final ActorAccess actors;
    private final AgentConfigurationPolicy policy;
    private final AgentModelProvider provider;
    private final KnowledgeStudioService knowledge;
    private final AuditWriter audit;

    private void admin(long id) {
        ActorAccess.requireRole(actors.requireId(id), "ADMIN");
    }

    private void lock(long id) {
        ActorAccess.requireRole(actors.lock(id), "ADMIN");
    }

    private Agent editable(UUID id) {
        var a = dao.get(id, true);
        if ("ARCHIVED".equals(a.status()))
            throw new ApiException(ErrorCode.CONFLICT, "Agent is archived.");
        return a;
    }

    @Transactional(readOnly = true)
    public List<Agent> list(long admin) {
        admin(admin);
        return dao.list();
    }

    @Transactional
    public Agent create(long admin, AgentInput input) {
        lock(admin);
        CoreValues.text(input.name(), 180);
        CoreValues.text(input.purpose(), 2000);
        var a = dao.create(admin, input);
        audit.recordKey(admin, "AGENT_CREATED", "agent", a.id(), Map.of("status", "DRAFT"));
        return a;
    }

    @Transactional(readOnly = true)
    public Agent get(long admin, UUID id) {
        admin(admin);
        return dao.get(id, false);
    }

    @Transactional
    public Agent archive(long admin, UUID id, long revision) {
        var a = get(admin, id);
        return update(admin, id, revision, new AgentInput(a.name(), a.purpose(), "ARCHIVED"));
    }

    @Transactional
    public Agent update(long admin, UUID id, long revision, AgentInput input) {
        lock(admin);
        var a = dao.get(id, true);
        CoreValues.match(revision, a.revision());
        CoreValues.text(input.name(), 180);
        CoreValues.text(input.purpose(), 2000);
        if (input.status() == null
                || !Set.of("DRAFT", "ACTIVE", "ARCHIVED").contains(input.status())
                || "ACTIVE".equals(input.status()) && a.publishedConfigurationId() == null)
            CoreValues.invalid("Invalid Agent lifecycle transition.");
        dao.update(id, input.name(), input.purpose(), input.status());
        audit.recordKey(
                admin, "AGENT_UPDATED", "agent", id.toString(), Map.of("status", input.status()));
        return dao.get(id, false);
    }

    @Transactional(readOnly = true)
    public List<Configuration> configurations(long admin, UUID agent) {
        admin(admin);
        dao.get(agent, false);
        return dao.configurations(agent);
    }

    @Transactional(readOnly = true)
    public Configuration configuration(long admin, UUID agent, UUID config) {
        admin(admin);
        dao.get(agent, false);
        return dao.configuration(agent, config, false);
    }

    @Transactional
    public Configuration save(long admin, UUID agent, long revision, ConfigurationInput input) {
        lock(admin);
        var a = editable(agent);
        CoreValues.match(revision, a.revision());
        policy.validate(input, provider.modelKeys());
        knowledge.lockAvailable(admin, null, new HashSet<>(input.knowledgeBaseIds()), List.of());
        var c =
                dao.save(
                        admin,
                        agent,
                        input,
                        policy.reference(),
                        policy.promptHash(input.promptTemplate()),
                        policy.hash(input));
        audit.recordKey(
                admin,
                "AGENT_CONFIG_SAVED",
                "agent_configuration",
                c.id(),
                Map.of("hash", c.contentHash()));
        return c;
    }

    @Transactional
    public Agent publish(long admin, UUID agent, UUID config, long revision, boolean confirmed) {
        lock(admin);
        var a = editable(agent);
        CoreValues.match(revision, a.revision());
        if (!confirmed) CoreValues.invalid("Explicit Agent publication confirmation is required.");
        var c = dao.configuration(agent, config, true);
        if ("RETIRED".equals(c.status()))
            throw new ApiException(
                    ErrorCode.CONFLICT, "Retired configuration cannot be republished.");
        knowledge.lockAvailable(
                admin,
                config,
                c.knowledgeBaseIds().stream()
                        .map(UUID::fromString)
                        .collect(java.util.stream.Collectors.toSet()),
                List.of());
        if (!dao.previewed(config, c.contentHash()))
            throw new ApiException(
                    ErrorCode.CONFLICT,
                    "Successful preview of this exact configuration is required.");
        if (!"PUBLISHED".equals(c.status())) dao.publish(agent, config);
        audit.recordKey(
                admin,
                "AGENT_PUBLISHED",
                "agent",
                agent.toString(),
                Map.of("hash", c.contentHash()));
        return dao.get(agent, false);
    }

    @Transactional(readOnly = true)
    public List<Assignment> assignments(long admin, UUID agent) {
        admin(admin);
        dao.get(agent, false);
        return dao.assignments(agent);
    }

    @Transactional
    public Assignment assign(long admin, UUID agent, long student) {
        lock(admin);
        var a = editable(agent);
        if (!"ACTIVE".equals(a.status()))
            throw new ApiException(ErrorCode.CONFLICT, "Publish an Agent before assignment.");
        ActorAccess.requireRole(actors.lock(student), "STUDENT");
        var x = dao.assign(admin, agent, student);
        audit.recordKey(
                admin, "AGENT_ASSIGNED", "agent_assignment", x.id(), Map.of("status", "ACTIVE"));
        return x;
    }

    @Transactional
    public Assignment revoke(long admin, UUID agent, UUID assignment) {
        lock(admin);
        dao.get(agent, true);
        dao.assignment(agent, assignment);
        dao.revoke(agent, assignment);
        audit.recordKey(
                admin,
                "AGENT_ASSIGNMENT_REVOKED",
                "agent_assignment",
                assignment.toString(),
                Map.of("status", "REVOKED"));
        return dao.assignment(agent, assignment);
    }

    @Transactional(readOnly = true)
    public List<PublicAgent> assigned(long student) {
        ActorAccess.requireRole(actors.requireId(student), "STUDENT");
        return dao.assigned(student);
    }

    @Transactional(readOnly = true)
    public void requireAssignedConfiguration(long student, UUID agent, UUID config) {
        ActorAccess.requireRole(actors.requireId(student), "STUDENT");
        if (!dao.assignedConfiguration(student, agent, config))
            throw new NotFoundException("Assigned Agent unavailable.");
    }
}
