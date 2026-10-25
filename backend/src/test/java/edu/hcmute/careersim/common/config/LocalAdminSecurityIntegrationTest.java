package edu.hcmute.careersim.common.config;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(properties = {"app.core-v2-enabled=true", "app.agent-platform-enabled=true"})
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalAdminSecurityIntegrationTest {
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

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired edu.hcmute.careersim.identity.service.BrowserAuthentication auth;

    @BeforeEach
    void fixture() {
        jdbc.update("DELETE FROM public.student_profiles WHERE user_id=3001");
        jdbc.update("DELETE FROM public.app_users WHERE id IN (2001,2002,3001)");
        jdbc.update(
                "INSERT INTO public.app_users(id,email,password_hash,display_name,role,status) VALUES (2001,'local-admin-a@example.invalid','unused','Local A','ADMINISTRATOR','ACTIVE'),(2002,'local-admin-b@example.invalid','unused','Local B','ADMINISTRATOR','ACTIVE'),(3001,'local-student@example.invalid','unused','Student','STUDENT','ACTIVE')");
        jdbc.update("INSERT INTO public.student_profiles(user_id) VALUES (3001)");
    }

    static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder localGet(
            String path) {
        return get(path).servletPath(path);
    }

    @Test
    void directLocalReadsUseAnExistingAdminAndMutationsStillRequireCsrf() throws Exception {
        mvc.perform(localGet("/v2/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("2001"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
        mvc.perform(localGet("/v2/admin/knowledge-bases")).andExpect(status().isOk());
        mvc.perform(localGet("/v2/admin/agents")).andExpect(status().isOk());
        mvc.perform(
                        post("/v2/admin/knowledge-bases")
                                .servletPath("/v2/admin/knowledge-bases")
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void selectionIsStableAndRechecksInactiveAccounts() throws Exception {
        mvc.perform(localGet("/v2/me")).andExpect(jsonPath("$.data.id").value("2001"));
        jdbc.update("UPDATE public.app_users SET status='INACTIVE' WHERE id=2001");
        mvc.perform(localGet("/v2/me")).andExpect(jsonPath("$.data.id").value("2002"));
        jdbc.update("UPDATE public.app_users SET status='INACTIVE' WHERE id=2002");
        mvc.perform(localGet("/v2/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void realStudentJwtIsNeverElevatedAndUnrelatedEndpointsStayProtected() throws Exception {
        var credential = auth.issue(3001);
        var cookie = new jakarta.servlet.http.Cookie("CAREERSIM_AUTH", credential.value());
        mvc.perform(localGet("/v2/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
        mvc.perform(localGet("/v2/admin/knowledge-bases").cookie(cookie))
                .andExpect(status().isForbidden());
        mvc.perform(localGet("/v2/admin/assessment-types")).andExpect(status().isUnauthorized());
        mvc.perform(
                        localGet("/v2/me")
                                .with(
                                        r -> {
                                            r.setServerName("remote.example.invalid");
                                            return r;
                                        }))
                .andExpect(status().isUnauthorized());
    }
}
