package edu.hcmute.careersim.simulation.dao;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LearningPolicyDao {
    private final JdbcTemplate jdbc;

    public boolean ordered(long version) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.scenario_learning_policy WHERE scenario_version_id=?)",
                        Boolean.class,
                        version));
    }

    public void create(long version) {
        jdbc.update(
                "INSERT INTO public.scenario_learning_policy VALUES (?,'ordered-review/1','ORDERED_SELF_REVIEW','1') ON CONFLICT DO NOTHING",
                version);
    }

    public String type(long task) {
        return jdbc
                .query(
                        "SELECT task_type FROM public.task_learning_policy WHERE task_id=?",
                        (r, n) -> r.getString(1),
                        task)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Rubric rubric(long task) {
        return jdbc
                .query(
                        "SELECT version,evaluator_key,quiz_scoring_config::text FROM public.task_rubric WHERE task_id=?",
                        (r, n) ->
                                new Rubric(
                                        r.getInt(1),
                                        r.getString(2),
                                        CoreValues.json(r.getString(3))),
                        task)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public void put(long task, Task t) {
        jdbc.update(
                "INSERT INTO public.task_learning_policy(task_id,task_type) VALUES (?,?)",
                task,
                t.taskType());
        if (t.rubric() != null)
            jdbc.update(
                    "INSERT INTO public.task_rubric(task_id,version,evaluator_key,quiz_scoring_config) VALUES (?,?,?,CAST(? AS jsonb))",
                    task,
                    t.rubric().version(),
                    t.rubric().evaluatorKey(),
                    CoreValues.write(t.rubric().quizScoringConfig()));
    }

    public List<TaskProgress> progress(long enrollment, long version) {
        boolean ordered = ordered(version);
        var rows =
                jdbc.query(
                        "SELECT t.id::text,s.status='FINAL' AS finalized,r.submission_id IS NOT NULL AS reviewed FROM public.simulation_task t LEFT JOIN public.task_submission s ON s.task_id=t.id AND s.enrollment_id=? LEFT JOIN public.task_self_review r ON r.submission_id=s.id WHERE t.scenario_version_id=? ORDER BY t.position,t.id",
                        (r, n) ->
                                new TaskProgress(
                                        r.getString(1), true, r.getBoolean(2), r.getBoolean(3)),
                        enrollment,
                        version);
        return edu.hcmute.careersim.simulation.domain.TaskProgressionPolicy.project(rows, ordered);
    }

    public void requireAccessible(long enrollment, long version, long task) {
        var p =
                progress(enrollment, version).stream()
                        .filter(r -> r.taskId().equals(Long.toString(task)))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Task not found."));
        if (!p.accessible())
            throw new ApiException(
                    ErrorCode.CONFLICT, "Finalize and self-review the previous task first.");
    }

    public void finalized(long submission, long actor, Task task, JsonNode score) {
        jdbc.update(
                "INSERT INTO public.task_submission_actor(submission_id,submitted_by_user_id) VALUES (?,?)",
                submission,
                actor);
        if (score != null)
            jdbc.update(
                    "INSERT INTO public.task_quiz_result(submission_id,task_id,rubric_version,evaluator_key,score_data) VALUES (?,?,?,?,CAST(? AS jsonb))",
                    submission,
                    CoreValues.id(task.id()),
                    task.rubric().version(),
                    task.rubric().evaluatorKey(),
                    CoreValues.write(score));
    }

    public void review(long submission, long actor) {
        // Avoid ON CONFLICT's INSERT trigger on an existing immutable record.
        if (reviewed(submission) == null)
            jdbc.update(
                    "INSERT INTO public.task_self_review(submission_id,reviewed_by_user_id) VALUES (?,?)",
                    submission,
                    actor);
    }

    public Instant reviewed(long submission) {
        return jdbc
                .query(
                        "SELECT reviewed_at FROM public.task_self_review WHERE submission_id=?",
                        (r, n) -> r.getTimestamp(1).toInstant(),
                        submission)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public JsonNode score(long submission) {
        return jdbc
                .query(
                        "SELECT score_data::text FROM public.task_quiz_result WHERE submission_id=?",
                        (r, n) -> CoreValues.json(r.getString(1)),
                        submission)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public String actor(long submission) {
        return jdbc
                .query(
                        "SELECT submitted_by_user_id::text FROM public.task_submission_actor WHERE submission_id=?",
                        (r, n) -> r.getString(1),
                        submission)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
