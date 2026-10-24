package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ScenarioDao {
    private final JdbcTemplate jdbc;
    private final LearningPolicyDao learning;

    public Version ownedVersion(long version, long enterprise, boolean lock) {
        if (!Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.scenario_version v JOIN public.scenario s ON s.id=v.scenario_id WHERE v.id=? AND s.enterprise_id=?)",
                        Boolean.class,
                        version,
                        enterprise))) throw new NotFoundException("Scenario version not found.");
        return version(version, lock);
    }

    public record Root(
            long id,
            long enterprise,
            String slug,
            String status,
            String industryCode,
            long revision,
            Long current) {}

    public Root root(long id, boolean lock) {
        return jdbc
                .query(
                        "SELECT id,enterprise_id,slug,availability_status,industry_code,row_version,current_version_id FROM public.scenario WHERE id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        (r, n) ->
                                new Root(
                                        r.getLong(1),
                                        r.getLong(2),
                                        r.getString(3),
                                        r.getString(4),
                                        r.getString(5),
                                        r.getLong(6),
                                        r.getObject(7, Long.class)),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Scenario not found."));
    }

    public long create(long enterprise, String slug) {
        return jdbc.queryForObject(
                "INSERT INTO public.scenario(enterprise_id,slug) VALUES (?,?) RETURNING id",
                Long.class,
                enterprise,
                slug);
    }

    public long version(long scenario, Metadata m) {
        long id =
                jdbc.queryForObject(
                        "INSERT INTO public.scenario_version(scenario_id,version_number,title,target_role,career_track,summary,introduction,learning_objectives,difficulty,estimated_minutes,translations,provenance) SELECT ?,COALESCE(MAX(version_number),0)+1,?,?,?,?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb) FROM public.scenario_version WHERE scenario_id=? RETURNING id",
                        Long.class,
                        scenario,
                        m.title(),
                        m.targetRole(),
                        m.careerTrack(),
                        m.summary(),
                        m.introduction(),
                        m.learningObjectives(),
                        m.difficulty(),
                        m.estimatedMinutes(),
                        CoreValues.write(m.translations()),
                        CoreValues.write(m.provenance()),
                        scenario);
        learning.create(id);
        return id;
    }

    public Version version(long id, boolean lock) {
        var v =
                jdbc
                        .query(
                                "SELECT v.*,s.enterprise_id,s.availability_status,s.row_version AS scenario_revision FROM public.scenario_version v JOIN public.scenario s ON s.id=v.scenario_id WHERE v.id=?"
                                        + (lock ? " FOR UPDATE OF v" : ""),
                                (r, n) ->
                                        new Version(
                                                Long.toString(r.getLong("id")),
                                                Long.toString(r.getLong("scenario_id")),
                                                Long.toString(r.getLong("enterprise_id")),
                                                r.getInt("version_number"),
                                                r.getString("status"),
                                                r.getString("availability_status"),
                                                r.getLong("row_version"),
                                                r.getLong("scenario_revision"),
                                                r.getString("content_hash"),
                                                new Metadata(
                                                        r.getString("title"),
                                                        r.getString("target_role"),
                                                        r.getString("career_track"),
                                                        r.getString("summary"),
                                                        r.getString("introduction"),
                                                        r.getString("learning_objectives"),
                                                        r.getString("difficulty"),
                                                        r.getInt("estimated_minutes"),
                                                        CoreValues.json(
                                                                r.getString("translations")),
                                                        CoreValues.json(r.getString("provenance"))),
                                                List.of()),
                                id)
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Scenario version not found."));
        return new Version(
                v.id(),
                v.scenarioId(),
                v.enterpriseId(),
                v.versionNumber(),
                v.status(),
                v.availabilityStatus(),
                v.revision(),
                v.scenarioRevision(),
                v.contentHash(),
                v.metadata(),
                tasks(id));
    }

    public List<Task> tasks(long version) {
        return jdbc
                .query(
                        "SELECT id,position,title,workplace_context,instructions,required,translations::text FROM public.simulation_task WHERE scenario_version_id=? ORDER BY position,id",
                        (r, n) ->
                                new Task(
                                        Long.toString(r.getLong(1)),
                                        r.getInt(2),
                                        r.getString(3),
                                        r.getString(4),
                                        r.getString(5),
                                        r.getBoolean(6),
                                        CoreValues.json(r.getString(7)),
                                        List.of(),
                                        List.of(),
                                        List.of(),
                                        null),
                        version)
                .stream()
                .map(
                        t -> {
                            long id = CoreValues.id(t.id());
                            return new Task(
                                    t.id(),
                                    t.position(),
                                    t.title(),
                                    t.workplaceContext(),
                                    t.instructions(),
                                    t.required(),
                                    t.translations(),
                                    definitions(id),
                                    resources(id),
                                    skillIds(id),
                                    answer(id),
                                    learning.type(id),
                                    learning.rubric(id));
                        })
                .toList();
    }

    public List<Definition> definitions(long task) {
        return jdbc.query(
                "SELECT id,position,name,format,required,validation_config::text FROM public.task_deliverable_definition WHERE task_id=? ORDER BY position,id",
                (r, n) ->
                        new Definition(
                                Long.toString(r.getLong(1)),
                                r.getInt(2),
                                r.getString(3),
                                r.getString(4),
                                r.getBoolean(5),
                                CoreValues.json(r.getString(6))),
                task);
    }

    public List<Resource> resources(long task) {
        return jdbc.query(
                "SELECT id,position,title,file_asset_id::text,external_url,release_policy FROM public.task_resource WHERE task_id=? ORDER BY position,id",
                (r, n) ->
                        new Resource(
                                Long.toString(r.getLong(1)),
                                r.getInt(2),
                                r.getString(3),
                                r.getString(4),
                                r.getString(5),
                                r.getString(6)),
                task);
    }

    public Answer answer(long task) {
        return jdbc
                .query(
                        "SELECT content,translations::text FROM public.model_answer WHERE task_id=?",
                        (r, n) -> new Answer(r.getString(1), CoreValues.json(r.getString(2))),
                        task)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<String> skillIds(long task) {
        return jdbc.query(
                "SELECT skill_id::text FROM public.task_skill WHERE task_id=? ORDER BY skill_id",
                (r, n) -> r.getString(1),
                task);
    }

    public List<Skill> skills() {
        return jdbc.query(
                "SELECT id::text,code,name,status,row_version FROM public.career_skill ORDER BY id",
                (r, n) ->
                        new Skill(
                                r.getString(1),
                                r.getString(2),
                                r.getString(3),
                                r.getString(4),
                                r.getLong(5)));
    }

    public String skill(SkillRequest r) {
        return jdbc.queryForObject(
                "INSERT INTO public.career_skill(code,name) VALUES (?,?) RETURNING id::text",
                String.class,
                r.code(),
                r.name());
    }

    public void metadata(long id, Metadata m) {
        jdbc.update(
                "UPDATE public.scenario_version SET title=?,target_role=?,career_track=?,summary=?,introduction=?,learning_objectives=?,difficulty=?,estimated_minutes=?,translations=CAST(? AS jsonb),provenance=CAST(? AS jsonb),row_version=row_version+1 WHERE id=?",
                m.title(),
                m.targetRole(),
                m.careerTrack(),
                m.summary(),
                m.introduction(),
                m.learningObjectives(),
                m.difficulty(),
                m.estimatedMinutes(),
                CoreValues.write(m.translations()),
                CoreValues.write(m.provenance()),
                id);
    }

    public void replaceTasks(long version, List<Task> tasks) {
        for (String table :
                List.of(
                        "task_rubric",
                        "task_learning_policy",
                        "task_skill",
                        "model_answer",
                        "task_resource",
                        "task_deliverable_definition"))
            jdbc.update(
                    "DELETE FROM public."
                            + table
                            + " WHERE task_id IN (SELECT id FROM public.simulation_task WHERE scenario_version_id=?)",
                    version);
        jdbc.update("DELETE FROM public.simulation_task WHERE scenario_version_id=?", version);
        for (var t : tasks) {
            Long id = t.id() == null ? null : CoreValues.id(t.id());
            if (id == null)
                id =
                        jdbc.queryForObject(
                                "INSERT INTO public.simulation_task(scenario_version_id,position,title,workplace_context,instructions,required,translations) VALUES (?,?,?,?,?,?,CAST(? AS jsonb)) RETURNING id",
                                Long.class,
                                version,
                                t.position(),
                                t.title(),
                                t.workplaceContext(),
                                t.instructions(),
                                t.required(),
                                CoreValues.write(t.translations()));
            else
                jdbc.update(
                        "INSERT INTO public.simulation_task(id,scenario_version_id,position,title,workplace_context,instructions,required,translations) VALUES (?,?,?,?,?,?,?,CAST(? AS jsonb))",
                        id,
                        version,
                        t.position(),
                        t.title(),
                        t.workplaceContext(),
                        t.instructions(),
                        t.required(),
                        CoreValues.write(t.translations()));
            learning.put(id, t);
            for (var d : t.definitions()) {
                if (d.id() == null)
                    jdbc.update(
                            "INSERT INTO public.task_deliverable_definition(task_id,position,name,format,required,validation_config) VALUES (?,?,?,?,?,CAST(? AS jsonb))",
                            id,
                            d.position(),
                            d.name(),
                            d.format(),
                            d.required(),
                            CoreValues.write(d.validationConfig()));
                else
                    jdbc.update(
                            "INSERT INTO public.task_deliverable_definition(id,task_id,position,name,format,required,validation_config) VALUES (?,?,?,?,?,?,CAST(? AS jsonb))",
                            CoreValues.id(d.id()),
                            id,
                            d.position(),
                            d.name(),
                            d.format(),
                            d.required(),
                            CoreValues.write(d.validationConfig()));
            }
            for (var r : t.resources()) {
                if (r.id() == null)
                    jdbc.update(
                            "INSERT INTO public.task_resource(task_id,position,title,file_asset_id,external_url,release_policy) VALUES (?,?,?,?,?,?)",
                            id,
                            r.position(),
                            r.title(),
                            r.fileAssetId() == null ? null : CoreValues.id(r.fileAssetId()),
                            r.externalUrl(),
                            r.releasePolicy());
                else
                    jdbc.update(
                            "INSERT INTO public.task_resource(id,task_id,position,title,file_asset_id,external_url,release_policy) VALUES (?,?,?,?,?,?,?)",
                            CoreValues.id(r.id()),
                            id,
                            r.position(),
                            r.title(),
                            r.fileAssetId() == null ? null : CoreValues.id(r.fileAssetId()),
                            r.externalUrl(),
                            r.releasePolicy());
            }
            for (var s : t.skillIds())
                jdbc.update(
                        "INSERT INTO public.task_skill(task_id,skill_id,skill_name_snapshot) SELECT ?,id,name FROM public.career_skill WHERE id=?",
                        id,
                        CoreValues.id(s));
            if (t.modelAnswer() != null)
                jdbc.update(
                        "INSERT INTO public.model_answer(task_id,content,translations) VALUES (?,?,CAST(? AS jsonb))",
                        id,
                        t.modelAnswer().content(),
                        CoreValues.write(t.modelAnswer().translations()));
        }
    }

    public void publish(long id, long scenario, String hash) {
        jdbc.update(
                "UPDATE public.scenario_version SET status='PUBLISHED',content_hash=?,published_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=?",
                hash,
                id);
        jdbc.update(
                "UPDATE public.scenario SET current_version_id=?,availability_status='AVAILABLE',row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                id,
                scenario);
    }

    public void availability(long scenario, String status) {
        jdbc.update(
                "UPDATE public.scenario SET availability_status=?,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                status,
                scenario);
    }

    public void classification(long scenario, String industryCode) {
        jdbc.update(
                "UPDATE public.scenario SET industry_code=?,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                industryCode,
                scenario);
    }

    public List<Summary> catalog(Long after, int limit) {
        return catalog(after, limit, "");
    }

    public List<Summary> catalog(Long after, int limit, String industry) {
        var args = new ArrayList<Object>();
        args.add(after == null ? 0 : after);
        if (!industry.isEmpty()) args.add(industry);
        args.add(limit);
        String sql =
                "SELECT s.id,s.slug,v.id AS version_id,s.enterprise_id,e.name,s.industry_code FROM public.scenario s JOIN public.scenario_version v ON v.id=s.current_version_id JOIN public.enterprise e ON e.id=s.enterprise_id WHERE s.availability_status='AVAILABLE' AND v.status='PUBLISHED' AND s.id>?";
        if (!industry.isEmpty()) sql += " AND s.industry_code=?";
        sql += " ORDER BY s.id LIMIT ?";
        return jdbc
                .query(
                        sql,
                        (r, n) ->
                                new Summary(
                                        Long.toString(r.getLong(1)),
                                        r.getString(2),
                                        Long.toString(r.getLong(3)),
                                        Long.toString(r.getLong(4)),
                                        r.getString(5),
                                        r.getString(6),
                                        null),
                        args.toArray())
                .stream()
                .map(
                        s ->
                                new Summary(
                                        s.id(),
                                        s.slug(),
                                        s.scenarioVersionId(),
                                        s.enterpriseId(),
                                        s.enterpriseName(),
                                        s.industryCode(),
                                        publicMetadata(
                                                version(CoreValues.id(s.scenarioVersionId()), false)
                                                        .metadata())))
                .toList();
    }

    public Optional<Long> bySlug(String slug) {
        return jdbc
                .query(
                        "SELECT current_version_id FROM public.scenario WHERE slug=? AND availability_status='AVAILABLE' AND current_version_id IS NOT NULL",
                        (r, n) -> r.getLong(1),
                        slug)
                .stream()
                .findFirst();
    }

    public Metadata publicMetadata(Metadata m) {
        return new Metadata(
                m.title(),
                m.targetRole(),
                m.careerTrack(),
                m.summary(),
                m.introduction(),
                m.learningObjectives(),
                m.difficulty(),
                m.estimatedMinutes(),
                m.translations(),
                CoreValues.tree(Map.of()));
    }
}
