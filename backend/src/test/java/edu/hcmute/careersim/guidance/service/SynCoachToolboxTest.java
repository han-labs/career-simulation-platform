package edu.hcmute.careersim.guidance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.guidance.dao.SynAgentDao;
import edu.hcmute.careersim.guidance.domain.*;
import edu.hcmute.careersim.guidance.dto.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class SynCoachToolboxTest {
    private final SynAgentDao dao = mock(SynAgentDao.class);
    private final SynAgentToolbox toolbox = new SynAgentToolbox(dao, new StandardGuidancePolicy());
    private final SynAgentPolicy policy = new SynAgentPolicy();
    private final DashboardResponse dashboard =
            new DashboardResponse(
                    "LIVE",
                    "Synthetic",
                    new DashboardResponse.Progress(0, 3, "Start"),
                    List.of(),
                    List.of(),
                    List.of(),
                    null);
    private final SynAgentDao.LearningResource sql =
            new SynAgentDao.LearningResource(
                    "PostgreSQL Tutorial",
                    "PostgreSQL",
                    "https://www.postgresql.org/docs/current/tutorial.html",
                    "sql",
                    "INTRODUCTORY",
                    30);

    @Test
    void exactSkillMissFallsBackToSamePathAndProducesBilingualPlan() {
        when(dao.findResources(List.of("BACKEND_DEVELOPMENT"), List.of(), 3))
                .thenReturn(List.of(sql));
        String message = "Tôi muốn học SQL backend và kế hoạch một tuần";
        var result =
                toolbox.execute(
                        policy.decide("FREE_TEXT", message, List.of(), false),
                        message,
                        dashboard,
                        null);
        assertThat(result.resourceCards())
                .extracting(SynMessageResponse.ResourceCard::title)
                .containsExactly("PostgreSQL Tutorial");
        assertThat(result.planDraft().title()).contains("một tuần");
        assertThat(result.planDraft().steps()).anyMatch(s -> s.contains("PostgreSQL Tutorial"));
        verify(dao).findResources(List.of("BACKEND_DEVELOPMENT"), List.of("sql"), 3);
        verify(dao).findResources(List.of("BACKEND_DEVELOPMENT"), List.of(), 3);
    }

    @Test
    void exactMatchesRankFirstAndUrlsAreNotDuplicated() {
        var rest =
                new SynAgentDao.LearningResource(
                        "Spring REST",
                        "Spring",
                        "https://spring.io/guides/gs/rest-service",
                        "api_reasoning",
                        "INTRODUCTORY",
                        30);
        when(dao.findResources(List.of("BACKEND_DEVELOPMENT"), List.of("sql"), 3))
                .thenReturn(List.of(sql));
        when(dao.findResources(List.of("BACKEND_DEVELOPMENT"), List.of(), 3))
                .thenReturn(List.of(rest, sql));
        String message = "SQL backend resource";
        assertThat(
                        toolbox.execute(
                                        policy.decide("FREE_TEXT", message, List.of(), false),
                                        message,
                                        dashboard,
                                        null)
                                .resourceCards())
                .extracting(SynMessageResponse.ResourceCard::title)
                .containsExactly("PostgreSQL Tutorial", "Spring REST");
    }

    @Test
    void externalSearchUsesSafeQueryAndIsNotRepeatedDuringFallback() {
        WebSearchProvider web = mock(WebSearchProvider.class);
        toolbox.setWebSearch(web);
        when(web.search(anyString()))
                .thenReturn(
                        new WebSearchProvider.Result(
                                "LIVE_WEB",
                                "2026-09-29T00:00:00Z",
                                List.of(
                                        new WebSearchProvider.Source(
                                                "W1",
                                                "Spring",
                                                "https://spring.io/guides",
                                                "REST guide")),
                                null));
        String message = "tôi Alice đạt 67 điểm cần nguồn học backend bên ngoài";
        var decision = policy.decide("FREE_TEXT", message, List.of(), false);
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(20), () -> true)) {
            assertThat(toolbox.execute(decision, message, dashboard, null).webSearch().sources())
                    .hasSize(1);
            assertThat(toolbox.execute(decision, message, dashboard, null).webSearch().sources())
                    .hasSize(1);
        }
        verify(web, times(1))
                .search(
                        argThat(
                                q ->
                                        q.contains("backend")
                                                && !q.contains("Alice")
                                                && !q.contains("67")));
    }

    @Test
    void noConsentPreservesReviewedResourcesWithoutWebCall() {
        WebSearchProvider web = mock(WebSearchProvider.class);
        toolbox.setWebSearch(web);
        when(dao.findResources(anyList(), anyList(), eq(3))).thenReturn(List.of(sql));
        String message = "nguồn học backend bên ngoài";
        var result =
                toolbox.execute(
                        policy.decide("FREE_TEXT", message, List.of(), false),
                        message,
                        dashboard,
                        null);
        assertThat(result.webSearch().status()).isEqualTo("NO_CONSENT");
        assertThat(result.resourceCards()).hasSize(1);
        verifyNoInteractions(web);
    }
}
