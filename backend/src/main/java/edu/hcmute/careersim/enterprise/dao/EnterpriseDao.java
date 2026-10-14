package edu.hcmute.careersim.enterprise.dao;

import edu.hcmute.careersim.enterprise.access.EnterpriseAccess.Membership;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnterpriseDao {
    private final JdbcTemplate jdbc;

    public long create(String name) {
        return jdbc.queryForObject(
                "INSERT INTO public.enterprise(name) VALUES (?) RETURNING id", Long.class, name);
    }

    public void representative(long user, long enterprise) {
        jdbc.update(
                "INSERT INTO public.enterprise_representative(user_id,enterprise_id) VALUES (?,?)",
                user,
                enterprise);
    }

    public Optional<Membership> membership(long actor) {
        return jdbc
                .query(
                        "SELECT e.id,e.name,e.verification_status,e.publishing_status,r.membership_status,r.publishing_permission,e.row_version,r.row_version FROM public.enterprise_representative r JOIN public.enterprise e ON e.id=r.enterprise_id WHERE r.user_id=?",
                        (r, n) ->
                                new Membership(
                                        Long.toString(r.getLong(1)),
                                        r.getString(2),
                                        r.getString(3),
                                        r.getString(4),
                                        r.getString(5),
                                        r.getBoolean(6),
                                        r.getLong(7),
                                        r.getLong(8)),
                        actor)
                .stream()
                .findFirst();
    }

    public long lock(long id) {
        return jdbc.queryForObject(
                "SELECT row_version FROM public.enterprise WHERE id=? FOR UPDATE", Long.class, id);
    }

    public void verification(long id, String state) {
        jdbc.update(
                "UPDATE public.enterprise SET verification_status=?,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                state,
                id);
    }

    public void publishing(long id, String state) {
        jdbc.update(
                "UPDATE public.enterprise SET publishing_status=?,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                state,
                id);
    }

    public Optional<Long> memberEnterprise(long id) {
        return jdbc
                .query(
                        "SELECT enterprise_id FROM public.enterprise_representative WHERE user_id=? FOR UPDATE",
                        (r, n) -> r.getLong(1),
                        id)
                .stream()
                .findFirst();
    }

    public long memberRevision(long id) {
        return jdbc.queryForObject(
                "SELECT row_version FROM public.enterprise_representative WHERE user_id=?",
                Long.class,
                id);
    }

    public String accountRole(long id) {
        return jdbc.queryForObject(
                "SELECT role FROM public.app_users WHERE id=? FOR UPDATE", String.class, id);
    }

    public void membership(long user, String status, boolean permission) {
        jdbc.update(
                "UPDATE public.enterprise_representative SET membership_status=?,publishing_permission=?,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE user_id=?",
                status,
                permission,
                user);
        jdbc.update(
                "UPDATE public.app_users SET privacy_version=privacy_version+1,row_version=row_version+1 WHERE id=?",
                user);
    }
}
