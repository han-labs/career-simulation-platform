package edu.hcmute.careersim.identity.access;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStudentAccountAccess implements StudentAccountAccess {

    private final JdbcTemplate jdbcTemplate;
    private final ConsentAccess consents;
    private final String provider;

    JdbcStudentAccountAccess(
            JdbcTemplate jdbcTemplate,
            ConsentAccess consents,
            @org.springframework.beans.factory.annotation.Value("${app.ai.provider:disabled}")
                    String provider) {
        this.jdbcTemplate = jdbcTemplate;
        this.consents = consents;
        this.provider = provider;
    }

    @Override
    public Optional<StudentAccount> findByEmail(String email) {
        List<StudentAccount> accounts =
                jdbcTemplate.query(
                        """
                        SELECT u.id, u.display_name, u.role, u.status, u.privacy_version
                        FROM public.app_users u
                        JOIN public.student_profiles p ON p.user_id = u.id
                        WHERE LOWER(u.email) = LOWER(?)
                        """,
                        (resultSet, rowNumber) ->
                                new StudentAccount(
                                        resultSet.getLong("id"),
                                        resultSet.getString("display_name"),
                                        resultSet.getString("role"),
                                        resultSet.getString("status"),
                                        consents.effective(
                                                        resultSet.getLong("id"),
                                                        "EXTERNAL_AI",
                                                        "provider:" + provider)
                                                ? java.time.Instant.now()
                                                : null,
                                        resultSet.getLong("privacy_version")),
                        email);
        return accounts.stream().findFirst();
    }
}
