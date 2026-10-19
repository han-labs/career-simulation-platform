package edu.hcmute.careersim.identity.service.impl;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.dao.BrowserAuthDao;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrowserAuthenticationImpl implements BrowserAuthentication {
    private static final String ISSUER = "careersim";
    private static final String AUDIENCE = "careersim-browser";
    private final BrowserAuthDao dao;
    private final ActorAccess actors;
    private final AuditWriter audit;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long lifetime;

    public BrowserAuthenticationImpl(
            BrowserAuthDao dao,
            ActorAccess actors,
            AuditWriter audit,
            @Value("${app.auth.jwt.signing-key:}") String encodedKey,
            @Value("${app.auth.jwt.allow-ephemeral-key:false}") boolean ephemeral,
            @Value("${app.auth.jwt.lifetime-seconds:900}") long lifetime) {
        this.dao = dao;
        this.actors = actors;
        this.audit = audit;
        this.lifetime = lifetime;
        if (lifetime < 60 || lifetime > 1800)
            throw new IllegalStateException("JWT lifetime must be 60–1800 seconds.");
        byte[] key;
        if (encodedKey.isBlank() && ephemeral) {
            key = new byte[32];
            new SecureRandom().nextBytes(key);
        } else {
            try {
                key = Base64.getDecoder().decode(encodedKey);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("JWT_SIGNING_KEY must be base64.");
            }
            if (key.length < 32)
                throw new IllegalStateException(
                        "Set JWT_SIGNING_KEY to at least 32 random bytes (base64).");
        }
        var secret = new SecretKeySpec(key, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret));
        var jwtDecoder =
                NimbusJwtDecoder.withSecretKey(secret).macAlgorithm(MacAlgorithm.HS256).build();
        jwtDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(Duration.ZERO),
                        new JwtIssuerValidator(ISSUER),
                        jwt ->
                                jwt.getAudience().contains(AUDIENCE)
                                        ? OAuth2TokenValidatorResult.success()
                                        : OAuth2TokenValidatorResult.failure(
                                                new OAuth2Error("invalid_token"))));
        decoder = jwtDecoder;
    }

    @Transactional
    public Credential issue(long user) {
        dao.lock(user);
        actors.requireId(user);
        var now = Instant.now();
        var expiry = now.plusSeconds(lifetime);
        var claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .audience(List.of(AUDIENCE))
                        .subject(Long.toString(user))
                        .id(UUID.randomUUID().toString())
                        .issuedAt(now)
                        .notBefore(now)
                        .expiresAt(expiry)
                        .claim("authVersion", dao.version(user))
                        .build();
        return new Credential(
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue(),
                expiry);
    }

    private Jwt decode(String token) {
        try {
            var jwt = decoder.decode(token);
            if (jwt.getExpiresAt() == null
                    || jwt.getIssuedAt() == null
                    || jwt.getExpiresAt().isAfter(jwt.getIssuedAt().plusSeconds(1800))
                    || !(jwt.getClaim("authVersion") instanceof Number version)
                    || version.longValue() < 0) throw new IllegalArgumentException();
            UUID.fromString(jwt.getId());
            Long.parseLong(jwt.getSubject());
            return jwt;
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid or expired sign-in.");
        }
    }

    @Transactional(readOnly = true)
    public AccountPrincipal verify(String token) {
        var jwt = decode(token);
        long id = Long.parseLong(jwt.getSubject());
        var actor =
                actors.requireId(id); // Locked accounts remain 403, including before token-version
        // validation.
        if (dao.revoked(UUID.fromString(jwt.getId()))
                || dao.version(id) != ((Number) jwt.getClaim("authVersion")).longValue())
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        return new AccountPrincipal(id, actor.email());
    }

    @Transactional
    public void revoke(String token) {
        try {
            var jwt = decode(token);
            long id = Long.parseLong(jwt.getSubject());
            dao.revoke(UUID.fromString(jwt.getId()), id, jwt.getExpiresAt());
            audit.record(
                    id, "BROWSER_LOGOUT", "app_users", id, Map.of("reason", "CURRENT_CREDENTIAL"));
        } catch (ApiException e) {
            if (e.getErrorCode() != ErrorCode.UNAUTHORIZED) throw e;
        }
    }

    @Transactional(readOnly = true)
    public long version(long user) {
        actors.requireId(user);
        return dao.version(user);
    }

    @Transactional
    public long externalIdentity(
            String issuer,
            String subject,
            String email,
            String name,
            Long linkUser,
            Long linkVersion) {
        if (!"https://accounts.google.com".equals(issuer)
                || subject == null
                || subject.isBlank()
                || subject.length() > 255) throw new ApiException(ErrorCode.UNAUTHORIZED);
        if (linkUser != null) {
            dao.lock(linkUser);
            actors.requireId(linkUser);
            if (!Objects.equals(linkVersion, dao.version(linkUser)))
                throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        var existing = dao.external(issuer, subject);
        if (existing.isPresent()) {
            if (linkUser != null && !existing.get().equals(linkUser))
                throw new ApiException(
                        ErrorCode.CONFLICT, "This Google identity is already linked.");
            actors.requireId(existing.get());
            return existing.get();
        }
        try {
            long user;
            if (linkUser != null) user = linkUser;
            else {
                if (email == null || email.length() > 255 || !email.contains("@"))
                    throw new ApiException(ErrorCode.UNAUTHORIZED);
                email = email.strip().toLowerCase(Locale.ROOT);
                if (dao.emailExists(email))
                    throw new ApiException(
                            ErrorCode.CONFLICT,
                            "Sign in to your existing account and explicitly link Google.");
                user =
                        dao.provision(
                                email,
                                name == null || name.isBlank()
                                        ? "Student"
                                        : name.substring(0, Math.min(name.length(), 150)));
            }
            dao.link(user, issuer, subject);
            audit.record(
                    user,
                    "EXTERNAL_IDENTITY_LINKED",
                    "app_users",
                    user,
                    Map.of(
                            "reason",
                            linkUser == null ? "VERIFIED_GOOGLE_REGISTRATION" : "EXPLICIT_LINK"));
            return user;
        } catch (DuplicateKeyException e) {
            throw new ApiException(
                    ErrorCode.CONFLICT, "Identity linking conflicts with an existing account.");
        }
    }
}
