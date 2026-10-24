package edu.hcmute.careersim.simulation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.hcmute.careersim.common.config.SecurityConfig;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.service.ScenarioAuthoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = ScenarioAuthoringController.class,
        properties = "app.core-v2-enabled=true",
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class ScenarioAuthoringControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ScenarioAuthoringService service;
    @MockitoBean private ActorAccess actors;
    @MockitoBean
    private edu.hcmute.careersim.identity.service.BrowserAuthentication browserAuthentication;
    @MockitoBean
    private edu.hcmute.careersim.common.config.BrowserCredentialCookies browserCredentialCookies;

    @BeforeEach
    void actor() {
        when(actors.require(any()))
                .thenReturn(
                        new ActorAccess.Actor(
                                7, "owner@example.test", "Owner", "ENTERPRISE", 0, 0));
    }

    @Test
    void classificationRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/v2/enterprise/scenarios/11/classification"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void classificationUpdateRequiresIfMatch() throws Exception {
        mockMvc.perform(
                        put("/v2/enterprise/scenarios/11/classification")
                                .with(user("owner@example.test"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"industryCode\":\"IT\"}"))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    }

    @Test
    void staleIfMatchReturnsConflict() throws Exception {
        when(service.classify(eq(7L), eq(11L), eq(3L), eq("IT")))
                .thenThrow(new ApiException(ErrorCode.CONFLICT));

        mockMvc.perform(
                        put("/v2/enterprise/scenarios/11/classification")
                                .with(user("owner@example.test"))
                                .with(csrf())
                                .header("If-Match", "3")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"industryCode\":\"IT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void blankClassificationIsRejected() throws Exception {
        mockMvc.perform(
                        put("/v2/enterprise/scenarios/11/classification")
                                .with(user("owner@example.test"))
                                .with(csrf())
                                .header("If-Match", "3")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"industryCode\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
