package edu.hcmute.careersim.guidance.agent.dao;

import static edu.hcmute.careersim.guidance.agent.dto.StudentAgentContracts.*;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InsightReportDao {
    private final JdbcTemplate jdbc;
    private final RowMapper<Report> map =
            (r, n) ->
                    new Report(
                            r.getString("id"),
                            r.getString("source_execution_id"),
                            r.getString("schema_version"),
                            CoreValues.json(r.getString("structured_insights")),
                            CoreValues.json(r.getString("evidence_refs")),
                            r.getTimestamp("created_at").toInstant());

    public String forExecution(UUID execution) {
        return jdbc
                .query(
                        "SELECT id FROM public.insight_report WHERE source_execution_id=? AND status='AVAILABLE'",
                        (r, n) -> r.getString(1),
                        execution)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public void insert(long owner, UUID execution, UUID input, JsonNode report) {
        jdbc.update(
                "INSERT INTO public.insight_report(id,owner_user_id,requested_interaction_id,source_execution_id,schema_version,structured_insights,evidence_refs) VALUES (?,?,?,?,'careersim-insight-v1',CAST(? AS jsonb),CAST(? AS jsonb))",
                UUID.randomUUID(),
                owner,
                input,
                execution,
                CoreValues.write(report),
                CoreValues.write(report.path("evidenceRefs")));
    }

    public List<Report> list(long owner) {
        return jdbc.query(
                "SELECT * FROM public.insight_report WHERE owner_user_id=? AND status='AVAILABLE' ORDER BY created_at DESC,id DESC LIMIT 100",
                map,
                owner);
    }

    public Report get(long owner, UUID id) {
        return jdbc
                .query(
                        "SELECT * FROM public.insight_report WHERE owner_user_id=? AND id=? AND status='AVAILABLE'",
                        map,
                        owner,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Report unavailable."));
    }
}
