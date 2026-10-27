package edu.hcmute.careersim.identity.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.common.config.BrowserCredentialCookies;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
        properties = {
            "app.core-v2-enabled=true",
            "app.agent-platform-enabled=true",
            "app.ai.provider=disabled",
            "app.knowledge.embedding-enabled=false"
        })
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Testcontainers
class NormalLocalAuthenticationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:0.8.2-pg17");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.user", PG::getUsername);
        r.add("spring.flyway.password", PG::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "ADMINISTRATOR"})
    void normalLocalIsAnonymousUntilRealLoginAndLogoutRevokesEvenWhenAdminExists(String role)
            throws Exception {
        String email = "local-auth-" + UUID.randomUUID() + "@example.test",
                password = "Synthetic-local-test-2026";
        jdbc.update(
                "INSERT INTO app_users(email,display_name,role,status) VALUES (?,'Synthetic existing Admin','ADMINISTRATOR','ACTIVE')",
                UUID.randomUUID() + "@example.test");
        jdbc.update(
                "INSERT INTO app_users(email,password_hash,display_name,role,status) VALUES (?,?,'Synthetic local login',?,'ACTIVE')",
                email,
                encoder.encode(password),
                role);
        if (role.equals("STUDENT"))
            jdbc.update(
                    "INSERT INTO student_profiles(user_id) SELECT id FROM app_users WHERE email=?",
                    email);
        mvc.perform(get("/v2/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.localWorkspace").value(false));
        mvc.perform(get("/v2/me")).andExpect(status().isUnauthorized());
        for (String path :
                new String[] {
                    "/v2/admin/agents", "/v2/admin/knowledge-bases", "/v2/admin/agent-role-bindings"
                }) mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for (int round = 0; round < 2; round++) {
            var bootstrap =
                    mvc.perform(get("/v2/auth/csrf")).andExpect(status().isOk()).andReturn();
            var session = (MockHttpSession) bootstrap.getRequest().getSession(false);
            String csrf =
                    json.readTree(bootstrap.getResponse().getContentAsString())
                            .path("data")
                            .path("token")
                            .asText();
            var login =
                    mvc.perform(
                                    post("/v2/auth/login")
                                            .session(session)
                                            .header("X-CSRF-TOKEN", csrf)
                                            .contentType("application/json")
                                            .content(
                                                    json.writeValueAsString(
                                                            Map.of(
                                                                    "email",
                                                                    email,
                                                                    "password",
                                                                    password))))
                            .andExpect(status().isOk())
                            .andReturn();
            Cookie credential = login.getResponse().getCookie(BrowserCredentialCookies.NAME);
            org.assertj.core.api.Assertions.assertThat(credential).isNotNull();
            for (int refresh = 0; refresh < 2; refresh++)
                mvc.perform(get("/v2/me").cookie(credential))
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.role")
                                        .value(role.equals("STUDENT") ? "STUDENT" : "ADMIN"));
            for (String path :
                    new String[] {
                        "/v2/admin/agents",
                        "/v2/admin/knowledge-bases",
                        "/v2/admin/agent-role-bindings"
                    })
                mvc.perform(get(path).cookie(credential))
                        .andExpect(
                                role.equals("STUDENT") ? status().isForbidden() : status().isOk());
            var fresh =
                    mvc.perform(get("/v2/auth/csrf").cookie(credential).session(session))
                            .andExpect(status().isOk())
                            .andReturn();
            csrf =
                    json.readTree(fresh.getResponse().getContentAsString())
                            .path("data")
                            .path("token")
                            .asText();
            mvc.perform(
                            post("/v2/auth/logout")
                                    .cookie(credential)
                                    .session(session)
                                    .header("X-CSRF-TOKEN", csrf)
                                    .contentType("application/json")
                                    .content("{}"))
                    .andExpect(status().isOk());
            mvc.perform(get("/v2/me")).andExpect(status().isUnauthorized());
            mvc.perform(get("/v2/me").cookie(credential)).andExpect(status().isUnauthorized());
            mvc.perform(get("/v2/admin/agents").cookie(credential))
                    .andExpect(status().isUnauthorized());
        }
    }
}
