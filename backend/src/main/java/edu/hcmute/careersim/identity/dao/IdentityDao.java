package edu.hcmute.careersim.identity.dao;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.dto.IdentityContracts.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IdentityDao {
    private final JdbcTemplate jdbc;

    public record Credential(long id, String email, String hash, String role, String status) {}

    public Optional<Credential> credential(String email) {
        return jdbc
                .query(
                        "SELECT id,email,password_hash,role,status FROM public.app_users WHERE lower(btrim(email))=?",
                        (r, n) ->
                                new Credential(
                                        r.getLong(1),
                                        r.getString(2),
                                        r.getString(3),
                                        r.getString(4),
                                        r.getString(5)),
                        email)
                .stream()
                .findFirst();
    }

    public long register(Registration r, String hash) {
        return jdbc.queryForObject(
                "INSERT INTO public.app_users(email,password_hash,display_name,role,status) VALUES (?,?,?,?,'ACTIVE') RETURNING id",
                Long.class,
                r.email().strip().toLowerCase(Locale.ROOT),
                hash,
                r.displayName().strip(),
                r.role());
    }

    public void profile(long id) {
        jdbc.update("INSERT INTO public.student_profiles(user_id) VALUES (?)", id);
    }

    public Account account(long id, Object consents) {
        return jdbc.query(
                        "SELECT u.id,u.email,u.display_name,u.role,u.status,u.row_version,CASE WHEN p.user_id IS NULL THEN NULL ELSE jsonb_build_object('schoolName',p.school_name,'educationLevel',p.education_level,'graduationYear',p.graduation_year,'major',p.major,'careerGoal',p.career_goal,'declaredInterests',p.declared_interests,'revision',p.row_version)::text END profile FROM public.app_users u LEFT JOIN public.student_profiles p ON p.user_id=u.id WHERE u.id=?",
                        (r, n) ->
                                new Account(
                                        Long.toString(r.getLong(1)),
                                        r.getString(2),
                                        r.getString(3),
                                        "ADMINISTRATOR".equals(r.getString(4))
                                                ? "ADMIN"
                                                : r.getString(4),
                                        r.getString(5),
                                        r.getLong(6),
                                        r.getString(7) == null
                                                ? null
                                                : CoreValues.json(r.getString(7)),
                                        consents),
                        id)
                .get(0);
    }

    public long profileRevision(long id) {
        return jdbc.queryForObject(
                "SELECT row_version FROM public.student_profiles WHERE user_id=? FOR UPDATE",
                Long.class,
                id);
    }

    public void updateProfile(long id, Profile p) {
        jdbc.update(
                "UPDATE public.student_profiles SET school_name=?,education_level=?,graduation_year=?,major=?,career_goal=?,declared_interests=CAST(? AS jsonb),row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE user_id=?",
                p.schoolName(),
                p.educationLevel(),
                p.graduationYear(),
                p.major(),
                p.careerGoal(),
                CoreValues.write(p.declaredInterests()),
                id);
    }

    public void status(long id, String status) {
        jdbc.update(
                "UPDATE public.app_users SET status=?,row_version=row_version+1,privacy_version=privacy_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                status,
                id);
    }

    public long lockRevision(long id) {
        return jdbc.queryForObject(
                "SELECT row_version FROM public.app_users WHERE id=? FOR UPDATE", Long.class, id);
    }

    public List<Map<String, Object>> grants(long id) {
        return jdbc.query(
                "SELECT id::text,purpose,enterprise_id::text,recipient_key,policy_version,confirmed_at,revoked_at,expires_at,(revoked_at IS NULL AND (expires_at IS NULL OR expires_at>CURRENT_TIMESTAMP)) AS effective FROM public.consent_grant WHERE user_id=? ORDER BY id DESC",
                (r, n) -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("id", r.getString(1));
                    m.put("purpose", r.getString(2));
                    m.put("enterpriseId", r.getString(3));
                    m.put("recipientKey", r.getString(4));
                    m.put("policyVersion", r.getString(5));
                    m.put("confirmedAt", r.getTimestamp(6).toInstant());
                    m.put(
                            "revokedAt",
                            r.getTimestamp(7) == null ? null : r.getTimestamp(7).toInstant());
                    m.put(
                            "expiresAt",
                            r.getTimestamp(8) == null ? null : r.getTimestamp(8).toInstant());
                    m.put("effective", r.getBoolean(9));
                    return m;
                },
                id);
    }

    public void expire(long user, String purpose, String recipient) {
        jdbc.update(
                "UPDATE public.consent_grant SET revoked_at=CURRENT_TIMESTAMP WHERE user_id=? AND purpose=? AND recipient_key=? AND revoked_at IS NULL AND expires_at<=CURRENT_TIMESTAMP",
                user,
                purpose,
                recipient);
    }

    public Optional<Long> currentGrant(long user, String purpose, String recipient) {
        return jdbc
                .query(
                        "SELECT id FROM public.consent_grant WHERE user_id=? AND purpose=? AND recipient_key=? AND revoked_at IS NULL",
                        (r, n) -> r.getLong(1),
                        user,
                        purpose,
                        recipient)
                .stream()
                .findFirst();
    }

    public long grant(long user, Grant g, Long enterprise) {
        return jdbc.queryForObject(
                "INSERT INTO public.consent_grant(user_id,purpose,enterprise_id,recipient_key,policy_version,confirmed_at,confirmation_method) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP,'EXPLICIT_UI') RETURNING id",
                Long.class,
                user,
                g.purpose(),
                enterprise,
                g.recipientKey(),
                g.policyVersion());
    }

    public Optional<String[]> grantIdentity(long user, long id) {
        return jdbc
                .query(
                        "SELECT purpose,recipient_key FROM public.consent_grant WHERE id=? AND user_id=? FOR UPDATE",
                        (r, n) -> new String[] {r.getString(1), r.getString(2)},
                        id,
                        user)
                .stream()
                .findFirst();
    }

    public boolean revoke(long user, long id) {
        return jdbc.update(
                        "UPDATE public.consent_grant SET revoked_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=? AND revoked_at IS NULL",
                        id,
                        user)
                > 0;
    }

    public void privacy(long user) {
        jdbc.update(
                "UPDATE public.app_users SET privacy_version=privacy_version+1,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                user);
    }

    public boolean enterpriseExists(long id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.enterprise WHERE id=?)",
                        Boolean.class,
                        id));
    }
}
