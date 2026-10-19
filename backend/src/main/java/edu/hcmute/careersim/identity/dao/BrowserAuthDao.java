package edu.hcmute.careersim.identity.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BrowserAuthDao {
    private final JdbcTemplate jdbc;

    public long version(long user) {
        return jdbc.queryForObject(
                "SELECT auth_version FROM public.app_users WHERE id=?", Long.class, user);
    }

    public void lock(long user) {
        jdbc.queryForObject(
                "SELECT id FROM public.app_users WHERE id=? FOR UPDATE", Long.class, user);
    }

    public boolean revoked(UUID token) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.auth_token_revocation WHERE token_id=?)",
                        Boolean.class,
                        token));
    }

    public void revoke(UUID token, long user, Instant expiry) {
        jdbc.update(
                "INSERT INTO public.auth_token_revocation(token_id,user_id,expires_at) VALUES (?,?,?) ON CONFLICT DO NOTHING",
                token,
                user,
                Timestamp.from(expiry));
    }

    public Optional<Long> external(String issuer, String subject) {
        return jdbc
                .query(
                        "SELECT user_id FROM public.external_identity WHERE issuer=? AND subject=?",
                        (r, n) -> r.getLong(1),
                        issuer,
                        subject)
                .stream()
                .findFirst();
    }

    public void link(long user, String issuer, String subject) {
        jdbc.update(
                "INSERT INTO public.external_identity(id,user_id,issuer,subject) VALUES (?,?,?,?)",
                UUID.randomUUID(),
                user,
                issuer,
                subject);
    }

    public boolean emailExists(String email) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.app_users WHERE lower(btrim(email))=?)",
                        Boolean.class,
                        email));
    }

    public long provision(String email, String name) {
        long user =
                jdbc.queryForObject(
                        "INSERT INTO public.app_users(email,password_hash,display_name,role,status) VALUES (?,NULL,?,'STUDENT','ACTIVE') RETURNING id",
                        Long.class,
                        email,
                        name);
        jdbc.update("INSERT INTO public.student_profiles(user_id) VALUES (?)", user);
        return user;
    }
}
