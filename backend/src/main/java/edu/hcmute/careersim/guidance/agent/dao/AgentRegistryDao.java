package edu.hcmute.careersim.guidance.agent.dao;

import static edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;

import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AgentRegistryDao {
    private final JdbcTemplate jdbc;
    private final RowMapper<Agent> agentMap =
            (r, n) ->
                    new Agent(
                            r.getString("id"),
                            r.getString("name"),
                            r.getString("purpose"),
                            r.getString("status"),
                            r.getLong("row_version"),
                            r.getString("published_configuration_id"),
                            r.getTimestamp("created_at").toInstant());
    private final RowMapper<Configuration> configMap =
            (r, n) ->
                    new Configuration(
                            r.getString("id"),
                            r.getString("agent_id"),
                            r.getInt("version_number"),
                            r.getString("model_key"),
                            r.getString("prompt_template"),
                            r.getString("prompt_template_ref"),
                            r.getString("prompt_hash"),
                            CoreValues.json(r.getString("tool_policy")),
                            CoreValues.json(r.getString("memory_policy")),
                            CoreValues.json(r.getString("output_policy")),
                            List.of(),
                            r.getString("status"),
                            r.getString("content_hash"),
                            r.getTimestamp("created_at").toInstant(),
                            r.getTimestamp("published_at") == null
                                    ? null
                                    : r.getTimestamp("published_at").toInstant(),
                            r.getString("experience_config") == null
                                    ? null
                                    : CoreValues.json(r.getString("experience_config")));
    private final RowMapper<Assignment> assignmentMap =
            (r, n) ->
                    new Assignment(
                            r.getString("id"),
                            r.getString("agent_id"),
                            r.getString("student_user_id"),
                            r.getString("assigned_by_admin_user_id"),
                            r.getString("status"),
                            r.getTimestamp("assigned_at").toInstant(),
                            r.getTimestamp("revoked_at") == null
                                    ? null
                                    : r.getTimestamp("revoked_at").toInstant(),
                            r.getString("student_display_name"));

    public List<Agent> list() {
        return jdbc.query("SELECT * FROM public.agent ORDER BY created_at,id LIMIT 200", agentMap);
    }

    public Agent get(UUID id, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM public.agent WHERE id=?" + (lock ? " FOR UPDATE" : ""),
                        agentMap,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Agent not found."));
    }

    public Agent create(long actor, AgentInput input) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.agent(id,name,purpose,created_by_admin_id) VALUES (?,?,?,?)",
                id,
                input.name(),
                input.purpose(),
                actor);
        return get(id, false);
    }

    public void update(UUID id, String name, String purpose, String status) {
        jdbc.update(
                "UPDATE public.agent SET name=?,purpose=?,status=?,updated_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=?",
                name,
                purpose,
                status,
                id);
    }

    public List<Configuration> configurations(UUID id) {
        return jdbc
                .query(
                        "SELECT * FROM public.agent_configuration WHERE agent_id=? AND sealed ORDER BY version_number DESC LIMIT 200",
                        configMap,
                        id)
                .stream()
                .map(this::granted)
                .toList();
    }

    private Configuration granted(Configuration c) {
        var grants =
                jdbc.queryForList(
                        "SELECT knowledge_base_id::text FROM public.agent_config_kb_scope WHERE configuration_id=? ORDER BY knowledge_base_id",
                        String.class,
                        UUID.fromString(c.id()));
        return new Configuration(
                c.id(),
                c.agentId(),
                c.versionNumber(),
                c.modelKey(),
                c.promptTemplate(),
                c.promptTemplateRef(),
                c.promptHash(),
                c.toolPolicy(),
                c.memoryPolicy(),
                c.outputPolicy(),
                grants,
                c.status(),
                c.contentHash(),
                c.createdAt(),
                c.publishedAt(),
                c.experienceConfig());
    }

    public Configuration configuration(UUID agent, UUID config, boolean lock) {
        return granted(
                jdbc
                        .query(
                                "SELECT * FROM public.agent_configuration WHERE id=? AND agent_id=? AND sealed"
                                        + (lock ? " FOR UPDATE" : ""),
                                configMap,
                                config,
                                agent)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () -> new NotFoundException("Agent configuration not found.")));
    }

    public Configuration configuration(UUID config) {
        return granted(
                jdbc
                        .query(
                                "SELECT * FROM public.agent_configuration WHERE id=? AND sealed",
                                configMap,
                                config)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () -> new NotFoundException("Agent configuration not found.")));
    }

    public Configuration save(
            long admin,
            UUID agent,
            ConfigurationInput input,
            String ref,
            String promptHash,
            String contentHash) {
        UUID id = UUID.randomUUID();
        int version =
                jdbc.queryForObject(
                        "SELECT COALESCE(max(version_number),0)+1 FROM public.agent_configuration WHERE agent_id=?",
                        Integer.class,
                        agent);
        jdbc.update(
                "INSERT INTO public.agent_configuration(id,agent_id,version_number,model_key,prompt_template,prompt_template_ref,prompt_hash,tool_policy,memory_policy,output_policy,created_by_admin_id,content_hash,experience_config) VALUES (?,?,?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),?,?,CAST(? AS jsonb))",
                id,
                agent,
                version,
                input.modelKey(),
                input.promptTemplate(),
                ref,
                promptHash,
                CoreValues.write(input.toolPolicy()),
                CoreValues.write(input.memoryPolicy()),
                CoreValues.write(input.outputPolicy()),
                admin,
                contentHash,
                input.experienceConfig() == null || input.experienceConfig().isNull()
                        ? null
                        : CoreValues.write(input.experienceConfig()));
        for (var kb : input.knowledgeBaseIds())
            jdbc.update(
                    "INSERT INTO public.agent_config_kb_scope(configuration_id,knowledge_base_id) VALUES (?,?)",
                    id,
                    kb);
        jdbc.update("UPDATE public.agent_configuration SET sealed=true WHERE id=?", id);
        jdbc.update(
                "UPDATE public.agent SET row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                agent);
        return configuration(agent, id, false);
    }

    public boolean previewed(UUID configuration, String hash) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.agent_execution WHERE configuration_id=? AND configuration_hash=? AND execution_mode='ADMIN_PREVIEW' AND status='SUCCEEDED')",
                        Boolean.class,
                        configuration,
                        hash));
    }

    public void publish(UUID agent, UUID configuration) {
        jdbc.update(
                "UPDATE public.agent_configuration SET status='PUBLISHED',published_at=COALESCE(published_at,CURRENT_TIMESTAMP) WHERE id=?",
                configuration);
        jdbc.update(
                "UPDATE public.agent SET status='ACTIVE',published_configuration_id=?,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                configuration,
                agent);
        jdbc.update(
                "UPDATE public.agent_configuration SET status='RETIRED' WHERE agent_id=? AND id<>? AND status='PUBLISHED'",
                agent,
                configuration);
    }

    public List<Assignment> assignments(UUID agent) {
        return jdbc.query(
                "SELECT x.*,u.display_name AS student_display_name FROM public.agent_assignment x JOIN public.app_users u ON u.id=x.student_user_id WHERE x.agent_id=? ORDER BY x.assigned_at DESC,x.id LIMIT 500",
                assignmentMap,
                agent);
    }

    public Assignment assignment(UUID agent, UUID assignment) {
        return jdbc
                .query(
                        "SELECT x.*,u.display_name AS student_display_name FROM public.agent_assignment x JOIN public.app_users u ON u.id=x.student_user_id WHERE x.id=? AND x.agent_id=?",
                        assignmentMap,
                        assignment,
                        agent)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Assignment not found."));
    }

    public Assignment assign(long admin, UUID agent, long student) {
        var old =
                jdbc.query(
                        "SELECT x.*,u.display_name AS student_display_name FROM public.agent_assignment x JOIN public.app_users u ON u.id=x.student_user_id WHERE x.agent_id=? AND x.student_user_id=? AND x.status='ACTIVE'",
                        assignmentMap,
                        agent,
                        student);
        if (!old.isEmpty()) return old.get(0);
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.agent_assignment(id,agent_id,student_user_id,assigned_by_admin_user_id) VALUES (?,?,?,?)",
                id,
                agent,
                student,
                admin);
        return assignment(agent, id);
    }

    public void revoke(UUID agent, UUID assignment) {
        jdbc.update(
                "UPDATE public.agent_assignment SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP WHERE id=? AND agent_id=? AND status='ACTIVE'",
                assignment,
                agent);
    }

    public List<PublicAgent> available(String role) {
        return jdbc.query(
                "SELECT a.id,COALESCE(c.experience_config->>'name',a.name),COALESCE(c.experience_config->>'description',a.purpose) FROM agent_role_binding b JOIN agent a ON a.id=b.agent_id AND a.status='ACTIVE' JOIN agent_configuration c ON c.id=a.published_configuration_id AND c.status='PUBLISHED' AND c.sealed WHERE b.role=? AND NOT EXISTS(SELECT 1 FROM agent_config_kb_scope g JOIN knowledge_base kb ON kb.id=g.knowledge_base_id WHERE g.configuration_id=c.id AND kb.status<>'ACTIVE')",
                (r, n) ->
                        new PublicAgent(
                                r.getString(1),
                                r.getString(2),
                                r.getString(3),
                                "STUDENT".equals(role)),
                role);
    }

    public boolean assignedConfiguration(long student, UUID agent, UUID config) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM agent_role_binding rb JOIN agent a ON a.id=rb.agent_id AND a.status='ACTIVE' JOIN agent_configuration current_config ON current_config.id=a.published_configuration_id AND current_config.status='PUBLISHED' AND current_config.sealed JOIN agent_configuration pinned ON pinned.agent_id=a.id AND pinned.id=? AND pinned.status IN ('PUBLISHED','RETIRED') AND pinned.sealed JOIN app_users u ON u.id=? AND u.role='STUDENT' AND u.status='ACTIVE' WHERE rb.role='STUDENT' AND a.id=?)",
                        Boolean.class,
                        config,
                        student,
                        agent));
    }
}
