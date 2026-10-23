package edu.hcmute.careersim.identity.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.assessment.domain.RiasecV1Evaluator;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.assessment.service.AssessmentAuthoringService;
import edu.hcmute.careersim.common.config.BrowserCredentialCookies;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Real PostgreSQL, security filters, cookies and CSRF; no demo actor or live provider. */
@SpringBootTest(
        properties = {
            "app.core-v2-enabled=true",
            "app.legacy-write-enabled=false",
            "app.ai.provider=disabled"
        })
@AutoConfigureMockMvc
@Testcontainers
class RegistrationAssessmentIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:0.8.2-pg17");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired AssessmentAuthoringService authoring;
    @Autowired BrowserAuthentication authentication;
    static final String PASSWORD = "synthetic-registration-2026";

    class Browser {
        MockHttpSession session;
        Cookie credential;
        String csrf;

        void bootstrap() throws Exception {
            var request = get("/v2/auth/csrf");
            if (session != null) request.session(session);
            if (credential != null) request.cookie(credential);
            var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
            session = (MockHttpSession) result.getRequest().getSession(false);
            csrf = data(result).path("token").asText();
        }

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder write(
                String url, Object body) throws Exception {
            var request =
                    post(url)
                            .session(session)
                            .header("X-CSRF-TOKEN", csrf)
                            .contentType("application/json")
                            .content(json.writeValueAsString(body));
            if (credential != null) request.cookie(credential);
            return request;
        }

        String registerAndLogin() throws Exception {
            bootstrap();
            String email = "registration-" + UUID.randomUUID() + "@example.test";
            mvc.perform(write("/v2/auth/register", registration(email)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.role").value("STUDENT"));
            var result =
                    mvc.perform(
                                    write(
                                            "/v2/auth/login",
                                            Map.of("email", email, "password", PASSWORD)))
                            .andExpect(status().isOk())
                            .andReturn();
            credential = result.getResponse().getCookie(BrowserCredentialCookies.NAME);
            assertThat(credential).isNotNull();
            assertThat(credential.isHttpOnly()).isTrue();
            assertThat(credential.getSecure()).isTrue();
            assertThat(credential.getAttribute("SameSite")).isEqualTo("Lax");
            bootstrap(); // Login rotated the CSRF token.
            return data(result).path("id").asText();
        }
    }

    Map<String, Object> registration(String email) {
        return Map.of(
                "email",
                email,
                "password",
                PASSWORD,
                "displayName",
                "Synthetic Student",
                "role",
                "STUDENT");
    }

    JsonNode data(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString()).path("data");
    }

    String publishedVersion() {
        long admin =
                jdbc.queryForObject(
                        "INSERT INTO app_users(email,display_name,role,status) VALUES (?, 'Synthetic Admin','ADMINISTRATOR','ACTIVE') RETURNING id",
                        Long.class,
                        "admin-" + UUID.randomUUID() + "@example.test");
        var config =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "dimensions",
                                RiasecV1Evaluator.DIMENSIONS,
                                "aggregation",
                                "SUM",
                                "minAnswer",
                                1,
                                "maxAnswer",
                                5,
                                "tieOrder",
                                RiasecV1Evaluator.DIMENSIONS));
        var questions = new ArrayList<QuestionDraft>();
        for (String dimension : RiasecV1Evaluator.DIMENSIONS)
            questions.add(
                    new QuestionDraft(
                            questions.size() + 1,
                            "Synthetic interest " + dimension,
                            "LIKERT",
                            CoreValues.tree(
                                    Map.of(
                                            "schemaVersion",
                                            1,
                                            "min",
                                            1,
                                            "max",
                                            5,
                                            "dimension",
                                            dimension)),
                            CoreValues.tree(Map.of()),
                            true));
        String type =
                authoring.createType(
                        admin,
                        new TypeRequest(
                                "REGISTRATION_" + UUID.randomUUID(), "Synthetic assessment"));
        var version =
                authoring.createVersion(
                        admin,
                        Long.parseLong(type),
                        new VersionDraft("riasec-sum", "1", config, questions));
        return authoring.publish(admin, Long.parseLong(version.id()), version.revision()).id();
    }

    @Test
    void verifiedGoogleIdentityUsesCoreAssessmentAndReloadsAfterSubjectLogin() throws Exception {
        // Service boundary fixture: full signed OIDC/state verification is covered separately
        // by AuthenticationContractTest. No live Google acceptance is implied here.
        String subject = "core-google-" + UUID.randomUUID();
        String email = "google-" + UUID.randomUUID() + "@example.test";
        long user =
                authentication.externalIdentity(
                        "https://accounts.google.com",
                        subject,
                        email,
                        "Synthetic Google Student",
                        null,
                        null);
        var browser = new Browser();
        browser.credential =
                new Cookie(BrowserCredentialCookies.NAME, authentication.issue(user).value());
        browser.bootstrap();
        mvc.perform(get("/v2/me").cookie(browser.credential))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(Long.toString(user)))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
        String version = publishedVersion();
        var started =
                mvc.perform(
                                browser.write(
                                        "/v2/assessment-versions/" + version + "/attempts",
                                        Map.of("requestId", UUID.randomUUID())))
                        .andExpect(status().isOk())
                        .andReturn();
        var attempt = data(started);
        String id = attempt.path("id").asText();
        var responses = new ArrayList<Map<String, Object>>();
        attempt.path("questions")
                .forEach(
                        q ->
                                responses.add(
                                        Map.of(
                                                "questionId",
                                                q.path("id").asText(),
                                                "answerData",
                                                Map.of("schemaVersion", 1, "value", 3))));
        var saved =
                mvc.perform(
                                browser.write(
                                                "/v2/assessment-attempts/" + id + "/submit",
                                                Map.of("items", responses))
                                        .header("If-Match", "0"))
                        .andExpect(status().isOk())
                        .andReturn();
        mvc.perform(browser.write("/v2/auth/logout", Map.of())).andExpect(status().isOk());
        mvc.perform(get("/v2/assessment-attempts/" + id + "/result").cookie(browser.credential))
                .andExpect(status().isUnauthorized());
        long again =
                authentication.externalIdentity(
                        "https://accounts.google.com",
                        subject,
                        "changed-" + email,
                        "Renamed",
                        null,
                        null);
        assertThat(again).isEqualTo(user);
        var renewed =
                new Cookie(BrowserCredentialCookies.NAME, authentication.issue(again).value());
        var loaded =
                mvc.perform(get("/v2/assessment-attempts/" + id + "/result").cookie(renewed))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(data(loaded)).isEqualTo(data(saved));
        assertThat(data(saved).path("resultData").path("dimensions").size()).isEqualTo(6);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM assessment_result WHERE attempt_id=?",
                                Integer.class,
                                Long.parseLong(id)))
                .isEqualTo(1);
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
        mvc.perform(get("/v2/assessment-attempts/" + id + "/result"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newlyRegisteredStudentCompletesPublishedAssessmentAndReloadsOwnedResultAfterRelogin()
            throws Exception {
        var browser = new Browser();
        String student = browser.registerAndLogin();
        String version = publishedVersion();
        mvc.perform(get("/v2/assessment-types").cookie(browser.credential))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].activeVersionId").exists());
        mvc.perform(
                        post("/v2/assessment-versions/" + version + "/attempts")
                                .cookie(browser.credential)
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                Map.of("requestId", UUID.randomUUID()))))
                .andExpect(status().isForbidden());
        var started =
                mvc.perform(
                                browser.write(
                                        "/v2/assessment-versions/" + version + "/attempts",
                                        Map.of("requestId", UUID.randomUUID())))
                        .andExpect(status().isOk())
                        .andReturn();
        var attempt = data(started);
        String id = attempt.path("id").asText();
        mvc.perform(
                        browser.write(
                                        "/v2/assessment-attempts/" + id + "/submit",
                                        Map.of("items", List.of()))
                                .header("If-Match", "0"))
                .andExpect(status().isUnprocessableEntity());
        var responses = new ArrayList<Map<String, Object>>();
        attempt.path("questions")
                .forEach(
                        q ->
                                responses.add(
                                        Map.of(
                                                "questionId",
                                                q.path("id").asText(),
                                                "answerData",
                                                Map.of("schemaVersion", 1, "value", 3))));
        var submitted =
                mvc.perform(
                                browser.write(
                                                "/v2/assessment-attempts/" + id + "/submit",
                                                Map.of("items", responses))
                                        .header("If-Match", "0"))
                        .andExpect(status().isOk())
                        .andReturn();
        var dimensions = data(submitted).path("resultData").path("dimensions");
        assertThat(dimensions.size()).isEqualTo(6);
        for (int i = 0; i < 6; i++) {
            assertThat(dimensions.get(i).path("code").asText())
                    .isEqualTo(RiasecV1Evaluator.DIMENSIONS.get(i));
            assertThat(dimensions.get(i).path("raw").asInt()).isEqualTo(3);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM assessment_result WHERE attempt_id=?",
                                Integer.class,
                                Long.parseLong(id)))
                .isEqualTo(1);
        var loaded =
                mvc.perform(
                                get("/v2/assessment-attempts/" + id + "/result")
                                        .cookie(browser.credential))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(data(loaded)).isEqualTo(data(submitted));
        var other = new Browser();
        other.registerAndLogin();
        mvc.perform(get("/v2/assessment-attempts/" + id + "/result").cookie(other.credential))
                .andExpect(status().isNotFound());
        mvc.perform(get("/v2/assessment-attempts/" + id + "/result"))
                .andExpect(status().isUnauthorized());
        String email =
                jdbc.queryForObject(
                        "SELECT email FROM app_users WHERE id=?",
                        String.class,
                        Long.parseLong(student));
        var oldCookie = browser.credential;
        mvc.perform(browser.write("/v2/auth/logout", Map.of())).andExpect(status().isOk());
        mvc.perform(get("/v2/assessment-attempts/" + id + "/result").cookie(oldCookie))
                .andExpect(status().isUnauthorized());
        browser.session = null;
        browser.credential = null;
        browser.bootstrap();
        var relogin =
                mvc.perform(
                                browser.write(
                                        "/v2/auth/login",
                                        Map.of("email", email, "password", PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();
        browser.credential = relogin.getResponse().getCookie(BrowserCredentialCookies.NAME);
        var reloaded =
                mvc.perform(
                                get("/v2/assessment-attempts/" + id + "/result")
                                        .cookie(browser.credential))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(data(reloaded)).isEqualTo(data(submitted));
    }

    @Test
    void concurrentRegistrationCreatesExactlyOneAccountAndProfile() throws Exception {
        var first = new Browser();
        var second = new Browser();
        first.bootstrap();
        second.bootstrap();
        String email = "concurrent-" + UUID.randomUUID() + "@example.test";
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var jobs = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (var browser : List.of(first, second)) {
                jobs.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    return mvc.perform(
                                                    browser.write(
                                                            "/v2/auth/register",
                                                            registration(email)))
                                            .andReturn()
                                            .getResponse()
                                            .getStatus();
                                }));
            }
            start.countDown();
            assertThat(
                            List.of(
                                    jobs.get(0).get(15, java.util.concurrent.TimeUnit.SECONDS),
                                    jobs.get(1).get(15, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM app_users u JOIN student_profiles p ON p.user_id=u.id WHERE u.email=?",
                                    Integer.class,
                                    email))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void registrationRequiresCsrfValidFieldsAndUniqueEmailAndDoesNotGrantConsent()
            throws Exception {
        var browser = new Browser();
        browser.bootstrap();
        String email = "unique-" + UUID.randomUUID() + "@example.test";
        mvc.perform(
                        post("/v2/auth/register")
                                .contentType("application/json")
                                .content(json.writeValueAsString(registration(email))))
                .andExpect(status().isForbidden());
        mvc.perform(
                        browser.write(
                                "/v2/auth/register",
                                Map.of(
                                        "email",
                                        "invalid",
                                        "password",
                                        "short",
                                        "displayName",
                                        "",
                                        "role",
                                        "ADMINISTRATOR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
        var created =
                mvc.perform(browser.write("/v2/auth/register", registration(email)))
                        .andExpect(status().isOk())
                        .andReturn();
        long id = data(created).path("id").asLong();
        mvc.perform(
                        browser.write(
                                "/v2/auth/register", registration(email.toUpperCase(Locale.ROOT))))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An account with this email already exists. Please sign in."));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM app_users WHERE lower(email)=?",
                                Integer.class,
                                email))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM student_profiles WHERE user_id=?",
                                Integer.class,
                                id))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM consent_grant WHERE user_id=?",
                                Integer.class,
                                id))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT password_hash FROM app_users WHERE id=?", String.class, id))
                .startsWith("{pbkdf2@SpringSecurity_v5_8}");
        assertThat(created.getResponse().getCookie(BrowserCredentialCookies.NAME)).isNull();
        mvc.perform(get("/v2/me").session(browser.session)).andExpect(status().isUnauthorized());
    }
}
