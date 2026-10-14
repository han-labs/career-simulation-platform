package edu.hcmute.careersim.guidance.dao;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.dto.PlanContracts.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConfirmedPlanDao {
    public long importConfirmed(
            long owner, String title, List<String> steps, java.time.Instant at) {
        return jdbc.queryForObject(
                "INSERT INTO public.exploration_plan(student_id,title,steps,confirmed_at) VALUES (?,?,CAST(? AS jsonb),?) RETURNING id",
                Long.class,
                owner,
                title,
                CoreValues.write(steps),
                java.sql.Timestamp.from(at));
    }

    private final JdbcTemplate jdbc;

    public List<Plan> list(long user) {
        return jdbc.query(
                "SELECT id::text,title,steps::text,status,confirmed_at,row_version FROM public.exploration_plan WHERE student_id=? ORDER BY confirmed_at DESC,id DESC LIMIT 100",
                (r, n) -> {
                    List<String> steps = new ArrayList<>();
                    CoreValues.json(r.getString(3)).forEach(v -> steps.add(v.asText()));
                    return new Plan(
                            r.getString(1),
                            r.getString(2),
                            steps,
                            r.getString(4),
                            r.getTimestamp(5).toInstant(),
                            r.getLong(6));
                },
                user);
    }

    public void archive(long owner) {
        jdbc.update(
                "UPDATE public.exploration_plan SET status='ARCHIVED',row_version=row_version+1 WHERE student_id=? AND status='ACTIVE'",
                owner);
    }

    public long insert(long owner, Confirm r) {
        return jdbc.queryForObject(
                "INSERT INTO public.exploration_plan(student_id,title,steps,confirmed_at) VALUES (?,?,CAST(? AS jsonb),CURRENT_TIMESTAMP) RETURNING id",
                Long.class,
                owner,
                r.title(),
                CoreValues.write(r.steps()));
    }
}
