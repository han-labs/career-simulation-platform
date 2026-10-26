package edu.hcmute.careersim.guidance.agent.dao;

import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.RoleBinding;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AgentRoleBindingDao {
    private final JdbcTemplate jdbc;

    public void lock() {
        jdbc.queryForList("SELECT role FROM agent_role_binding ORDER BY role FOR UPDATE");
    }

    public List<RoleBinding> list() {
        return jdbc.query(
                "SELECT b.*,a.name FROM agent_role_binding b LEFT JOIN agent a ON a.id=b.agent_id ORDER BY b.role",
                (r, n) ->
                        new RoleBinding(
                                r.getString("role"),
                                r.getString("agent_id"),
                                r.getString("name"),
                                r.getLong("row_version"),
                                r.getTimestamp("updated_at").toInstant()));
    }

    public void bind(String role, UUID agent) {
        jdbc.update(
                "UPDATE agent_role_binding SET agent_id=?,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE role=? AND agent_id IS DISTINCT FROM ?",
                agent,
                role,
                agent);
    }

    public void shareStudent() {
        jdbc.queryForList("SELECT role FROM agent_role_binding WHERE role='STUDENT' FOR SHARE");
    }
}
