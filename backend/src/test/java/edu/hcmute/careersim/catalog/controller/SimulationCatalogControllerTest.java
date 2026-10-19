package edu.hcmute.careersim.catalog.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.hcmute.careersim.catalog.dto.SimulationSummaryResponse;
import edu.hcmute.careersim.catalog.service.SimulationCatalogService;
import edu.hcmute.careersim.common.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = SimulationCatalogController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class SimulationCatalogControllerTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private edu.hcmute.careersim.identity.service.BrowserAuthentication browserAuthentication;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private edu.hcmute.careersim.common.config.BrowserCredentialCookies browserCredentialCookies;

    @MockitoBean private edu.hcmute.careersim.identity.access.ActorAccess actors;
    @Autowired private MockMvc mockMvc;

    @MockitoBean private SimulationCatalogService simulationCatalogService;

    @Test
    void getPublishedSimulationsReturnsSharedApiEnvelope() throws Exception {
        when(simulationCatalogService.getPublishedSimulations())
                .thenReturn(
                        List.of(
                                new SimulationSummaryResponse(
                                        1L,
                                        "backend-api-triage",
                                        "Backend API Triage",
                                        "BACKEND_DEVELOPMENT",
                                        "Practice backend decision making.",
                                        "INTRODUCTORY",
                                        35)));

        mockMvc.perform(get("/v1/simulations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].slug").value("backend-api-triage"))
                .andExpect(jsonPath("$.data[0].estimatedMinutes").value(35));
    }
}
