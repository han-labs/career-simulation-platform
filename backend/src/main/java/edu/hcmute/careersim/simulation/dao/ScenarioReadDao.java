package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ScenarioReadDao {
    private final JdbcTemplate jdbc;
    private final ScenarioDao scenarios;

    public CatalogPage search(CatalogQuery q) {
        String text = q.q() == null ? "" : q.q().strip().toLowerCase(Locale.ROOT);
        String track = q.careerTrack() == null ? "" : q.careerTrack().strip();
        String difficulty = q.difficulty() == null ? "" : q.difficulty();
        String industry = q.industry();
        String sort = q.sort() == null ? "ID_ASC" : q.sort();
        if (text.length() > 200
                || track.length() > 60
                || (!difficulty.isEmpty()
                        && !Set.of("INTRODUCTORY", "INTERMEDIATE", "ADVANCED").contains(difficulty))
                || !Set.of("ID_ASC", "TITLE_ASC", "DURATION_ASC", "NEWEST").contains(sort))
            CoreValues.invalid("Invalid catalogue query.");
        int limit = Math.max(1, Math.min(q.limit(), 100));
        String fingerprint =
                industry.isEmpty()
                        ? CoreValues.hash(List.of(text, track, difficulty, sort))
                        : CoreValues.hash(List.of(text, track, difficulty, sort, industry));
        String key = "";
        long lastId = 0;
        if (q.cursor() != null && !q.cursor().isBlank()) {
            try {
                if (q.cursor().length() > 2048) throw new IllegalArgumentException();
                var cursor =
                        CoreValues.json(
                                new String(
                                        Base64.getUrlDecoder().decode(q.cursor()),
                                        StandardCharsets.UTF_8));
                if (!fingerprint.equals(cursor.path("query").asText()))
                    throw new IllegalArgumentException();
                key = cursor.path("key").asText();
                lastId = CoreValues.id(cursor.path("id").asText());
            } catch (RuntimeException e) {
                throw new ApiException(
                        ErrorCode.BAD_REQUEST, "Cursor does not match this catalogue query.");
            }
        }
        String expression =
                switch (sort) {
                    case "TITLE_ASC" -> "lower(v.title)";
                    case "DURATION_ASC" -> "v.estimated_minutes";
                    case "NEWEST" -> "v.published_at";
                    default -> "s.id";
                };
        String cast =
                switch (sort) {
                    case "DURATION_ASC", "ID_ASC" -> "bigint";
                    case "NEWEST" -> "timestamptz";
                    default -> "text";
                };
        boolean descending = "NEWEST".equals(sort);
        var args = new ArrayList<Object>();
        String sql =
                "SELECT s.id,"
                        + expression
                        + "::text AS sort_key FROM public.scenario s JOIN public.scenario_version v ON v.id=s.current_version_id JOIN public.enterprise e ON e.id=s.enterprise_id WHERE s.availability_status='AVAILABLE' AND v.status='PUBLISHED'";
        if (!text.isEmpty()) {
            sql +=
                    " AND (strpos(lower(v.title),?)>0 OR strpos(lower(v.summary),?)>0 OR strpos(lower(v.target_role),?)>0)";
            args.add(text);
            args.add(text);
            args.add(text);
        }
        if (!track.isEmpty()) {
            sql += " AND v.career_track=?";
            args.add(track);
        }
        if (!difficulty.isEmpty()) {
            sql += " AND v.difficulty=?";
            args.add(difficulty);
        }
        if (!industry.isEmpty()) {
            sql += " AND s.industry_code=?";
            args.add(industry);
        }
        if (lastId != 0) {
            sql +=
                    " AND ("
                            + expression
                            + ",s.id)"
                            + (descending ? "<" : ">")
                            + "(CAST(? AS "
                            + cast
                            + "),?)";
            args.add(key);
            args.add(lastId);
        }
        sql +=
                " ORDER BY "
                        + expression
                        + (descending ? " DESC" : " ASC")
                        + ",s.id"
                        + (descending ? " DESC" : " ASC")
                        + " LIMIT ?";
        args.add(limit + 1);
        var rows =
                jdbc.query(
                        sql,
                        (r, n) -> new String[] {r.getString(1), r.getString(2)},
                        args.toArray());
        String next = null;
        if (rows.size() > limit) {
            var last = rows.get(limit - 1);
            next =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    CoreValues.write(
                                                    CoreValues.tree(
                                                            Map.of(
                                                                    "id",
                                                                    last[0],
                                                                    "key",
                                                                    last[1],
                                                                    "query",
                                                                    fingerprint)))
                                            .getBytes(StandardCharsets.UTF_8));
        }
        var items =
                rows.stream()
                        .limit(limit)
                        .map(r -> scenarios.catalog(Long.parseLong(r[0]) - 1, 1).get(0))
                        .toList();
        return new CatalogPage(items, next);
    }

    public List<Version> versions(long enterprise) {
        return jdbc
                .query(
                        "SELECT v.id FROM public.scenario_version v JOIN public.scenario s ON s.id=v.scenario_id WHERE s.enterprise_id=? ORDER BY v.id DESC LIMIT 100",
                        (r, n) -> r.getLong(1),
                        enterprise)
                .stream()
                .map(id -> scenarios.version(id, false))
                .toList();
    }

    public List<ParticipationSummary> participation(long enterprise) {
        return jdbc.query(
                "SELECT v.id::text,v.scenario_id::text,v.title,v.version_number,count(e.id),count(DISTINCT e.student_id),count(e.id) FILTER(WHERE e.status='COMPLETED') FROM public.scenario_version v JOIN public.scenario s ON s.id=v.scenario_id LEFT JOIN public.simulation_enrollment e ON e.scenario_version_id=v.id WHERE s.enterprise_id=? GROUP BY v.id ORDER BY v.id",
                (r, n) ->
                        new ParticipationSummary(
                                r.getString(1),
                                r.getString(2),
                                r.getString(3),
                                r.getInt(4),
                                r.getLong(5),
                                r.getLong(6),
                                r.getLong(7)),
                enterprise);
    }
}
