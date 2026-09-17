package edu.hcmute.careersim.guidance.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.hcmute.careersim.common.config.SecurityConfig;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.dto.PlanResponse;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import edu.hcmute.careersim.guidance.service.GuidanceService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = GuidanceController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class GuidanceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GuidanceService guidanceService;

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/guidance/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(
                        header().string(
                                        "Content-Security-Policy",
                                        "default-src 'none'; frame-ancestors 'none'"));
    }

    @Test
    void dashboardUsesAuthenticatedPrincipalAndSharedEnvelope() throws Exception {
        when(guidanceService.getDashboard("student@example.test"))
                .thenReturn(
                        new DashboardResponse(
                                "LIVE",
                                "Student",
                                new DashboardResponse.Progress(0, 3, "Start your exploration"),
                                List.of(),
                                List.of(),
                                List.of(),
                                null));

        mockMvc.perform(
                        get("/v1/guidance/dashboard")
                                .with(user("student@example.test").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.source").value("LIVE"))
                .andExpect(jsonPath("$.data.progress.total").value(3));
    }

    @Test
    void synMessageRejectsBlankInput() throws Exception {
        mockMvc.perform(
                        post("/v1/guidance/syn/messages")
                                .with(user("student@example.test").roles("STUDENT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"message\":\"   \",\"actionType\":\"FREE_TEXT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("message"));
    }

    @Test
    void synMessageReturnsStructuredSourceAndResult() throws Exception {
        var result =
                new DashboardResponse.RecentResult(
                        42L, "Backend API Triage", 80, "Completed simulation", List.of());
        when(guidanceService.sendMessage(eq("student@example.test"), any()))
                .thenReturn(
                        new SynMessageResponse(
                                "reply-1",
                                "Objective evidence is unchanged.",
                                "STANDARD",
                                result,
                                List.of("Draft a plan"),
                                null));

        mockMvc.perform(
                        post("/v1/guidance/syn/messages")
                                .with(user("student@example.test").roles("STUDENT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"message":"Review my result","actionType":"REVIEW_LATEST","context":{"attemptId":42}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provenance").value("STANDARD"))
                .andExpect(jsonPath("$.data.resultCard.score").value(80));
    }

    @Test
    void planRequiresAtLeastOneBoundedStep() throws Exception {
        mockMvc.perform(
                        post("/v1/guidance/plans")
                                .with(user("student@example.test").roles("STUDENT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"My plan\",\"steps\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void confirmedPlanReturnsSavedState() throws Exception {
        when(guidanceService.savePlan(eq("student@example.test"), any()))
                .thenReturn(
                        new PlanResponse(
                                "Explore backend work",
                                List.of("Complete one simulation"),
                                "ACTIVE",
                                true));

        mockMvc.perform(
                        post("/v1/guidance/plans")
                                .with(user("student@example.test").roles("STUDENT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"title":"Explore backend work","steps":["Complete one simulation"]}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Exploration plan saved."))
                .andExpect(jsonPath("$.data.saved").value(true));
    }

    @Test
    void studentCanClearOnlyTheSessionHandledByTheService() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(guidanceService.clearSynSession("student@example.test", sessionId)).thenReturn(true);

        mockMvc.perform(
                        delete("/v1/guidance/syn/sessions/{sessionId}", sessionId)
                                .with(user("student@example.test").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}
