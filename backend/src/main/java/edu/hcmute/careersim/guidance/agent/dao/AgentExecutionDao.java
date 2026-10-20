package edu.hcmute.careersim.guidance.agent.dao;

import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.Execution;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AgentExecutionDao {
    private final JdbcTemplate jdbc;
    private final RowMapper<Execution> mapping =
            (r, n) ->
                    new Execution(
                            r.getString("id"),
                            r.getString("execution_mode"),
                            r.getString("task_type"),
                            r.getString("initiated_by_user_id"),
                            r.getString("session_id"),
                            r.getString("configuration_id"),
                            r.getString("input_interaction_id"),
                            r.getString("knowledge_base_id"),
                            r.getObject("request_id", UUID.class),
                            CoreValues.json(r.getString("input_snapshot")),
                            r.getString("status"),
                            r.getString("refusal_or_error_code"),
                            CoreValues.json(r.getString("tool_steps_and_sources")),
                            r.getString("output_snapshot") == null
                                    ? null
                                    : CoreValues.json(r.getString("output_snapshot")),
                            r.getString("model_key"),
                            r.getString("prompt_hash"),
                            r.getString("configuration_hash"),
                            r.getObject("input_tokens", Long.class),
                            r.getObject("output_tokens", Long.class),
                            r.getBigDecimal("estimated_cost"),
                            r.getString("cost_status"),
                            r.getObject("latency_ms", Long.class),
                            r.getString("pricing_snapshot") == null
                                    ? null
                                    : CoreValues.json(r.getString("pricing_snapshot")),
                            r.getTimestamp("started_at").toInstant(),
                            r.getTimestamp("finished_at") == null
                                    ? null
                                    : r.getTimestamp("finished_at").toInstant(),
                            r.getTimestamp("cancellation_requested_at") != null);

    public Optional<Execution> request(long actor, String mode, UUID request) {
        return jdbc
                .query(
                        "SELECT * FROM public.agent_execution WHERE initiated_by_user_id=? AND execution_mode=? AND request_id=?",
                        mapping,
                        actor,
                        mode,
                        request)
                .stream()
                .findFirst();
    }

    public String hash(UUID id) {
        return jdbc.queryForObject(
                "SELECT request_hash FROM public.agent_execution WHERE id=?", String.class, id);
    }

    public Execution get(UUID id, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM public.agent_execution WHERE id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        mapping,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Execution not found."));
    }

    public Execution accept(
            long actor,
            String mode,
            UUID config,
            UUID kb,
            UUID request,
            String hash,
            Object input,
            String model,
            String promptHash,
            String configHash,
            long privacy) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.agent_execution(id,execution_mode,task_type,initiated_by_user_id,configuration_id,knowledge_base_id,input_snapshot,request_id,request_hash,status,model_key,prompt_hash,configuration_hash,privacy_version) VALUES (?,?,?,?,?,?,CAST(? AS jsonb),?,?,'ACCEPTED',?,?,?,?)",
                id,
                mode,
                "KB_TEST".equals(mode) ? "KB_QUERY" : "CHAT",
                actor,
                config,
                kb,
                CoreValues.write(input),
                request,
                hash,
                model,
                promptHash,
                configHash,
                privacy);
        return get(id, false);
    }

    public Execution acceptStudent(
            long actor,
            UUID session,
            UUID config,
            UUID inputId,
            UUID request,
            String hash,
            String task,
            Object input,
            String model,
            String promptHash,
            String configHash,
            long privacy,
            long epoch) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.agent_execution(id,execution_mode,task_type,initiated_by_user_id,session_id,configuration_id,input_interaction_id,input_snapshot,request_id,request_hash,status,model_key,prompt_hash,configuration_hash,privacy_version,context_epoch) VALUES (?,'STUDENT_CHAT',?,?,?,?,?,CAST(? AS jsonb),?,?,'ACCEPTED',?,?,?,?,?)",
                id,
                task,
                actor,
                session,
                config,
                inputId,
                CoreValues.write(input),
                request,
                hash,
                model,
                promptHash,
                configHash,
                privacy,
                epoch);
        return get(id, false);
    }

    public long epoch(UUID execution) {
        return jdbc.queryForObject(
                "SELECT context_epoch FROM public.agent_execution WHERE id=?",
                Long.class,
                execution);
    }

    public long privacy(UUID execution) {
        return jdbc.queryForObject(
                "SELECT privacy_version FROM public.agent_execution WHERE id=?",
                Long.class,
                execution);
    }

    public void finish(
            UUID id,
            String status,
            String error,
            Object steps,
            Object output,
            Long input,
            Long out,
            BigDecimal cost,
            String costStatus,
            long latency,
            Object pricing) {
        jdbc.update(
                "UPDATE public.agent_execution SET status=?,refusal_or_error_code=?,tool_steps_and_sources=CAST(? AS jsonb),output_snapshot=CAST(? AS jsonb),input_tokens=?,output_tokens=?,estimated_cost=?,cost_status=?,latency_ms=?,pricing_snapshot=CAST(? AS jsonb),finished_at=CURRENT_TIMESTAMP WHERE id=? AND status IN ('ACCEPTED','RUNNING')",
                status,
                error,
                CoreValues.write(steps),
                output == null ? null : CoreValues.write(output),
                input,
                out,
                cost,
                costStatus,
                latency,
                CoreValues.write(pricing),
                id);
    }

    public void begin(UUID id) {
        jdbc.update(
                "UPDATE public.agent_execution SET status='RUNNING' WHERE id=? AND status='ACCEPTED' AND cancellation_requested_at IS NULL",
                id);
    }

    public void cancel(UUID id) {
        jdbc.update(
                "UPDATE public.agent_execution SET cancellation_requested_at=COALESCE(cancellation_requested_at,CURRENT_TIMESTAMP) WHERE id=? AND status IN ('ACCEPTED','RUNNING')",
                id);
    }
}
