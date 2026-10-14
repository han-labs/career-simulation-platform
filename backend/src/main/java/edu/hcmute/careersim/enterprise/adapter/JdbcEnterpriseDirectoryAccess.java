package edu.hcmute.careersim.enterprise.adapter;

import edu.hcmute.careersim.enterprise.access.EnterpriseDirectoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcEnterpriseDirectoryAccess implements EnterpriseDirectoryAccess {
    private final JdbcTemplate jdbc;

    public String name(long id) {
        return jdbc.queryForObject(
                "SELECT name FROM public.enterprise WHERE id=?", String.class, id);
    }
}
