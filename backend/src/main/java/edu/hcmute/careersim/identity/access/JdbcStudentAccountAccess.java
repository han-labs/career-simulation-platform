package edu.hcmute.careersim.identity.access;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcStudentAccountAccess implements StudentAccountAccess {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<StudentAccount> findByEmail(String email) {
        List<StudentAccount> accounts =
                jdbcTemplate.query(
                        """
                        SELECT u.id, u.display_name, u.role, u.status, p.consented_to_ai_at
                        FROM app_users u
                        LEFT JOIN student_profiles p ON p.user_id = u.id
                        WHERE LOWER(u.email) = LOWER(?)
                        """,
                        (resultSet, rowNumber) ->
                                new StudentAccount(
                                        resultSet.getLong("id"),
                                        resultSet.getString("display_name"),
                                        resultSet.getString("role"),
                                        resultSet.getString("status"),
                                        resultSet.getTimestamp("consented_to_ai_at") == null
                                                ? null
                                                : resultSet
                                                        .getTimestamp("consented_to_ai_at")
                                                        .toInstant()),
                        email);
        return accounts.stream().findFirst();
    }
}
