package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Completion;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CompletionDao {
    private final JdbcTemplate jdbc;

    public long insert(long enrollment, Instant now, String snippet, String code, Object snapshot) {
        return jdbc.queryForObject(
                "INSERT INTO public.simulation_completion(enrollment_id,completed_at,resume_snippet_text,snippet_template_version,certificate_code,certificate_snapshot,certificate_issued_at) VALUES (?,?,?,'completion-v1',?,CAST(? AS jsonb),?) RETURNING id",
                Long.class,
                enrollment,
                java.sql.Timestamp.from(now),
                snippet,
                code,
                CoreValues.write(snapshot),
                java.sql.Timestamp.from(now));
    }

    public void complete(long enrollment, Instant now) {
        jdbc.update(
                "UPDATE public.simulation_enrollment SET status='COMPLETED',completed_at=?,row_version=row_version+1 WHERE id=?",
                java.sql.Timestamp.from(now),
                enrollment);
    }

    public Completion read(long owner, long id) {
        return jdbc
                .query(
                        "SELECT c.* FROM public.simulation_completion c JOIN public.simulation_enrollment e ON e.id=c.enrollment_id WHERE c.id=? AND e.student_id=?",
                        (r, n) ->
                                new Completion(
                                        Long.toString(r.getLong("id")),
                                        Long.toString(r.getLong("enrollment_id")),
                                        r.getTimestamp("completed_at").toInstant(),
                                        r.getString("resume_snippet_text"),
                                        r.getString("snippet_template_version"),
                                        r.getString("certificate_code"),
                                        CoreValues.json(r.getString("certificate_snapshot")),
                                        r.getTimestamp("certificate_issued_at").toInstant()),
                        id,
                        owner)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Completion not found."));
    }

    public List<Completion> list(long owner) {
        return jdbc
                .query(
                        "SELECT c.id FROM public.simulation_completion c JOIN public.simulation_enrollment e ON e.id=c.enrollment_id WHERE e.student_id=? ORDER BY c.completed_at DESC,c.id DESC LIMIT 100",
                        (r, n) -> r.getLong(1),
                        owner)
                .stream()
                .map(id -> read(owner, id))
                .toList();
    }

    public List<String> requiredSkills(long enrollment) {
        return jdbc.query(
                "SELECT DISTINCT ts.skill_name_snapshot FROM public.task_submission s JOIN public.simulation_task t ON t.id=s.task_id JOIN public.task_skill ts ON ts.task_id=t.id WHERE s.enrollment_id=? AND s.status='FINAL' AND t.required ORDER BY ts.skill_name_snapshot",
                (r, n) -> r.getString(1),
                enrollment);
    }
}
