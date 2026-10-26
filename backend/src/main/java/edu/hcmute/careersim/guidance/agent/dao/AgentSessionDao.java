package edu.hcmute.careersim.guidance.agent.dao;

import static edu.hcmute.careersim.guidance.agent.dto.StudentAgentContracts.*;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AgentSessionDao {
    private final JdbcTemplate jdbc;
    private static final String SELECT =
            "SELECT s.*,COALESCE(c.experience_config->>'name',a.name) AS agent_name,c.version_number FROM public.agent_session s JOIN public.agent a ON a.id=s.agent_id JOIN public.agent_configuration c ON c.id=s.configuration_id";
    private final RowMapper<Session> map =
            (r, n) ->
                    new Session(
                            r.getString("id"),
                            r.getString("agent_id"),
                            r.getString("agent_name"),
                            r.getString("configuration_id"),
                            r.getInt("version_number"),
                            r.getString("title"),
                            r.getString("status"),
                            r.getString("summary_status"),
                            r.getLong("next_sequence"),
                            r.getLong("context_epoch"),
                            r.getLong("row_version"),
                            r.getTimestamp("started_at").toInstant());

    public List<Session> list(
            long owner, UUID agent, java.time.Instant before, UUID beforeId, int limit) {
        return jdbc.query(
                SELECT
                        + " WHERE s.owner_user_id=? AND s.status='ACTIVE' AND (?::uuid IS NULL OR s.agent_id=?) AND (?::timestamptz IS NULL OR (s.started_at,s.id)<(?,?)) ORDER BY s.started_at DESC,s.id DESC LIMIT ?",
                map,
                owner,
                agent,
                agent,
                before == null ? null : java.sql.Timestamp.from(before),
                before == null ? null : java.sql.Timestamp.from(before),
                beforeId,
                limit);
    }

    public Session get(long owner, UUID id, boolean lock) {
        return jdbc
                .query(
                        SELECT
                                + " WHERE s.owner_user_id=? AND s.id=? AND s.status='ACTIVE'"
                                + (lock ? " FOR UPDATE OF s" : ""),
                        map,
                        owner,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Conversation unavailable."));
    }

    public Optional<Session> request(long owner, UUID request) {
        return jdbc
                .query(
                        SELECT
                                + " WHERE s.owner_user_id=? AND s.creation_request_id=? AND s.status='ACTIVE'",
                        map,
                        owner,
                        request)
                .stream()
                .findFirst();
    }

    public Session create(long owner, UUID agent, UUID configuration, UUID request, String title) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.agent_session(id,owner_user_id,agent_id,configuration_id,title,creation_request_id) VALUES (?,?,?,?,?,?)",
                id,
                owner,
                agent,
                configuration,
                title,
                request);
        return get(owner, id, false);
    }

    public void delete(long owner, UUID session) {
        jdbc.update(
                "UPDATE public.agent_session SET status='DELETED',deleted_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=? AND owner_user_id=? AND status='ACTIVE'",
                session,
                owner);
    }

    public boolean deleted(long owner, UUID session) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.agent_session WHERE id=? AND owner_user_id=? AND status='DELETED')",
                        Boolean.class,
                        session,
                        owner));
    }

    public List<Interaction> messages(long owner, UUID session, long after, int limit) {
        return jdbc.query(
                "SELECT i.* FROM public.interaction_record i JOIN public.agent_session s ON s.id=i.session_id WHERE i.owner_user_id=? AND i.session_id=? AND i.sequence_number>? AND i.deleted_at IS NULL AND s.status='ACTIVE' ORDER BY i.sequence_number LIMIT ?",
                (r, n) ->
                        new Interaction(
                                r.getString("id"),
                                r.getLong("sequence_number"),
                                r.getString("actor_type"),
                                CoreValues.json(r.getString("content")),
                                r.getString("execution_id"),
                                r.getTimestamp("created_at").toInstant(),
                                false),
                owner,
                session,
                after,
                limit);
    }

    public List<Interaction> recent(long owner, UUID session, int limit) {
        return jdbc.query(
                "SELECT i.* FROM public.interaction_record i JOIN public.agent_session s ON s.id=i.session_id WHERE i.owner_user_id=? AND i.session_id=? AND i.deleted_at IS NULL AND s.status='ACTIVE' ORDER BY i.sequence_number DESC LIMIT ?",
                (r, n) ->
                        new Interaction(
                                r.getString("id"),
                                r.getLong("sequence_number"),
                                r.getString("actor_type"),
                                CoreValues.json(r.getString("content")),
                                r.getString("execution_id"),
                                r.getTimestamp("created_at").toInstant(),
                                false),
                owner,
                session,
                limit);
    }

    public UUID append(long owner, UUID session, String actor, Object content, UUID execution) {
        long sequence =
                jdbc.queryForObject(
                        "UPDATE public.agent_session SET next_sequence=next_sequence+1,row_version=row_version+1 WHERE id=? AND owner_user_id=? AND status='ACTIVE' RETURNING next_sequence-1",
                        Long.class,
                        session,
                        owner);
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.interaction_record(id,session_id,owner_user_id,sequence_number,actor_type,content,execution_id) VALUES (?,?,?,?,?,CAST(? AS jsonb),?)",
                id,
                session,
                owner,
                sequence,
                actor,
                CoreValues.write(content),
                execution);
        return id;
    }

    public void bindInput(UUID interaction, UUID execution) {
        jdbc.update(
                "UPDATE public.interaction_record SET execution_id=? WHERE id=? AND execution_id IS NULL",
                execution,
                interaction);
    }

    public Optional<UUID> activeExecution(UUID session) {
        return jdbc
                .query(
                        "SELECT id FROM public.agent_execution WHERE session_id=? AND status IN ('ACCEPTED','RUNNING')",
                        (r, n) -> r.getObject(1, UUID.class),
                        session)
                .stream()
                .findFirst();
    }

    public JsonNode summary(UUID session) {
        return jdbc.queryForObject(
                "SELECT CASE WHEN summary_status='VALID' THEN context_summary ELSE NULL END FROM public.agent_session WHERE id=?",
                (r, n) -> r.getString(1) == null ? null : CoreValues.json(r.getString(1)),
                session);
    }

    public Set<UUID> summarySources(UUID session) {
        return new LinkedHashSet<>(
                jdbc.query(
                        "SELECT source_interaction_id FROM public.agent_summary_source WHERE target_session_id=? ORDER BY source_interaction_id",
                        (r, n) -> r.getObject(1, UUID.class),
                        session));
    }

    public Set<UUID> executionSources(UUID execution) {
        return new LinkedHashSet<>(
                jdbc.query(
                        "SELECT source_interaction_id FROM public.agent_execution_context_source WHERE execution_id=? ORDER BY source_interaction_id",
                        (r, n) -> r.getObject(1, UUID.class),
                        execution));
    }

    public boolean sourcesAvailable(long owner, Collection<UUID> sources) {
        for (UUID source : sources)
            if (!Boolean.TRUE.equals(
                    jdbc.queryForObject(
                            "SELECT EXISTS(SELECT 1 FROM public.interaction_record i JOIN public.agent_session s ON s.id=i.session_id WHERE i.id=? AND i.owner_user_id=? AND i.deleted_at IS NULL AND s.status='ACTIVE')",
                            Boolean.class,
                            source,
                            owner))) return false;
        return true;
    }

    public record Source(UUID session, String actor, UUID execution) {}

    public Optional<Source> source(long owner, UUID id) {
        return jdbc
                .query(
                        "SELECT session_id,actor_type,execution_id FROM public.interaction_record WHERE id=? AND owner_user_id=? AND deleted_at IS NULL",
                        (r, n) ->
                                new Source(
                                        r.getObject(1, UUID.class),
                                        r.getString(2),
                                        r.getObject(3, UUID.class)),
                        id,
                        owner)
                .stream()
                .findFirst();
    }

    public void recordExecutionSources(long owner, UUID execution, Collection<UUID> sources) {
        for (UUID id : sources)
            jdbc.update(
                    "INSERT INTO public.agent_execution_context_source(execution_id,source_interaction_id,owner_user_id) VALUES (?,?,?) ON CONFLICT DO NOTHING",
                    execution,
                    id,
                    owner);
    }

    public void summary(long owner, UUID session, Object value, Collection<UUID> sources) {
        jdbc.update("DELETE FROM public.agent_summary_source WHERE target_session_id=?", session);
        for (UUID source : sources)
            jdbc.update(
                    "INSERT INTO public.agent_summary_source(target_session_id,source_interaction_id,owner_user_id) VALUES (?,?,?)",
                    session,
                    source,
                    owner);
        jdbc.update(
                "UPDATE public.agent_session SET context_summary=CAST(? AS jsonb),summary_source_refs=CAST(? AS jsonb),summary_status=?,summary_updated_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=? AND owner_user_id=?",
                value == null ? null : CoreValues.write(value),
                CoreValues.write(sources),
                value == null ? "NONE" : "VALID",
                session,
                owner);
    }

    public void event(UUID execution, String type, Object payload) {
        jdbc.queryForObject(
                "SELECT id FROM public.agent_execution WHERE id=? FOR UPDATE",
                UUID.class,
                execution);
        long seq =
                jdbc.queryForObject(
                        "SELECT COALESCE(max(sequence_number),0)+1 FROM public.agent_execution_event WHERE execution_id=?",
                        Long.class,
                        execution);
        jdbc.update(
                "INSERT INTO public.agent_execution_event(execution_id,sequence_number,event_type,payload) VALUES (?,?,?,CAST(? AS jsonb))",
                execution,
                seq,
                type,
                CoreValues.write(payload));
    }

    public long lastEvent(UUID execution) {
        return jdbc.queryForObject(
                "SELECT COALESCE(max(sequence_number),0) FROM public.agent_execution_event WHERE execution_id=?",
                Long.class,
                execution);
    }

    public List<Event> events(UUID execution, long after) {
        return jdbc.query(
                "SELECT sequence_number,event_type,payload FROM public.agent_execution_event WHERE execution_id=? AND sequence_number>? ORDER BY sequence_number LIMIT 100",
                (r, n) -> new Event(r.getLong(1), r.getString(2), CoreValues.json(r.getString(3))),
                execution,
                after);
    }
}
