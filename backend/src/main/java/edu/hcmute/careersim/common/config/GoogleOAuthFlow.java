package edu.hcmute.careersim.common.config;

import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.identity.dto.OAuthLinkIntent;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
@ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "true")
public class GoogleOAuthFlow {
    @Bean
    ClientRegistrationRepository googleClients(
            @Value("${app.auth.google.client-id}") String id,
            @Value("${app.auth.google.client-secret}") String secret,
            @Value("${app.auth.google.redirect-uri}") String callback) {
        if (id.isBlank() || secret.isBlank() || !callback.endsWith("/api/login/oauth2/code/google"))
            throw new IllegalStateException(
                    "Configure explicit Google credentials and the trusted callback URI.");
        return new InMemoryClientRegistrationRepository(
                ClientRegistration.withRegistrationId("google")
                        .clientId(id)
                        .clientSecret(secret)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri(callback)
                        .scope("openid", "profile", "email")
                        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                        .tokenUri("https://oauth2.googleapis.com/token")
                        .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                        .issuerUri("https://accounts.google.com")
                        .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                        .userNameAttributeName("sub")
                        .clientName("Google")
                        .build());
    }

    public static String hash(String value) {
        if (value == null) throw new ApiException(ErrorCode.UNAUTHORIZED);
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static AuthorizationRequestRepository<OAuth2AuthorizationRequest> requests() {
        var delegate = new HttpSessionOAuth2AuthorizationRequestRepository();
        return new AuthorizationRequestRepository<>() {
            public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest req) {
                return delegate.loadAuthorizationRequest(req);
            }

            public void saveAuthorizationRequest(
                    OAuth2AuthorizationRequest value,
                    HttpServletRequest req,
                    HttpServletResponse res) {
                if (value != null) {
                    var session = req.getSession();
                    Object intent = session.getAttribute(OAuthLinkIntent.KEY);
                    session.removeAttribute(OAuthLinkIntent.KEY);
                    if (intent instanceof OAuthLinkIntent link
                            && link.expiresAt().isAfter(Instant.now())) {
                        value =
                                OAuth2AuthorizationRequest.from(value)
                                        .attributes(a -> a.put(OAuthLinkIntent.KEY, link))
                                        .build();
                    }
                }
                delegate.saveAuthorizationRequest(value, req, res);
            }

            public OAuth2AuthorizationRequest removeAuthorizationRequest(
                    HttpServletRequest req, HttpServletResponse res) {
                var value = delegate.removeAuthorizationRequest(req, res);
                if (value != null)
                    req.setAttribute(OAuthLinkIntent.KEY, value.getAttribute(OAuthLinkIntent.KEY));
                return value;
            }
        };
    }

    static OAuth2AuthorizedClientRepository transientClients() {
        return new OAuth2AuthorizedClientRepository() {
            public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                    String id, Authentication auth, HttpServletRequest req) {
                return null;
            }

            public void saveAuthorizedClient(
                    OAuth2AuthorizedClient c,
                    Authentication auth,
                    HttpServletRequest req,
                    HttpServletResponse res) {
                /* Provider tokens are not retained. */
            }

            public void removeAuthorizedClient(
                    String id,
                    Authentication auth,
                    HttpServletRequest req,
                    HttpServletResponse res) {}
        };
    }

    static void success(
            HttpServletRequest req,
            HttpServletResponse res,
            Authentication authentication,
            BrowserAuthentication auth,
            BrowserCredentialCookies cookies,
            CsrfTokenRepository csrf)
            throws java.io.IOException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken token)
                    || !"google".equals(token.getAuthorizedClientRegistrationId())
                    || !(token.getPrincipal() instanceof OidcUser oidc)
                    || !Boolean.TRUE.equals(oidc.getEmailVerified()))
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            Long user = null, version = null;
            if (req.getAttribute(OAuthLinkIntent.KEY) instanceof OAuthLinkIntent link) {
                String cookie = cookies.read(req);
                if (!link.expiresAt().isAfter(Instant.now())
                        || !MessageDigest.isEqual(
                                link.credentialHash().getBytes(StandardCharsets.US_ASCII),
                                hash(cookie).getBytes(StandardCharsets.US_ASCII))
                        || auth.verify(cookie).id() != link.user())
                    throw new ApiException(ErrorCode.UNAUTHORIZED);
                user = link.user();
                version = link.version();
            }
            long id =
                    auth.externalIdentity(
                            oidc.getIssuer().toString(),
                            oidc.getSubject(),
                            oidc.getEmail(),
                            oidc.getFullName(),
                            user,
                            version);
            if (req.getSession(false) != null) req.changeSessionId();
            cookies.write(req, res, auth.issue(id));
            csrf.saveToken(null, req, res);
            res.sendRedirect("/account?oauth=success");
        } catch (ApiException e) {
            res.sendRedirect(
                    "/account?oauthError="
                            + (e.getErrorCode() == ErrorCode.CONFLICT
                                    ? "link-required"
                                    : "denied"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
