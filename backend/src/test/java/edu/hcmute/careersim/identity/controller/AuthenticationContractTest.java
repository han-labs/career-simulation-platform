package edu.hcmute.careersim.identity.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import edu.hcmute.careersim.common.config.BrowserCredentialCookies;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.identity.dto.IdentityContracts.*;
import edu.hcmute.careersim.identity.service.*;
import jakarta.servlet.http.Cookie;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** All provider I/O terminates in this process. No Google account or live token is used. */
@SpringBootTest(
        properties = {
            "app.core-v2-enabled=false",
            "app.legacy-write-enabled=true",
            "app.ai.provider=disabled",
            "app.auth.google.enabled=true",
            "app.auth.google.client-id=synthetic-client",
            "app.auth.google.client-secret=synthetic-not-a-credential",
            "app.auth.google.redirect-uri=http://localhost/api/login/oauth2/code/google"
        })
@AutoConfigureMockMvc
@Testcontainers
@Import(AuthenticationContractTest.ProviderConfiguration.class)
class AuthenticationContractTest {
    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:0.8.2-pg17");

    static final String PASSWORD = "synthetic-password-only-2026";
    static final RSAKey KEY;
    static final HttpServer PROVIDER;
    static volatile String nonce;
    static volatile String subject = "synthetic-google-subject";
    static volatile String email;
    static volatile boolean verified = true;
    static volatile String audience = "synthetic-client";
    static volatile int expirationSeconds = 120;
    static volatile boolean providerUnavailable;
    static final AtomicInteger calls = new AtomicInteger();

    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("synthetic-key").generate();
            PROVIDER =
                    HttpServer.create(
                            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            PROVIDER.createContext(
                    "/jwks",
                    exchange -> {
                        byte[] body =
                                new JWKSet(KEY.toPublicJWK())
                                        .toString()
                                        .getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, body.length);
                        exchange.getResponseBody().write(body);
                        exchange.close();
                    });
            PROVIDER.createContext(
                    "/token",
                    exchange -> {
                        calls.incrementAndGet();
                        if (providerUnavailable) {
                            exchange.sendResponseHeaders(503, -1);
                            exchange.close();
                            return;
                        }
                        try {
                            var now = Instant.now();
                            var claims =
                                    new JWTClaimsSet.Builder()
                                            .issuer("https://accounts.google.com")
                                            .subject(subject)
                                            .audience(audience)
                                            .issueTime(Date.from(now.minusSeconds(300)))
                                            .expirationTime(
                                                    Date.from(now.plusSeconds(expirationSeconds)))
                                            .claim("nonce", nonce)
                                            .claim("email", email)
                                            .claim("email_verified", verified)
                                            .claim("name", "Synthetic Google Student")
                                            .build();
                            var jwt =
                                    new SignedJWT(
                                            new JWSHeader.Builder(JWSAlgorithm.RS256)
                                                    .keyID(KEY.getKeyID())
                                                    .build(),
                                            claims);
                            jwt.sign(new RSASSASigner(KEY));
                            byte[] body =
                                    new ObjectMapper()
                                            .writeValueAsBytes(
                                                    Map.of(
                                                            "access_token",
                                                            "synthetic-access-token",
                                                            "token_type",
                                                            "Bearer",
                                                            "expires_in",
                                                            120,
                                                            "scope",
                                                            "openid",
                                                            "id_token",
                                                            jwt.serialize()));
                            exchange.getResponseHeaders().set("Content-Type", "application/json");
                            exchange.sendResponseHeaders(200, body.length);
                            exchange.getResponseBody().write(body);
                        } catch (Exception e) {
                            exchange.sendResponseHeaders(500, -1);
                        } finally {
                            exchange.close();
                        }
                    });
            PROVIDER.start();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @AfterAll
    static void stopProvider() {
        PROVIDER.stop(0);
    }

    @TestConfiguration
    static class ProviderConfiguration {
        @Bean
        @Primary
        ClientRegistrationRepository stubGoogle() {
            String base = "http://localhost:" + PROVIDER.getAddress().getPort();
            return new InMemoryClientRegistrationRepository(
                    ClientRegistration.withRegistrationId("google")
                            .clientId("synthetic-client")
                            .clientSecret("synthetic-not-a-credential")
                            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                            .clientAuthenticationMethod(
                                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                            .redirectUri("http://localhost/api/login/oauth2/code/google")
                            .scope("openid", "email", "profile")
                            .authorizationUri(base + "/authorize")
                            .tokenUri(base + "/token")
                            .jwkSetUri(base + "/jwks")
                            .issuerUri("https://accounts.google.com")
                            .userNameAttributeName("sub")
                            .build());
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired IdentityService identity;
    @Autowired BrowserAuthentication auth;
    @Autowired JdbcTemplate jdbc;
    long student, other, enterprise;
    String localEmail;

    @BeforeEach
    void fixture() {
        localEmail = "auth-" + UUID.randomUUID() + "@example.test";
        student =
                Long.parseLong(
                        identity.register(
                                        new Registration(
                                                localEmail, PASSWORD, "Student A", "STUDENT", null))
                                .id());
        other =
                Long.parseLong(
                        identity.register(
                                        new Registration(
                                                "other-" + UUID.randomUUID() + "@example.test",
                                                PASSWORD,
                                                "Student B",
                                                "STUDENT",
                                                null))
                                .id());
        enterprise =
                Long.parseLong(
                        identity.register(
                                        new Registration(
                                                "enterprise-" + UUID.randomUUID() + "@example.test",
                                                PASSWORD,
                                                "Enterprise",
                                                "ENTERPRISE",
                                                "Synthetic Enterprise"))
                                .id());
        email = "google-" + UUID.randomUUID() + "@example.test";
        subject = "subject-" + UUID.randomUUID();
        nonce = null;
        verified = true;
        audience = "synthetic-client";
        expirationSeconds = 120;
        providerUnavailable = false;
    }

    Cookie credential(long user) {
        return new Cookie(BrowserCredentialCookies.NAME, auth.issue(user).value());
    }

    @Test
    void jwtRequestsKeepCsrfStableAcrossReadsAndRepeatedWrites() throws Exception {
        var cookie = credential(student);
        var bootstrap =
                mvc.perform(get("/v2/auth/csrf").cookie(cookie))
                        .andExpect(status().isOk())
                        .andReturn();
        var session = (MockHttpSession) bootstrap.getRequest().getSession(false);
        String token =
                json.readTree(bootstrap.getResponse().getContentAsString())
                        .path("data")
                        .path("token")
                        .asText();
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/v2/me").session(session).cookie(cookie)).andExpect(status().isOk());
            mvc.perform(
                            post("/v1/assessments/quizzes/WORK_ACTIVITIES/attempts")
                                    .session(session)
                                    .cookie(cookie)
                                    .header("X-CSRF-TOKEN", token))
                    .andExpect(status().isOk());
        }
        var after =
                mvc.perform(get("/v2/auth/csrf").session(session).cookie(cookie))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(
                        json.readTree(after.getResponse().getContentAsString())
                                .path("data")
                                .path("token")
                                .asText())
                .isEqualTo(token);
    }

    @Test
    void quizUsesActorRejectsSpoofAndForeignAttemptsAndGatesRoles() throws Exception {
        var a = credential(student);
        var b = credential(other);
        var started =
                mvc.perform(
                                post("/v1/assessments/quizzes/WORK_ACTIVITIES/attempts")
                                        .cookie(a)
                                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andReturn();
        String id =
                json.readTree(started.getResponse().getContentAsString())
                        .path("data")
                        .path("id")
                        .asText();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT student_id FROM assessment_attempts WHERE id=?",
                                Long.class,
                                Long.parseLong(id)))
                .isEqualTo(student);
        mvc.perform(get("/v1/assessments/quizzes/attempts/" + id + "/result").cookie(b))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/v1/assessments/quizzes/WORK_ACTIVITIES/attempts")
                                .cookie(a)
                                .header("X-Student-Id", other)
                                .with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/v1/assessments/quizzes/WORK_ACTIVITIES/questions")
                                .cookie(credential(enterprise)))
                .andExpect(status().isForbidden());
        var questions =
                mvc.perform(get("/v1/assessments/quizzes/WORK_ACTIVITIES/questions").cookie(a))
                        .andExpect(status().isOk())
                        .andReturn();
        var answers = new ArrayList<Map<String, Object>>();
        json.readTree(questions.getResponse().getContentAsString())
                .path("data")
                .forEach(q -> answers.add(Map.of("questionId", q.path("id").asLong(), "score", 3)));
        mvc.perform(
                        post("/v1/assessments/quizzes/attempts/" + id + "/submit")
                                .cookie(a)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json.writeValueAsString(Map.of("answers", answers))))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/assessments/quizzes/attempts/" + id + "/result").cookie(a))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRevokesOnlyCurrentCredentialAndLockEnforcesCurrentState() throws Exception {
        var a = credential(student);
        var second = credential(student);
        mvc.perform(post("/v2/auth/logout").cookie(a)).andExpect(status().isForbidden());
        mvc.perform(post("/v2/auth/logout").cookie(a).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(BrowserCredentialCookies.NAME, 0));
        mvc.perform(get("/v2/me").cookie(a)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v2/me").cookie(second)).andExpect(status().isOk());
        jdbc.update("UPDATE app_users SET status='LOCKED' WHERE id=?", student);
        mvc.perform(get("/v2/me").cookie(second)).andExpect(status().isForbidden());
        jdbc.update("UPDATE app_users SET status='ACTIVE' WHERE id=?", student);
        mvc.perform(get("/v2/me").cookie(second)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v2/me").cookie(new Cookie(BrowserCredentialCookies.NAME, "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordLoginIssuesSecureHttpOnlyCookieAndDoesNotAuthenticateSession() throws Exception {
        var session = new MockHttpSession();
        var result =
                mvc.perform(
                                post("/v2/auth/login")
                                        .session(session)
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        new Login(localEmail, PASSWORD))))
                        .andExpect(status().isOk())
                        .andReturn();
        var cookie = result.getResponse().getCookie(BrowserCredentialCookies.NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(cookie.getMaxAge()).isBetween(7195, 7200);
        assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
        mvc.perform(get("/v2/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v2/me").cookie(cookie)).andExpect(status().isOk());
    }

    Map<String, String> authorization(MockHttpSession session, Cookie cookie) throws Exception {
        var request = get("/oauth2/authorization/google").session(session);
        if (cookie != null) request.cookie(cookie);
        var start = mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
        String query = URI.create(start.getResponse().getRedirectedUrl()).getRawQuery();
        var values = new HashMap<String, String>();
        for (String item : query.split("&")) {
            var pair = item.split("=", 2);
            values.put(
                    URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        assertThat(values.get("state")).isNotBlank();
        assertThat(values.get("nonce")).isNotBlank();
        assertThat(values.get("code_challenge_method")).isEqualTo("S256");
        nonce = values.get("nonce");
        return values;
    }

    @Test
    void oidcCallbackValidatesStateNonceAndStableSubjectWithoutEmailMerging() throws Exception {
        var session = new MockHttpSession();
        var values = authorization(session, null);
        int before = calls.get();
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .session(session)
                                .param("state", "wrong-state")
                                .param("code", "synthetic"))
                .andExpect(redirectedUrl("/account?oauthError=denied"));
        assertThat(calls.get()).isEqualTo(before);
        values = authorization(session, null);
        var success =
                mvc.perform(
                                get("/login/oauth2/code/google")
                                        .session(session)
                                        .param("state", values.get("state"))
                                        .param("code", "synthetic"))
                        .andExpect(redirectedUrl("/account?oauth=success"))
                        .andReturn();
        var cookie = success.getResponse().getCookie(BrowserCredentialCookies.NAME);
        assertThat(cookie).isNotNull();
        long id = auth.verify(cookie.getValue()).id();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT password_hash FROM app_users WHERE id=?", String.class, id))
                .isNull();
        email = localEmail; // Changing provider email cannot transfer ownership of a linked
        // subject.
        var again =
                auth.externalIdentity(
                        "https://accounts.google.com", subject, email, "Changed", null, null);
        assertThat(again).isEqualTo(id);
        assertThatThrownBy(
                        () ->
                                auth.externalIdentity(
                                        "https://accounts.google.com",
                                        "new-subject",
                                        localEmail,
                                        "Collision",
                                        null,
                                        null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                auth.externalIdentity(
                                        "https://accounts.google.com",
                                        subject,
                                        email,
                                        "Collision",
                                        student,
                                        auth.version(student)))
                .isInstanceOf(ApiException.class);
        subject = "nonce-mismatch-" + UUID.randomUUID();
        values = authorization(session, null);
        nonce = "wrong-nonce";
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .session(session)
                                .param("state", values.get("state"))
                                .param("code", "synthetic"))
                .andExpect(redirectedUrl("/account?oauthError=denied"));
    }

    @Test
    void linkingRequiresCsrfAndSameLiveBrowserCredential() throws Exception {
        var session = new MockHttpSession();
        var cookie = credential(student);
        mvc.perform(post("/v2/auth/google/link").session(session).cookie(cookie))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v2/auth/google/link").session(session).cookie(cookie).with(csrf()))
                .andExpect(status().isOk());
        var values = authorization(session, cookie);
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .session(session)
                                .cookie(credential(other))
                                .param("state", values.get("state"))
                                .param("code", "synthetic"))
                .andExpect(redirectedUrl("/account?oauthError=denied"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM external_identity WHERE subject=?",
                                Integer.class,
                                subject))
                .isZero();
        mvc.perform(post("/v2/auth/google/link").session(session).cookie(cookie).with(csrf()))
                .andExpect(status().isOk());
        values = authorization(session, cookie);
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .session(session)
                                .cookie(cookie)
                                .param("state", values.get("state"))
                                .param("code", "synthetic"))
                .andExpect(redirectedUrl("/account?oauth=success"));
        assertThat(
                        auth.externalIdentity(
                                "https://accounts.google.com",
                                subject,
                                email,
                                "Linked",
                                null,
                                null))
                .isEqualTo(student);
    }

    @Test
    void newOidcStudentCanSubmitAndReloadCareerQuizWithIssuedCookie() throws Exception {
        var session = new MockHttpSession();
        var values = authorization(session, null);
        var callback =
                mvc.perform(
                                get("/login/oauth2/code/google")
                                        .session(session)
                                        .param("state", values.get("state"))
                                        .param("code", "synthetic"))
                        .andExpect(redirectedUrl("/account?oauth=success"))
                        .andReturn();
        var cookie = callback.getResponse().getCookie(BrowserCredentialCookies.NAME);
        assertThat(cookie).isNotNull();
        long user = auth.verify(cookie.getValue()).id();
        assertThat(user).isNotIn(student, other, enterprise);
        mvc.perform(get("/v2/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
        var csrfResponse =
                mvc.perform(get("/v2/auth/csrf").session(session).cookie(cookie))
                        .andExpect(status().isOk())
                        .andReturn();
        String token =
                json.readTree(csrfResponse.getResponse().getContentAsString())
                        .path("data")
                        .path("token")
                        .asText();
        var started =
                mvc.perform(
                                post("/v1/assessments/quizzes/WORK_ACTIVITIES/attempts")
                                        .session(session)
                                        .cookie(cookie)
                                        .header("X-CSRF-TOKEN", token))
                        .andExpect(status().isOk())
                        .andReturn();
        String id =
                json.readTree(started.getResponse().getContentAsString())
                        .path("data")
                        .path("id")
                        .asText();
        var questions =
                mvc.perform(get("/v1/assessments/quizzes/WORK_ACTIVITIES/questions").cookie(cookie))
                        .andExpect(status().isOk())
                        .andReturn();
        var answers = new ArrayList<Map<String, Object>>();
        json.readTree(questions.getResponse().getContentAsString())
                .path("data")
                .forEach(q -> answers.add(Map.of("questionId", q.path("id").asLong(), "score", 3)));
        mvc.perform(
                        post("/v1/assessments/quizzes/attempts/" + id + "/submit")
                                .session(session)
                                .cookie(cookie)
                                .header("X-CSRF-TOKEN", token)
                                .contentType("application/json")
                                .content(json.writeValueAsString(Map.of("answers", answers))))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/assessments/quizzes/attempts/" + id + "/result").cookie(cookie))
                .andExpect(status().isOk());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT student_id FROM assessment_attempts WHERE id=?",
                                Long.class,
                                Long.parseLong(id)))
                .isEqualTo(user);
    }

    @Test
    void cancelledAuthorizationAndProviderFailureDoNotCreateAccountOrCookie() throws Exception {
        var session = new MockHttpSession();
        var values = authorization(session, null);
        int before = calls.get();
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .session(session)
                                .param("state", values.get("state"))
                                .param("error", "access_denied"))
                .andExpect(redirectedUrl("/account?oauthError=cancelled"))
                .andExpect(cookie().doesNotExist(BrowserCredentialCookies.NAME));
        assertThat(calls.get()).isEqualTo(before);
        values = authorization(session, null);
        providerUnavailable = true;
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .session(session)
                                .param("state", values.get("state"))
                                .param("code", "synthetic"))
                .andExpect(redirectedUrl("/account?oauthError=provider-unavailable"))
                .andExpect(cookie().doesNotExist(BrowserCredentialCookies.NAME));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM external_identity WHERE subject=?",
                                Integer.class,
                                subject))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM app_users WHERE email=?",
                                Integer.class,
                                email))
                .isZero();
    }

    @Test
    void unverifiedEmailWrongAudienceAndExpiredIdentityNeverProvisionStudent() throws Exception {
        for (int invalid = 0; invalid < 3; invalid++) {
            verified = invalid != 0;
            audience = invalid == 1 ? "another-client" : "synthetic-client";
            expirationSeconds = invalid == 2 ? -120 : 120;
            var session = new MockHttpSession();
            var values = authorization(session, null);
            mvc.perform(
                            get("/login/oauth2/code/google")
                                    .session(session)
                                    .param("state", values.get("state"))
                                    .param("code", "synthetic"))
                    .andExpect(redirectedUrl("/account?oauthError=denied"))
                    .andExpect(cookie().doesNotExist(BrowserCredentialCookies.NAME));
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM app_users WHERE email=?",
                                Integer.class,
                                email))
                .isZero();
    }

    @Test
    void repeatOidcLoginKeepsOneAccountAndDisabledLinkedAccountIsDenied() throws Exception {
        Long user = null;
        for (int login = 0; login < 2; login++) {
            var session = new MockHttpSession();
            var values = authorization(session, null);
            var result =
                    mvc.perform(
                                    get("/login/oauth2/code/google")
                                            .session(session)
                                            .param("state", values.get("state"))
                                            .param("code", "synthetic"))
                            .andExpect(redirectedUrl("/account?oauth=success"))
                            .andReturn();
            var cookie = result.getResponse().getCookie(BrowserCredentialCookies.NAME);
            assertThat(cookie).isNotNull();
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.getSecure()).isTrue();
            assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
            long current = auth.verify(cookie.getValue()).id();
            if (user != null) assertThat(current).isEqualTo(user);
            user = current;
            mvc.perform(get("/v2/me").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.role").value("STUDENT"));
            assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM app_users WHERE email=?",
                                Integer.class,
                                email))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM student_profiles WHERE user_id=?",
                                Integer.class,
                                user))
                .isEqualTo(1);
        jdbc.update("UPDATE app_users SET status='LOCKED' WHERE id=?", user);
        var session = new MockHttpSession();
        var values = authorization(session, null);
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .session(session)
                                .param("state", values.get("state"))
                                .param("code", "synthetic"))
                .andExpect(redirectedUrl("/account?oauthError=account-disabled"))
                .andExpect(cookie().doesNotExist(BrowserCredentialCookies.NAME));
    }

    @Test
    void collidingEmailReturnsLinkRequiredWithoutCreatingDuplicate() throws Exception {
        email = localEmail;
        var session = new MockHttpSession();
        var values = authorization(session, null);
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .session(session)
                                .param("state", values.get("state"))
                                .param("code", "synthetic"))
                .andExpect(redirectedUrl("/account?oauthError=link-required"))
                .andExpect(cookie().doesNotExist(BrowserCredentialCookies.NAME));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM app_users WHERE email=?",
                                Integer.class,
                                email))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM external_identity WHERE subject=?",
                                Integer.class,
                                subject))
                .isZero();
    }

    @Test
    void linkedEnterpriseAndAdministratorKeepTheirRoleOnGoogleSignIn() throws Exception {
        long admin =
                jdbc.queryForObject(
                        "INSERT INTO app_users(email,display_name,role,status) VALUES (?, 'Synthetic Admin','ADMINISTRATOR','ACTIVE') RETURNING id",
                        Long.class,
                        "admin-google-" + UUID.randomUUID() + "@example.test");
        for (long owner : List.of(enterprise, admin)) {
            subject = "privileged-subject-" + UUID.randomUUID();
            authenticationLinkFixture(owner);
            var session = new MockHttpSession();
            var values = authorization(session, null);
            var result =
                    mvc.perform(
                                    get("/login/oauth2/code/google")
                                            .session(session)
                                            .param("state", values.get("state"))
                                            .param("code", "synthetic"))
                            .andExpect(redirectedUrl("/account?oauth=success"))
                            .andReturn();
            var cookie = result.getResponse().getCookie(BrowserCredentialCookies.NAME);
            mvc.perform(get("/v2/me").cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.data.role")
                                    .value(owner == enterprise ? "ENTERPRISE" : "ADMIN"));
            mvc.perform(get("/v1/assessments/quizzes/WORK_ACTIVITIES/questions").cookie(cookie))
                    .andExpect(status().isForbidden());
            assertThat(auth.verify(cookie.getValue()).id()).isEqualTo(owner);
        }
    }

    private void authenticationLinkFixture(long owner) {
        // Establish an existing explicit identity link; its browser/CSRF ceremony has
        // independent coverage in linkingRequiresCsrfAndSameLiveBrowserCredential.
        assertThat(
                        auth.externalIdentity(
                                "https://accounts.google.com",
                                subject,
                                email,
                                "Existing role",
                                owner,
                                auth.version(owner)))
                .isEqualTo(owner);
    }

    @Test
    void concurrentExplicitLinksCannotTransferOrMergeAnExternalIdentity() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        try {
            var jobs = new java.util.ArrayList<java.util.concurrent.Future<Long>>();
            for (long user : java.util.List.of(student, other)) {
                long version = auth.version(user);
                jobs.add(
                        pool.submit(
                                () -> {
                                    barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                                    try {
                                        return auth.externalIdentity(
                                                "https://accounts.google.com",
                                                subject,
                                                email,
                                                "Synthetic",
                                                user,
                                                version);
                                    } catch (
                                            edu.hcmute.careersim.common.exception.ApiException
                                                    failure) {
                                        assertThat(failure.getErrorCode())
                                                .isEqualTo(
                                                        edu.hcmute.careersim.common.exception
                                                                .ErrorCode.CONFLICT);
                                        return -1L;
                                    }
                                }));
            }
            long first = jobs.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS);
            long second = jobs.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(java.util.List.of(first, second).stream().filter(id -> id > 0).count())
                    .isEqualTo(1);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM external_identity WHERE issuer='https://accounts.google.com' AND subject=?",
                                    Integer.class,
                                    subject))
                    .isEqualTo(1);
            assertThat(
                            auth.externalIdentity(
                                    "https://accounts.google.com",
                                    subject,
                                    email,
                                    "Synthetic",
                                    null,
                                    null))
                    .isEqualTo(Math.max(first, second));
        } finally {
            pool.shutdownNow();
        }
    }
}
