package edu.hcmute.careersim.identity.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.identity.dao.BrowserAuthDao;
import edu.hcmute.careersim.identity.service.impl.BrowserAuthenticationImpl;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

class BrowserCredentialValidationTest {
    BrowserAuthDao dao = mock(BrowserAuthDao.class);
    ActorAccess actors = mock(ActorAccess.class);
    AuditWriter audit = mock(AuditWriter.class);
    byte[] key = new byte[32];
    BrowserAuthentication auth;
    JwtEncoder encoder;

    @BeforeEach
    void fixture() {
        new SecureRandom().nextBytes(key);
        auth =
                new BrowserAuthenticationImpl(
                        dao, actors, audit, Base64.getEncoder().encodeToString(key), false, 900);
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
    }

    String token(String issuer, String audience, Instant expiry) {
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                JwtClaimsSet.builder()
                                        .issuer(issuer)
                                        .audience(List.of(audience))
                                        .subject("7")
                                        .id(UUID.randomUUID().toString())
                                        .issuedAt(Instant.now().minusSeconds(300))
                                        .expiresAt(expiry)
                                        .claim("authVersion", 0)
                                        .build()))
                .getTokenValue();
    }

    @Test
    void rejectsExpiredWrongIssuerWrongAudienceAndTamperedTokensBeforeDataAccess() {
        for (String value :
                List.of(
                        token("careersim", "careersim-browser", Instant.now().minusSeconds(1)),
                        token("untrusted", "careersim-browser", Instant.now().plusSeconds(60)),
                        token("careersim", "another-browser", Instant.now().plusSeconds(60)),
                        "invalid.jwt.signature"))
            assertThatThrownBy(() -> auth.verify(value))
                    .isInstanceOf(ApiException.class)
                    .satisfies(
                            e ->
                                    assertThat(
                                                    ((ApiException) e)
                                                            .getErrorCode()
                                                            .getStatus()
                                                            .value())
                                            .isEqualTo(401));
        verifyNoInteractions(dao, actors, audit);
    }

    @Test
    void authenticationVersionInvalidatesAnOtherwiseValidSignature() {
        when(actors.requireId(7))
                .thenReturn(
                        new ActorAccess.Actor(
                                7, "fixture@example.test", "Fixture", "STUDENT", 0, 0));
        when(dao.version(7)).thenReturn(1L);
        assertThatThrownBy(
                        () ->
                                auth.verify(
                                        token(
                                                "careersim",
                                                "careersim-browser",
                                                Instant.now().plusSeconds(60))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void missingOrShortSigningKeyCannotEnableProductionAuthentication() {
        assertThatThrownBy(() -> new BrowserAuthenticationImpl(dao, actors, audit, "", false, 900))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                new BrowserAuthenticationImpl(
                                        dao,
                                        actors,
                                        audit,
                                        Base64.getEncoder().encodeToString(new byte[8]),
                                        false,
                                        900))
                .isInstanceOf(IllegalStateException.class);
    }
}
