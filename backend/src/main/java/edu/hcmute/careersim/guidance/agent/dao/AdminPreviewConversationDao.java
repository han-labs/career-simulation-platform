package edu.hcmute.careersim.guidance.agent.dao;

import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.guidance.agent.dto.AdminPreviewContracts.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminPreviewConversationDao {
    private final JdbcTemplate jdbc;
    private final RowMapper<Conversation> map =
            (r, n) ->
                    new Conversation(
                            r.getString("id"),
                            r.getString("agent_id"),
                            r.getString("configuration_id"),
                            r.getString("status"),
                            r.getLong("context_epoch"),
                            r.getLong("row_version"),
                            r.getTimestamp("created_at").toInstant(),
                            r.getTimestamp("updated_at").toInstant());

    public Conversation get(long owner, UUID agent, UUID id, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM public.admin_preview_conversation WHERE id=? AND owner_admin_id=? AND agent_id=? AND status='ACTIVE'"
                                + (lock ? " FOR UPDATE" : ""),
                        map,
                        id,
                        owner,
                        agent)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Preview conversation unavailable."));
    }

    public Optional<Conversation> request(long owner, UUID request) {
        return jdbc
                .query(
                        "SELECT * FROM public.admin_preview_conversation WHERE owner_admin_id=? AND request_id=?",
                        map,
                        owner,
                        request)
                .stream()
                .findFirst();
    }

    public List<Conversation> list(long owner, UUID agent, int limit, int offset) {
        return jdbc.query(
                "SELECT * FROM public.admin_preview_conversation WHERE owner_admin_id=? AND agent_id=? AND status='ACTIVE' ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
                map,
                owner,
                agent,
                limit,
                offset);
    }

    public Conversation create(long owner, UUID agent, UUID config, UUID request) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.admin_preview_conversation(id,owner_admin_id,agent_id,configuration_id,request_id) VALUES (?,?,?,?,?)",
                id,
                owner,
                agent,
                config,
                request);
        return get(owner, agent, id, false);
    }

    public record StoredTurn(long sequence, UUID execution) {}

    public List<StoredTurn> turns(UUID id, long epoch, long before, int limit) {
        return jdbc.query(
                "SELECT sequence_number,execution_id FROM public.admin_preview_turn WHERE conversation_id=? AND context_epoch=? AND sequence_number<? ORDER BY sequence_number DESC LIMIT ?",
                (r, n) -> new StoredTurn(r.getLong(1), r.getObject(2, UUID.class)),
                id,
                epoch,
                before,
                limit);
    }

    public void link(UUID id, long epoch, UUID execution) {
        long sequence =
                jdbc.queryForObject(
                        "SELECT next_sequence FROM public.admin_preview_conversation WHERE id=?",
                        Long.class,
                        id);
        jdbc.update(
                "INSERT INTO public.admin_preview_turn(conversation_id,context_epoch,sequence_number,execution_id) VALUES (?,?,?,?)",
                id,
                epoch,
                sequence,
                execution);
        jdbc.update(
                "UPDATE public.admin_preview_conversation SET next_sequence=next_sequence+1,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                id);
    }

    public boolean active(UUID id, long epoch) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.admin_preview_conversation WHERE id=? AND context_epoch=? AND status='ACTIVE')",
                        Boolean.class,
                        id,
                        epoch));
    }

    public boolean running(UUID id, long epoch) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.admin_preview_turn t JOIN public.agent_execution e ON e.id=t.execution_id WHERE t.conversation_id=? AND t.context_epoch=? AND e.status IN ('ACCEPTED','RUNNING'))",
                        Boolean.class,
                        id,
                        epoch));
    }

    public boolean contains(UUID id, long epoch, UUID execution) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.admin_preview_turn WHERE conversation_id=? AND context_epoch=? AND execution_id=?)",
                        Boolean.class,
                        id,
                        epoch,
                        execution));
    }

    public void change(UUID id, long epoch, boolean delete) {
        jdbc.update(
                "UPDATE public.agent_execution SET cancellation_requested_at=COALESCE(cancellation_requested_at,CURRENT_TIMESTAMP) WHERE id IN (SELECT execution_id FROM public.admin_preview_turn WHERE conversation_id=? AND context_epoch=?) AND status IN ('ACCEPTED','RUNNING')",
                id,
                epoch);
        jdbc.update(
                "UPDATE public.admin_preview_conversation SET context_epoch=context_epoch+1,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP,status=? WHERE id=?",
                delete ? "DELETED" : "ACTIVE",
                id);
    }
}
