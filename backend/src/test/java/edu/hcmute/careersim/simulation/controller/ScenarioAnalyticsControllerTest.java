package edu.hcmute.careersim.simulation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.hcmute.careersim.common.config.BrowserCredentialCookies;
import edu.hcmute.careersim.common.config.SecurityConfig;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import edu.hcmute.careersim.simulation.domain.AnalyticsFilter;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.ScenarioAnalyticsService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = ScenarioAnalyticsController.class,
        properties = "app.core-v2-enabled=true",
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class ScenarioAnalyticsControllerTest {
    private static final String PATH = "/v2/enterprises/11/analytics";
    @Autowired private MockMvc mvc;
    @MockitoBean private ScenarioAnalyticsService service;
    @MockitoBean private ActorAccess actors;
    @MockitoBean private BrowserAuthentication authentication;
    @MockitoBean private BrowserCredentialCookies cookies;

    @BeforeEach
    void actor() {
        when(actors.require(any()))
                .thenReturn(
                        new ActorAccess.Actor(
                                7, "fixture@example.test", "Fixture", "ENTERPRISE", 0, 0));
    }

    @Test
    void bothEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH + "/evidence")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidFiltersBeforeAccessingAnalytics() throws Exception {
        mvc.perform(
                        get(PATH)
                                .with(user("fixture"))
                                .param("from", "2026-10-12")
                                .param("to", "2026-10-11"))
                .andExpect(jsonPath("$.code").value("DOMAIN_VALIDATION"));
        mvc.perform(get(PATH + "/evidence").with(user("fixture")).param("status", "PENDING"))
                .andExpect(jsonPath("$.code").value("DOMAIN_VALIDATION"));
        mvc.perform(get(PATH).with(user("fixture")).param("to", "not-a-date"))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(service);
    }

    @Test
    void projectsOnlyMinimalEvidenceAndBindsFilters() throws Exception {
        var filter =
                AnalyticsFilter.of(
                        "4",
                        LocalDate.parse("2026-10-11"),
                        LocalDate.parse("2026-10-12"),
                        "COMPLETED");
        when(service.evidence(7, 11, filter, "90", 2))
                .thenReturn(
                        new AnalyticsEvidencePage(
                                List.of(
                                        new AnalyticsEvidence(
                                                "88",
                                                "Fixture program",
                                                2,
                                                Instant.parse("2026-10-12T11:00:00Z"))),
                                "88"));
        mvc.perform(
                        get(PATH + "/evidence")
                                .with(user("fixture"))
                                .param("scenarioId", "4")
                                .param("from", "2026-10-11")
                                .param("to", "2026-10-12")
                                .param("status", "COMPLETED")
                                .param("before", "90")
                                .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].reference").value("88"))
                .andExpect(jsonPath("$.data.items[0].programTitle").value("Fixture program"))
                .andExpect(jsonPath("$.data.items[0].versionNumber").value(2))
                .andExpect(jsonPath("$.data.items[0].completedAt").exists())
                .andExpect(jsonPath("$.data.items[0].studentId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].certificateCode").doesNotExist())
                .andExpect(jsonPath("$.data.nextBefore").value("88"));
    }
}
