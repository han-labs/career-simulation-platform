package edu.hcmute.careersim.guidance.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcSynTurnDao implements SynTurnDao {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    private RowMapper<Turn> mapping() {
        return (rs, row) -> {
            try {
                return new Turn(
                        rs.getObject("id", UUID.class),
                        rs.getLong("student_id"),
                        rs.getObject("request_id", UUID.class),
                        rs.getString("request_hash"),
                        rs.getObject("session_id", UUID.class),
                        rs.getLong("session_version"),
                        rs.getString("state"),
                        rs.getString("response") == null
                                ? null
                                : mapper.readValue(
                                        rs.getString("response"), SynMessageResponse.class),
                        rs.getBoolean("expired"));
            } catch (java.io.IOException e) {
                throw new java.sql.SQLException("Invalid stored turn", e);
            }
        };
    }

    private static final String SELECT =
            "SELECT *, response_expires_at <= CURRENT_TIMESTAMP AS expired FROM syn_turns ";

    public void lockStudent(long id) {
        jdbc.queryForObject("SELECT id FROM app_users WHERE id=? FOR UPDATE", Long.class, id);
        jdbc.query(
                "SELECT user_id FROM student_profiles WHERE user_id=? FOR SHARE",
                (rs, i) -> rs.getLong(1),
                id);
    }

    public Optional<Turn> findRequest(long id, UUID request) {
        return jdbc
                .query(SELECT + "WHERE student_id=? AND request_id=?", mapping(), id, request)
                .stream()
                .findFirst();
    }

    public Optional<Turn> find(long owner, UUID id) {
        return jdbc.query(SELECT + "WHERE student_id=? AND id=?", mapping(), owner, id).stream()
                .findFirst();
    }

    public long sessionVersion(long owner, UUID session) {
        List<Long> versions =
                jdbc.query(
                        "SELECT version FROM syn_sessions WHERE id=? AND student_id=? AND expires_at > CURRENT_TIMESTAMP FOR UPDATE",
                        (rs, i) -> rs.getLong(1),
                        session,
                        owner);
        return versions.isEmpty() ? -1 : versions.get(0);
    }

    public boolean sessionAlive(long owner, UUID session) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM syn_sessions WHERE id=? AND student_id=? AND expires_at>CURRENT_TIMESTAMP)",
                        Boolean.class,
                        session,
                        owner));
    }

    public boolean pending(UUID session) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM syn_turns WHERE session_id=? AND state='PENDING')",
                        Boolean.class,
                        session));
    }

    public int recentCount(long studentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM syn_turns WHERE student_id=? AND created_at>CURRENT_TIMESTAMP-INTERVAL '1 minute'",
                Integer.class,
                studentId);
    }

    public void insert(Turn t) {
        jdbc.update(
                "INSERT INTO syn_turns(id,student_id,request_id,request_hash,session_id,session_version,state) VALUES (?,?,?,?,?,?,'PENDING')",
                t.id(),
                t.studentId(),
                t.requestId(),
                t.requestHash(),
                t.sessionId(),
                t.sessionVersion());
    }

    public void complete(
            long owner,
            UUID id,
            SynMessageResponse reply,
            String reason,
            String model,
            int ms,
            java.util.Map<String, Object> trace) {
        try {
            if (jdbc.update(
                            "UPDATE syn_turns SET state='COMPLETED',source=?,reason=?,model_name=?,response=CAST(? AS JSONB),generation_ms=?,trace=CAST(? AS JSONB),completed_at=CURRENT_TIMESTAMP WHERE id=? AND student_id=? AND state='PENDING'",
                            reply.provenance(),
                            reason,
                            model,
                            mapper.writeValueAsString(reply),
                            ms,
                            mapper.writeValueAsString(trace),
                            id,
                            owner)
                    != 1) {
                throw new IllegalStateException("Turn is no longer pending");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize turn", e);
        }
    }

    public void fail(long owner, UUID id) {
        jdbc.update(
                "UPDATE syn_turns SET state='FAILED',reason='TURN_FAILED',completed_at=CURRENT_TIMESTAMP WHERE id=? AND student_id=? AND state='PENDING'",
                id,
                owner);
    }

    public void cancelSession(long owner, UUID session) {
        jdbc.update(
                "UPDATE syn_turns SET state='CANCELLED',response=NULL,reason='SESSION_CLEARED',completed_at=CURRENT_TIMESTAMP WHERE student_id=? AND session_id=?",
                owner,
                session);
    }

    public void cancel(long owner, UUID id) {
        jdbc.update(
                "UPDATE syn_turns SET state='CANCELLED',response=NULL,reason='USER_CANCELLED',completed_at=CURRENT_TIMESTAMP WHERE student_id=? AND id=?",
                owner,
                id);
    }

    public void recoverInterrupted(long owner) {
        jdbc.update(
                "UPDATE syn_turns SET state='FAILED',reason='INTERRUPTED',completed_at=CURRENT_TIMESTAMP WHERE student_id=? AND state='PENDING' AND created_at<CURRENT_TIMESTAMP-INTERVAL '1 minute'",
                owner);
    }

    public void cleanup() {
        jdbc.update(
                "UPDATE syn_turns SET state='FAILED',reason='INTERRUPTED',completed_at=CURRENT_TIMESTAMP WHERE state='PENDING' AND created_at<CURRENT_TIMESTAMP-INTERVAL '1 minute'");
        jdbc.update(
                "UPDATE syn_turns SET response=NULL WHERE response_expires_at<CURRENT_TIMESTAMP AND response IS NOT NULL");
        jdbc.update("DELETE FROM syn_turns WHERE created_at<CURRENT_TIMESTAMP-INTERVAL '30 days'");
        jdbc.update("DELETE FROM syn_sessions WHERE expires_at<CURRENT_TIMESTAMP");
    }
}
