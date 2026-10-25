package edu.hcmute.careersim.assessment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.hcmute.careersim.assessment.service.*;
import edu.hcmute.careersim.common.config.SecurityConfig;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = VersionedAssessmentController.class,
        properties = "app.core-v2-enabled=true",
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class VersionedAssessmentControllerTest {
    @MockitoBean
    private edu.hcmute.careersim.identity.service.BrowserAuthentication browserAuthentication;

    @MockitoBean
    private edu.hcmute.careersim.common.config.BrowserCredentialCookies browserCredentialCookies;

    @MockitoBean private ActorAccess actors;
    @MockitoBean private VersionedAssessmentService service;
    @MockitoBean private AssessmentAuthoringService authoring;
    @Autowired private MockMvc mvc;

    @Test
    void historyRequiresAuthentication() throws Exception {
        mvc.perform(get("/v2/assessment-attempts")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void historyUsesAuthenticatedActorRatherThanAnOwnerQueryParameter() throws Exception {
        when(actors.student(any()))
                .thenReturn(new ActorAccess.Actor(7, "s@example.test", "Student", "STUDENT", 0, 0));
        when(service.history(7)).thenReturn(List.of());
        mvc.perform(get("/v2/assessment-attempts").param("owner", "8").with(user("s")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
        verify(service).history(7);
        verify(service, never()).history(8);
    }

    @Test
    void wrongRoleIsDeniedBeforePrivateHistoryIsRead() throws Exception {
        when(actors.student(any())).thenThrow(new ApiException(ErrorCode.FORBIDDEN));
        mvc.perform(get("/v2/assessment-attempts").with(user("enterprise")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void foreignResultStaysNotFound() throws Exception {
        when(actors.student(any()))
                .thenReturn(new ActorAccess.Actor(7, "s@example.test", "Student", "STUDENT", 0, 0));
        when(service.result(7, 99))
                .thenThrow(new NotFoundException("Assessment attempt not found."));
        mvc.perform(get("/v2/assessment-attempts/99/result").with(user("s")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
