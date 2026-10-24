package edu.hcmute.careersim.common.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.identity.dao.BrowserAuthDao;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import edu.hcmute.careersim.identity.service.impl.BrowserAuthenticationImpl;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class AuthenticationStartupConfigurationTest {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(BrowserAuthenticationImpl.class, GoogleOAuthFlow.class)
                    .withBean(BrowserAuthDao.class, () -> mock(BrowserAuthDao.class))
                    .withBean(ActorAccess.class, () -> mock(ActorAccess.class))
                    .withBean(AuditWriter.class, () -> mock(AuditWriter.class))
                    .withPropertyValues(
                            "app.auth.jwt.allow-ephemeral-key=false",
                            "app.auth.google.enabled=true",
                            "app.auth.google.client-id=synthetic-client",
                            "app.auth.google.client-secret=synthetic-not-a-credential",
                            "app.auth.google.redirect-uri=http://localhost:8080/api/login/oauth2/code/google");

    @Test
    void googleCredentialsCannotReplaceMissingJwtSigningKey() {
        runner.withPropertyValues("app.auth.jwt.signing-key=")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasRootCauseMessage(
                                            "Set JWT_SIGNING_KEY to at least 32 random bytes (base64).");
                        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-base64!", "c2hvcnQ="})
    void invalidOrShortJwtKeyFailsClosed(String key) {
        runner.withPropertyValues("app.auth.jwt.signing-key=" + key)
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasRootCauseInstanceOf(IllegalStateException.class);
                        });
    }

    @Test
    void stableKeyAndExplicitGoogleConfigurationStartWithoutProviderIo() {
        configuredJwt()
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(BrowserAuthentication.class);
                            assertThat(context).hasSingleBean(ClientRegistrationRepository.class);
                            var google =
                                    context.getBean(ClientRegistrationRepository.class)
                                            .findByRegistrationId("google");
                            assertThat(google.getRedirectUri())
                                    .isEqualTo(
                                            "http://localhost:8080/api/login/oauth2/code/google");
                        });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "app.auth.google.client-id=",
                "app.auth.google.client-secret=",
                "app.auth.google.redirect-uri=http://localhost:8080/account"
            })
    void enabledGoogleWithIncompleteConfigurationFailsClosed(String invalidProperty) {
        configuredJwt()
                .withPropertyValues(invalidProperty)
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasRootCauseMessage(
                                            "Configure explicit Google credentials and the trusted callback URI.");
                        });
    }

    private ApplicationContextRunner configuredJwt() {
        // Synthetic fixture only. Runtime keys are generated privately by local setup.
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        return runner.withPropertyValues("app.auth.jwt.signing-key=" + key);
    }
}
