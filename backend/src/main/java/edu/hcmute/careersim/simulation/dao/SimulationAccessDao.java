package edu.hcmute.careersim.simulation.dao;

import edu.hcmute.careersim.common.exception.NotFoundException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SimulationAccessDao {
    private final JdbcTemplate jdbc;

    public record Work(
            long owner,
            long enterprise,
            long enrollment,
            long task,
            long version,
            String availability,
            String status) {}

    public Work work(long submission) {
        return jdbc
                .query(
                        "SELECT e.student_id,s.enterprise_id,e.id,t.task_id,e.scenario_version_id,s.availability_status,t.status FROM public.task_submission t JOIN public.simulation_enrollment e ON e.id=t.enrollment_id JOIN public.scenario_version v ON v.id=e.scenario_version_id JOIN public.scenario s ON s.id=v.scenario_id WHERE t.id=?",
                        (r, n) ->
                                new Work(
                                        r.getLong(1),
                                        r.getLong(2),
                                        r.getLong(3),
                                        r.getLong(4),
                                        r.getLong(5),
                                        r.getString(6),
                                        r.getString(7)),
                        submission)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Submission not found."));
    }

    public boolean attached(long submission, long asset) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.submitted_deliverable WHERE submission_id=? AND file_asset_id=?)",
                        Boolean.class,
                        submission,
                        asset));
    }

    public record Resource(
            long id,
            long task,
            long version,
            long enterprise,
            long asset,
            String release,
            String availability) {}

    public Resource resource(long id) {
        return jdbc
                .query(
                        "SELECT r.id,r.task_id,t.scenario_version_id,s.enterprise_id,r.file_asset_id,r.release_policy,s.availability_status FROM public.task_resource r JOIN public.simulation_task t ON t.id=r.task_id JOIN public.scenario_version v ON v.id=t.scenario_version_id JOIN public.scenario s ON s.id=v.scenario_id WHERE r.id=? AND r.file_asset_id IS NOT NULL",
                        (r, n) ->
                                new Resource(
                                        r.getLong(1),
                                        r.getLong(2),
                                        r.getLong(3),
                                        r.getLong(4),
                                        r.getLong(5),
                                        r.getString(6),
                                        r.getString(7)),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Resource not found."));
    }

    public Optional<Long> enrollment(long student, long version) {
        return jdbc
                .query(
                        "SELECT id FROM public.simulation_enrollment WHERE student_id=? AND scenario_version_id=?",
                        (r, n) -> r.getLong(1),
                        student,
                        version)
                .stream()
                .findFirst();
    }
}
