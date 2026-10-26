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
    private final edu.hcmute.careersim.guidance.agent.dao.AgentRoleBindingDao roles;
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
        roles.lock();
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
        roles.lock();
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
        if (input.experienceConfig() != null && !input.experienceConfig().isNull())
            dao.update(
                    agent,
                    input.experienceConfig().path("name").asText(),
                    input.experienceConfig().path("description").asText(),
                    a.status());
        var c =
                dao.save(
                        admin,
                        agent,
                        input,
                        policy.reference(),
                        policy.promptHash(input),
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

    @Transactional(readOnly = true)
    public EligibleStudents eligibleStudents(
            long admin, UUID agent, String query, long afterId, int limit) {
        admin(admin);
        dao.get(agent, false);
        if (query == null || query.length() > 80 || afterId < 0 || limit < 1 || limit > 50)
            CoreValues.invalid(
                    "Student search requires at most 80 characters, a nonnegative cursor and a limit from 1 to 50.");
        var rows = actors.eligibleStudents(query.trim(), afterId, limit + 1);
        var items = rows.stream().limit(limit).toList();
        return new EligibleStudents(
                items, rows.size() > limit ? items.get(items.size() - 1).id() : null);
    }

    @Transactional(readOnly = true)
    public PublicationReadiness publicationReadiness(long admin, UUID agent, UUID config) {
        admin(admin);
        var a = dao.get(agent, false);
        var c = dao.configuration(agent, config, false);
        boolean available = true;
        try {
            knowledge.requireAvailable(
                    admin,
                    config,
                    c.knowledgeBaseIds().stream()
                            .map(UUID::fromString)
                            .collect(java.util.stream.Collectors.toSet()));
        } catch (ApiException unavailable) {
            available = false;
        }
        boolean editable = !"ARCHIVED".equals(a.status());
        boolean previewed = dao.previewed(config, c.contentHash());
        return new PublicationReadiness(
                c.id(),
                c.contentHash(),
                a.revision(),
                c.status(),
                editable,
                available,
                previewed,
                editable && available && previewed && !"RETIRED".equals(c.status()));
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
        return dao.available("STUDENT");
    }

    @Transactional(readOnly = true)
    public void requireAssignedConfiguration(long student, UUID agent, UUID config) {
        ActorAccess.requireRole(actors.requireId(student), "STUDENT");
        if (!dao.assignedConfiguration(student, agent, config))
            throw new NotFoundException("Assigned Agent unavailable.");
    }

    @Transactional(readOnly = true)
    public List<PublicAgent> available(long id) {
        var actor = actors.requireId(id);
        if (!Set.of("STUDENT", "ENTERPRISE").contains(actor.role()))
            throw new ApiException(ErrorCode.FORBIDDEN);
        return dao.available(actor.role());
    }

    @Transactional(readOnly = true)
    public List<RoleBinding> roleBindings(long id) {
        admin(id);
        return roles.list();
    }

    @Transactional
    public List<RoleBinding> activateRoles(
            long id, UUID agent, long revision, RoleActivation input) {
        lock(id);
        roles.lock();
        var a = editable(agent);
        CoreValues.match(revision, a.revision());
        if (input == null
                || !input.confirmed()
                || input.roles() == null
                || input.roles().size() > 2
                || input.roles().stream().anyMatch(Objects::isNull)
                || new HashSet<>(input.roles()).size() != input.roles().size()
                || !Set.of("STUDENT", "ENTERPRISE").containsAll(input.roles())
                || input.expectedRoleRevisions() == null
                || !input.expectedRoleRevisions().keySet().equals(Set.of("STUDENT", "ENTERPRISE")))
            CoreValues.invalid("Confirm the supported role selection with current revisions.");
        var bindings = roles.list();
        for (var b : bindings)
            if (!Objects.equals(input.expectedRoleRevisions().get(b.role()), b.revision()))
                throw new ApiException(
                        ErrorCode.CONFLICT,
                        "Role availability changed. Refresh before activation.");
        if (!input.roles().isEmpty()) {
            if (!"ACTIVE".equals(a.status()) || a.publishedConfigurationId() == null)
                throw new ApiException(
                        ErrorCode.CONFLICT, "Publish this Agent before role activation.");
            var ready =
                    publicationReadiness(id, agent, UUID.fromString(a.publishedConfigurationId()));
            if (!ready.publishable())
                throw new ApiException(
                        ErrorCode.CONFLICT, "Published configuration is not ready for activation.");
        }
        for (var b : bindings) {
            if (input.roles().contains(b.role())) roles.bind(b.role(), agent);
            else if (agent.toString().equals(b.agentId())) roles.bind(b.role(), null);
        }
        audit.recordKey(
                id,
                "AGENT_ROLES_ACTIVATED",
                "agent",
                agent.toString(),
                Map.of("status", String.join(",", input.roles()), "count", input.roles().size()));
        return roles.list();
    }
}
