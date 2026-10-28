package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentDao {
    public boolean ownsFinal(long owner, long submission, long version, String skill) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.task_submission s JOIN public.simulation_enrollment e ON e.id=s.enrollment_id JOIN public.scenario_version v ON v.id=s.scenario_version_id JOIN public.scenario c ON c.id=v.scenario_id JOIN public.task_skill ts ON ts.task_id=s.task_id JOIN public.career_skill k ON k.id=ts.skill_id WHERE e.student_id=? AND s.id=? AND s.scenario_version_id=? AND k.code=? AND s.status='FINAL' AND c.availability_status<>'SUSPENDED')",
                        Boolean.class,
                        owner,
                        submission,
                        version,
                        skill));
    }

    private final JdbcTemplate jdbc;

    public record Row(
            long id,
            long owner,
            long version,
            String status,
            long revision,
            Instant enrolled,
            Instant started,
            Instant completed) {}

    public record SubmissionRow(
            long id,
            long enrollment,
            long task,
            long version,
            String status,
            long revision,
            UUID request,
            String hash,
            Instant submitted) {}

    public Row owned(long actor, long id, boolean lock) {
        return jdbc
                .query(
                        "SELECT id,student_id,scenario_version_id,status,row_version,enrolled_at,started_at,completed_at FROM public.simulation_enrollment WHERE id=? AND student_id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        (r, n) ->
                                new Row(
                                        r.getLong(1),
                                        r.getLong(2),
                                        r.getLong(3),
                                        r.getString(4),
                                        r.getLong(5),
                                        r.getTimestamp(6).toInstant(),
                                        r.getTimestamp(7) == null
                                                ? null
                                                : r.getTimestamp(7).toInstant(),
                                        r.getTimestamp(8) == null
                                                ? null
                                                : r.getTimestamp(8).toInstant()),
                        id,
                        actor)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Enrollment not found."));
    }

    public Optional<Long> existing(long owner, long version) {
        return jdbc
                .query(
                        "SELECT id FROM public.simulation_enrollment WHERE student_id=? AND scenario_version_id=?",
                        (r, n) -> r.getLong(1),
                        owner,
                        version)
                .stream()
                .findFirst();
    }

    public long enroll(long owner, long version) {
        return jdbc.queryForObject(
                "INSERT INTO public.simulation_enrollment(student_id,scenario_version_id) VALUES (?,?) ON CONFLICT(student_id,scenario_version_id) DO UPDATE SET student_id=EXCLUDED.student_id RETURNING id",
                Long.class,
                owner,
                version);
    }

    public List<Long> list(long owner, long after, int limit) {
        return jdbc.query(
                "SELECT e.id FROM public.simulation_enrollment e JOIN public.scenario_version v ON v.id=e.scenario_version_id JOIN public.scenario c ON c.id=v.scenario_id WHERE e.student_id=? AND e.id>? AND c.availability_status<>'SUSPENDED' ORDER BY e.id LIMIT ?",
                (r, n) -> r.getLong(1),
                owner,
                after,
                limit);
    }

    public OwnActivity activity(long owner) {
        return jdbc.queryForObject(
                "SELECT count(*),count(DISTINCT v.scenario_id),count(*) FILTER(WHERE e.status='ENROLLED'),count(*) FILTER(WHERE e.status='IN_PROGRESS'),count(*) FILTER(WHERE e.status='COMPLETED') FROM public.simulation_enrollment e JOIN public.scenario_version v ON v.id=e.scenario_version_id JOIN public.scenario c ON c.id=v.scenario_id WHERE e.student_id=? AND c.availability_status<>'SUSPENDED'",
                (r, n) ->
                        new OwnActivity(
                                r.getLong(1),
                                r.getLong(2),
                                r.getLong(3),
                                r.getLong(4),
                                r.getLong(5)),
                owner);
    }

    public Optional<SubmissionRow> submission(long enrollment, long task, boolean lock) {
        return jdbc
                .query(
                        "SELECT id,enrollment_id,task_id,scenario_version_id,status,row_version,final_request_id,final_payload_hash,submitted_at FROM public.task_submission WHERE enrollment_id=? AND task_id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        (r, n) ->
                                new SubmissionRow(
                                        r.getLong(1),
                                        r.getLong(2),
                                        r.getLong(3),
                                        r.getLong(4),
                                        r.getString(5),
                                        r.getLong(6),
                                        r.getObject(7, UUID.class),
                                        r.getString(8),
                                        r.getTimestamp(9) == null
                                                ? null
                                                : r.getTimestamp(9).toInstant()),
                        enrollment,
                        task)
                .stream()
                .findFirst();
    }

    public SubmissionRow draft(Row enrollment, long task) {
        jdbc.update(
                "INSERT INTO public.task_submission(enrollment_id,task_id,scenario_version_id) VALUES (?,?,?) ON CONFLICT(enrollment_id,task_id) DO NOTHING",
                enrollment.id(),
                task,
                enrollment.version());
        return submission(enrollment.id(), task, true).orElseThrow();
    }

    public List<Item> items(long submission) {
        return jdbc.query(
                "SELECT definition_id::text,content_text,content_url,file_asset_id::text,content_json::text FROM public.submitted_deliverable WHERE submission_id=? ORDER BY definition_id",
                (r, n) ->
                        new Item(
                                r.getString(1),
                                r.getString(2),
                                r.getString(3),
                                r.getString(4),
                                r.getString(5) == null ? null : CoreValues.json(r.getString(5))),
                submission);
    }

    public void save(SubmissionRow s, List<Item> items) {
        jdbc.update("DELETE FROM public.submitted_deliverable WHERE submission_id=?", s.id());
        for (var i : items)
            jdbc.update(
                    "INSERT INTO public.submitted_deliverable(submission_id,definition_id,task_id,content_text,content_url,file_asset_id,content_json) VALUES (?,?,?,?,?,?,CAST(? AS jsonb))",
                    s.id(),
                    CoreValues.id(i.definitionId()),
                    s.task(),
                    i.contentText(),
                    i.contentUrl(),
                    i.fileAssetId() == null ? null : CoreValues.id(i.fileAssetId()),
                    i.contentJson() == null ? null : CoreValues.write(i.contentJson()));
        jdbc.update(
                "UPDATE public.task_submission SET row_version=row_version+1 WHERE id=?", s.id());
    }

    public void started(long enrollment) {
        jdbc.update(
                "UPDATE public.simulation_enrollment SET status=CASE WHEN status='ENROLLED' THEN 'IN_PROGRESS' ELSE status END,started_at=COALESCE(started_at,CURRENT_TIMESTAMP),row_version=row_version+1 WHERE id=?",
                enrollment);
    }

    public void finalizeTask(long submission, UUID request, String hash) {
        jdbc.update(
                "UPDATE public.task_submission SET status='FINAL',final_request_id=?,final_payload_hash=?,submitted_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=?",
                request,
                hash,
                submission);
    }

    public Optional<Long> usedRequest(long enrollment, UUID request) {
        return jdbc
                .query(
                        "SELECT task_id FROM public.task_submission WHERE enrollment_id=? AND final_request_id=?",
                        (r, n) -> r.getLong(1),
                        enrollment,
                        request)
                .stream()
                .findFirst();
    }

    public int required(long version) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM public.simulation_task WHERE scenario_version_id=? AND required",
                Integer.class,
                version);
    }

    public int finalRequired(long enrollment, long version) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM public.task_submission s JOIN public.simulation_task t ON t.id=s.task_id WHERE s.enrollment_id=? AND t.scenario_version_id=? AND t.required AND s.status='FINAL'",
                Integer.class,
                enrollment,
                version);
    }

    public Set<String> finalTasks(long enrollment) {
        return new HashSet<>(
                jdbc.query(
                        "SELECT task_id::text FROM public.task_submission WHERE enrollment_id=? AND status='FINAL'",
                        (r, n) -> r.getString(1),
                        enrollment));
    }

    public Optional<String> completionId(long enrollment) {
        return jdbc
                .query(
                        "SELECT id::text FROM public.simulation_completion WHERE enrollment_id=?",
                        (r, n) -> r.getString(1),
                        enrollment)
                .stream()
                .findFirst();
    }

    public List<String> practicedSkills(long enrollment) {
        return jdbc.query(
                "SELECT DISTINCT ts.skill_name_snapshot FROM public.task_submission s JOIN public.task_skill ts ON ts.task_id=s.task_id WHERE s.enrollment_id=? AND s.status='FINAL' ORDER BY ts.skill_name_snapshot",
                (r, n) -> r.getString(1),
                enrollment);
    }

    public List<Participation> participation(long owner, int limit) {
        return jdbc.query(
                "SELECT s.id::text,s.task_id::text,s.scenario_version_id::text,k.code,ts.skill_name_snapshot,s.submitted_at FROM public.task_submission s JOIN public.simulation_enrollment e ON e.id=s.enrollment_id JOIN public.task_skill ts ON ts.task_id=s.task_id JOIN public.career_skill k ON k.id=ts.skill_id WHERE e.student_id=? AND s.status='FINAL' AND EXISTS(SELECT 1 FROM public.scenario_version v JOIN public.scenario c ON c.id=v.scenario_id WHERE v.id=s.scenario_version_id AND c.availability_status<>'SUSPENDED') ORDER BY s.submitted_at DESC,s.id,ts.skill_id LIMIT ?",
                (r, n) ->
                        new Participation(
                                r.getString(1),
                                r.getString(2),
                                r.getString(3),
                                r.getString(4),
                                r.getString(5),
                                r.getTimestamp(6).toInstant()),
                owner,
                limit);
    }
}
