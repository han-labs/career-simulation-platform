package edu.hcmute.careersim.system.migration;

import static edu.hcmute.careersim.system.migration.MigrationContracts.*;

import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MigrationRecordDao {
    public static final List<String> LEGACY =
            List.of(
                    "app_users",
                    "student_profiles",
                    "riasec_questions",
                    "assessment_attempts",
                    "assessment_answers",
                    "assessment_scores",
                    "career_simulations",
                    "simulation_tasks",
                    "simulation_attempts",
                    "task_submissions",
                    "evaluation_results",
                    "guidance_reports",
                    "exploration_plans",
                    "syn_sessions",
                    "career_path_profiles",
                    "learning_resources",
                    "simulation_task_skills",
                    "syn_turns",
                    "knowledge_documents",
                    "knowledge_versions",
                    "knowledge_chunks");
    private final JdbcTemplate jdbc;

    public String database() {
        return jdbc.queryForObject("SELECT current_database()", String.class);
    }

    public String schema() {
        return jdbc.queryForObject("SELECT current_schema()", String.class);
    }

    public String head() {
        return jdbc.queryForObject(
                "SELECT version FROM public.flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1",
                String.class);
    }

    public Map<String, Long> counts() {
        Map<String, Long> m = new TreeMap<>();
        for (String t : LEGACY)
            m.put(t, jdbc.queryForObject("SELECT count(*) FROM public." + t, Long.class));
        return m;
    }

    public Map<String, Long> conflicts() {
        var m = new TreeMap<String, Long>();
        m.put(
                "NORMALIZED_EMAIL_COLLISION",
                jdbc.queryForObject(
                        "SELECT count(*) FROM (SELECT lower(btrim(email)) FROM public.app_users GROUP BY 1 HAVING count(*)>1) c",
                        Long.class));
        m.put(
                "UNKNOWN_CONTENT_OWNER",
                jdbc.queryForObject(
                        "SELECT count(*) FROM public.career_simulations WHERE provider_id IS NULL",
                        Long.class));
        m.put(
                "WRONG_TASK_MEMBERSHIP",
                jdbc.queryForObject(
                        "SELECT count(*) FROM public.task_submissions t JOIN public.simulation_attempts a ON a.id=t.attempt_id JOIN public.simulation_tasks d ON d.id=t.task_id WHERE a.simulation_id<>d.simulation_id",
                        Long.class));
        m.put(
                "WRONG_KNOWLEDGE_VERSION",
                jdbc.queryForObject(
                        "SELECT count(*) FROM public.knowledge_documents d JOIN public.knowledge_versions v ON v.id=d.active_version_id WHERE v.document_id<>d.id",
                        Long.class));
        m.put(
                "INVALID_STUDENT_PROFILE",
                jdbc.queryForObject(
                        "SELECT count(*) FROM public.student_profiles p JOIN public.app_users u ON u.id=p.user_id WHERE u.role<>'STUDENT'",
                        Long.class));
        return m;
    }

    public String sourceHash(Entry e) {
        if (!LEGACY.contains(e.sourceTable())) CoreValues.invalid("Unknown legacy source.");
        List<Map<String, Object>> rows;
        if ("riasec_questions".equals(e.sourceTable()) && "snapshot".equals(e.sourceKey()))
            rows =
                    jdbc.queryForList(
                            "SELECT id,dimension,prompt,display_order,active FROM public.riasec_questions ORDER BY id");
        else if ("career_simulations".equals(e.sourceTable())) {
            long id = CoreValues.id(e.sourceKey());
            var root = jdbc.queryForList("SELECT * FROM public.career_simulations WHERE id=?", id);
            rows = new ArrayList<>(root);
            rows.addAll(
                    jdbc.queryForList(
                            "SELECT * FROM public.simulation_tasks WHERE simulation_id=? ORDER BY id",
                            id));
            rows.addAll(
                    jdbc.queryForList(
                            "SELECT k.* FROM public.simulation_task_skills k JOIN public.simulation_tasks t ON t.id=k.task_id WHERE t.simulation_id=? ORDER BY k.task_id,k.skill_code",
                            id));
        } else {
            String column =
                    switch (e.sourceTable()) {
                        case "student_profiles" -> "user_id";
                        case "evaluation_results" -> "attempt_id";
                        default -> "id";
                    };
            if (!Set.of(
                            "exploration_plans",
                            "app_users",
                            "student_profiles",
                            "career_skill",
                            "evaluation_results")
                    .contains(e.sourceTable()))
                CoreValues.invalid("Source requires a separately reviewed mapping kind.");
            rows =
                    jdbc.queryForList(
                            "SELECT * FROM public." + e.sourceTable() + " WHERE " + column + "=?",
                            CoreValues.id(e.sourceKey()));
        }
        if (rows.isEmpty()) CoreValues.invalid("Source record missing.");
        return CoreValues.hash(rows);
    }

    public List<Map<String, Object>> currentQuestions() {
        return jdbc.queryForList(
                "SELECT id,dimension,prompt,display_order FROM public.riasec_questions WHERE active ORDER BY display_order,id");
    }

    public record LegacyPlan(
            long owner,
            String title,
            com.fasterxml.jackson.databind.JsonNode steps,
            String status) {}

    public LegacyPlan legacyPlan(long id) {
        return jdbc
                .query(
                        "SELECT student_id,title,steps::text,status FROM public.exploration_plans WHERE id=?",
                        (r, n) ->
                                new LegacyPlan(
                                        r.getLong(1),
                                        r.getString(2),
                                        CoreValues.json(r.getString(3)),
                                        r.getString(4)),
                        id)
                .stream()
                .findFirst()
                .orElseThrow();
    }

    public Optional<Map<String, Object>> record(Entry e) {
        return jdbc
                .queryForList(
                        "SELECT source_hash,state,target_table,target_key,details->>'mappingHash' AS mapping_hash,details->>'targetKind' AS target_kind FROM public.migration_record WHERE source_table=? AND source_key=? FOR UPDATE",
                        e.sourceTable(),
                        e.sourceKey())
                .stream()
                .findFirst();
    }

    public void mapped(Entry e, Target target, long reviewer) {
        jdbc.update(
                "INSERT INTO public.migration_record(source_table,source_key,target_table,target_key,source_hash,state,reason_code,reviewed_by,reviewed_at,details) VALUES (?,?,?,?,?,'MAPPED','REVIEWED_COPY',?,CURRENT_TIMESTAMP,CAST(? AS jsonb))",
                e.sourceTable(),
                e.sourceKey(),
                target.table(),
                target.key(),
                e.sourceHash(),
                reviewer,
                CoreValues.write(
                        Map.of(
                                "mappingHash",
                                CoreValues.hash(e.reviewedMapping()),
                                "targetKind",
                                e.targetKind())));
    }

    public void conflict(Entry e, String reason) {
        jdbc.update(
                "INSERT INTO public.migration_record(source_table,source_key,source_hash,state,reason_code) VALUES (?,?,?,'CONFLICT',?) ON CONFLICT(source_table,source_key) DO UPDATE SET state='CONFLICT',reason_code=EXCLUDED.reason_code,updated_at=CURRENT_TIMESTAMP",
                e.sourceTable(),
                e.sourceKey(),
                e.sourceHash(),
                reason);
    }
}
