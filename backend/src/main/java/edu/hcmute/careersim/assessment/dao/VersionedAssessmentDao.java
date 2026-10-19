package edu.hcmute.careersim.assessment.dao;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VersionedAssessmentDao {
    private final JdbcTemplate jdbc;

    public record AttemptRow(
            long id,
            long owner,
            long version,
            String status,
            long revision,
            String hash,
            UUID startRequest) {}

    public long type(TypeRequest r) {
        return jdbc.queryForObject(
                "INSERT INTO public.assessment_type(code,name) VALUES (?,?) RETURNING id",
                Long.class,
                r.code(),
                r.name());
    }

    public List<Type> catalog() {
        return jdbc.query(
                "SELECT t.id,t.code,t.name,t.active_version_id FROM public.assessment_type t JOIN public.assessment_version v ON v.id=t.active_version_id WHERE t.status='ACTIVE' AND v.status='PUBLISHED' ORDER BY t.id",
                (r, n) ->
                        new Type(
                                Long.toString(r.getLong(1)),
                                r.getString(2),
                                r.getString(3),
                                Long.toString(r.getLong(4))));
    }

    public List<Type> administrationTypes() {
        return jdbc.query(
                "SELECT id::text,code,name,active_version_id::text FROM public.assessment_type ORDER BY id",
                (r, n) -> new Type(r.getString(1), r.getString(2), r.getString(3), r.getString(4)));
    }

    public List<Version> versions(long type) {
        return jdbc
                .query(
                        "SELECT id FROM public.assessment_version WHERE assessment_type_id=? ORDER BY version_number DESC LIMIT 100",
                        (r, n) -> r.getLong(1),
                        type)
                .stream()
                .map(id -> version(id, false))
                .toList();
    }

    public String typeCode(long type) {
        return jdbc.queryForObject(
                "SELECT code FROM public.assessment_type WHERE id=?", String.class, type);
    }

    public void lockType(long type) {
        if (jdbc.query(
                        "SELECT id FROM public.assessment_type WHERE id=? FOR UPDATE",
                        (r, n) -> r.getLong(1),
                        type)
                .isEmpty()) throw new NotFoundException("Assessment type not found.");
    }

    public long version(long type, VersionDraft v) {
        return jdbc.queryForObject(
                "INSERT INTO public.assessment_version(assessment_type_id,version_number,evaluator_key,evaluator_version,scoring_config) SELECT ?,COALESCE(MAX(version_number),0)+1,?,?,CAST(? AS jsonb) FROM public.assessment_version WHERE assessment_type_id=? RETURNING id",
                Long.class,
                type,
                v.evaluatorKey(),
                v.evaluatorVersion(),
                CoreValues.write(v.scoringConfig()),
                type);
    }

    public Version version(long id, boolean lock) {
        return jdbc
                .query(
                        "SELECT id,assessment_type_id,version_number,status,evaluator_key,evaluator_version,scoring_config::text,content_hash,row_version FROM public.assessment_version WHERE id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        (r, n) ->
                                new Version(
                                        Long.toString(r.getLong(1)),
                                        Long.toString(r.getLong(2)),
                                        r.getInt(3),
                                        r.getString(4),
                                        r.getString(5),
                                        r.getString(6),
                                        CoreValues.json(r.getString(7)),
                                        r.getString(8),
                                        r.getLong(9),
                                        List.of()),
                        id)
                .stream()
                .findFirst()
                .map(
                        v ->
                                new Version(
                                        v.id(),
                                        v.typeId(),
                                        v.versionNumber(),
                                        v.status(),
                                        v.evaluatorKey(),
                                        v.evaluatorVersion(),
                                        v.scoringConfig(),
                                        v.hash(),
                                        v.revision(),
                                        questions(id)))
                .orElseThrow(() -> new NotFoundException("Assessment version not found."));
    }

    public List<Question> questions(long version) {
        return jdbc.query(
                "SELECT id,position,prompt,response_type,response_config::text,translations::text,required FROM public.assessment_question WHERE assessment_version_id=? ORDER BY position,id",
                (r, n) ->
                        new Question(
                                Long.toString(r.getLong(1)),
                                r.getInt(2),
                                r.getString(3),
                                r.getString(4),
                                CoreValues.json(r.getString(5)),
                                CoreValues.json(r.getString(6)),
                                r.getBoolean(7)),
                version);
    }

    public void questions(long version, VersionDraft v) {
        jdbc.update(
                "DELETE FROM public.assessment_question WHERE assessment_version_id=?", version);
        for (var q : v.questions())
            jdbc.update(
                    "INSERT INTO public.assessment_question(assessment_version_id,position,prompt,response_type,response_config,translations,required) VALUES (?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),?)",
                    version,
                    q.position(),
                    q.prompt(),
                    q.responseType(),
                    CoreValues.write(q.responseConfig()),
                    CoreValues.write(q.translations()),
                    q.required());
    }

    public void draft(long version, VersionDraft v) {
        jdbc.update(
                "UPDATE public.assessment_version SET evaluator_key=?,evaluator_version=?,scoring_config=CAST(? AS jsonb),row_version=row_version+1 WHERE id=?",
                v.evaluatorKey(),
                v.evaluatorVersion(),
                CoreValues.write(v.scoringConfig()),
                version);
        questions(version, v);
    }

    public void publish(long version, String hash, long type) {
        jdbc.update(
                "UPDATE public.assessment_version SET status='PUBLISHED',content_hash=?,published_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=?",
                hash,
                version);
        jdbc.update(
                "UPDATE public.assessment_type SET active_version_id=?,row_version=row_version+1 WHERE id=?",
                version,
                type);
    }

    public boolean typeActive(long type) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT status='ACTIVE' FROM public.assessment_type WHERE id=?",
                        Boolean.class,
                        type));
    }

    public Optional<AttemptRow> request(long owner, UUID request) {
        return jdbc
                .query(
                        "SELECT id,student_id,assessment_version_id,status,row_version,final_payload_hash,start_request_id FROM public.assessment_attempt WHERE student_id=? AND start_request_id=?",
                        (r, n) ->
                                new AttemptRow(
                                        r.getLong(1),
                                        r.getLong(2),
                                        r.getLong(3),
                                        r.getString(4),
                                        r.getLong(5),
                                        r.getString(6),
                                        r.getObject(7, UUID.class)),
                        owner,
                        request)
                .stream()
                .findFirst();
    }

    public long start(long owner, long version, UUID request) {
        return jdbc.queryForObject(
                "INSERT INTO public.assessment_attempt(student_id,assessment_version_id,start_request_id) VALUES (?,?,?) RETURNING id",
                Long.class,
                owner,
                version,
                request);
    }

    public AttemptRow owned(long owner, long id, boolean lock) {
        return jdbc
                .query(
                        "SELECT id,student_id,assessment_version_id,status,row_version,final_payload_hash,start_request_id FROM public.assessment_attempt WHERE id=? AND student_id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        (r, n) ->
                                new AttemptRow(
                                        r.getLong(1),
                                        r.getLong(2),
                                        r.getLong(3),
                                        r.getString(4),
                                        r.getLong(5),
                                        r.getString(6),
                                        r.getObject(7, UUID.class)),
                        id,
                        owner)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Assessment attempt not found."));
    }

    public Attempt view(long owner, long id) {
        var row = owned(owner, id, false);
        var times =
                jdbc.queryForMap(
                        "SELECT started_at,submitted_at FROM public.assessment_attempt WHERE id=?",
                        id);
        var responses =
                jdbc.query(
                        "SELECT question_id,answer_data::text FROM public.assessment_response WHERE attempt_id=? ORDER BY question_id",
                        (r, n) ->
                                new Answer(
                                        Long.toString(r.getLong(1)),
                                        CoreValues.json(r.getString(2))),
                        id);
        return new Attempt(
                Long.toString(id),
                Long.toString(row.version()),
                row.status(),
                row.revision(),
                ((java.sql.Timestamp) times.get("started_at")).toInstant(),
                times.get("submitted_at") == null
                        ? null
                        : ((java.sql.Timestamp) times.get("submitted_at")).toInstant(),
                questions(row.version()),
                responses);
    }

    public void responses(AttemptRow row, List<Answer> items) {
        for (var a : items)
            jdbc.update(
                    "INSERT INTO public.assessment_response(attempt_id,question_id,assessment_version_id,answer_data) VALUES (?,?,?,CAST(? AS jsonb)) ON CONFLICT(attempt_id,question_id) DO UPDATE SET answer_data=EXCLUDED.answer_data,updated_at=CURRENT_TIMESTAMP",
                    row.id(),
                    CoreValues.id(a.questionId()),
                    row.version(),
                    CoreValues.write(a.answerData()));
        jdbc.update(
                "UPDATE public.assessment_attempt SET row_version=row_version+1 WHERE id=?",
                row.id());
    }

    public void result(
            AttemptRow a, Version v, String hash, com.fasterxml.jackson.databind.JsonNode data) {
        jdbc.update(
                "INSERT INTO public.assessment_result(attempt_id,result_data,evaluator_key,evaluator_version,input_hash) VALUES (?,CAST(? AS jsonb),?,?,?)",
                a.id(),
                CoreValues.write(data),
                v.evaluatorKey(),
                v.evaluatorVersion(),
                hash);
        jdbc.update(
                "UPDATE public.assessment_attempt SET status='SUBMITTED',submitted_at=CURRENT_TIMESTAMP,final_payload_hash=?,row_version=row_version+1 WHERE id=?",
                hash,
                a.id());
    }

    public Result result(long owner, long id) {
        owned(owner, id, false);
        return jdbc
                .query(
                        "SELECT r.attempt_id,t.code,a.assessment_version_id,r.evaluator_key,r.evaluator_version,r.result_data::text,r.calculated_at FROM public.assessment_result r JOIN public.assessment_attempt a ON a.id=r.attempt_id JOIN public.assessment_version v ON v.id=a.assessment_version_id JOIN public.assessment_type t ON t.id=v.assessment_type_id WHERE r.attempt_id=?",
                        (r, n) ->
                                new Result(
                                        Long.toString(r.getLong(1)),
                                        r.getString(2),
                                        Long.toString(r.getLong(3)),
                                        r.getString(4),
                                        r.getString(5),
                                        CoreValues.json(r.getString(6)),
                                        r.getTimestamp(7).toInstant()),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Submitted result not found."));
    }

    public List<Result> results(long owner) {
        return jdbc
                .query(
                        "SELECT a.id FROM public.assessment_attempt a WHERE student_id=? AND status='SUBMITTED' ORDER BY submitted_at DESC,id DESC LIMIT 100",
                        (r, n) -> r.getLong(1),
                        owner)
                .stream()
                .map(id -> result(owner, id))
                .toList();
    }
}
