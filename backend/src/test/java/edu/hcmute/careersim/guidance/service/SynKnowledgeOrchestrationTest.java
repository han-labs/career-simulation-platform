package edu.hcmute.careersim.guidance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.guidance.dao.SynAgentDao;
import edu.hcmute.careersim.guidance.domain.*;
import edu.hcmute.careersim.guidance.dto.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SynKnowledgeOrchestrationTest {
    private SynAgentOrchestrator agent;
    private SynGuidanceProvider provider;
    private KnowledgeRetrievalService retrieval;
    private final UUID session = UUID.randomUUID();
    private final KnowledgeChunk source =
            new KnowledgeChunk(
                    1,
                    2,
                    "SQL fixture",
                    "Join",
                    "A SQL join combines matching rows.",
                    "EN",
                    null,
                    1);
    private final DashboardResponse dashboard =
            new DashboardResponse(
                    "LIVE",
                    "Synthetic",
                    new DashboardResponse.Progress(0, 3, "Start"),
                    List.of(new DashboardResponse.InterestSignal("I", "Investigative", 32)),
                    List.of(),
                    List.of(
                            new DashboardResponse.RecentResult(
                                    91,
                                    "Backend Developer",
                                    67,
                                    "Today",
                                    List.of(),
                                    null,
                                    "BACKEND_DEVELOPMENT")),
                    null,
                    81L);

    @BeforeEach
    void setup() {
        var dao = mock(SynAgentDao.class);
        provider = mock(SynGuidanceProvider.class);
        retrieval = mock(KnowledgeRetrievalService.class);
        when(dao.findSession(7L, session))
                .thenReturn(
                        Optional.of(new SynAgentDao.SessionMemory(session, "", null, List.of())));
        when(provider.isAvailable()).thenReturn(true);
        var policy = new StandardGuidancePolicy();
        var toolbox = new SynAgentToolbox(dao, policy);
        toolbox.setKnowledge(retrieval);
        agent =
                new SynAgentOrchestrator(
                        dao, new SynAgentPolicy(), toolbox, policy, List.of(provider));
        agent.setKnowledge(retrieval);
    }

    @Test
    void missingKnowledgeAllowsExplicitlyGeneralExplanationWithoutInventedSources() {
        when(retrieval.retrieve(anyString(), anyBoolean()))
                .thenReturn(
                        new KnowledgeRetrievalService.Retrieval(
                                "NO_RELEVANT_KNOWLEDGE", List.of()));
        when(provider.generate(any(), any(), any()))
                .thenReturn(
                        new SynGuidanceProvider.AiReply(
                                "General knowledge: SQL joins combine matching rows; no reviewed source was retrieved.",
                                List.of()));
        var run = agent.respond(7L, true, request(), dashboard, null);
        assertThat(run.response().provenance()).isEqualTo("AI");
        assertThat(run.response().text()).contains("General knowledge");
        verify(provider)
                .generate(any(), any(), argThat(c -> c.toolData().containsKey("SEARCH_KNOWLEDGE")));
    }

    @Test
    void malformedOutputGetsOneRepairWithoutRepeatingRetrieval() {
        arrangeSource();
        when(provider.generate(any(), any(), any()))
                .thenThrow(
                        new SynGuidanceProvider.ProviderException(
                                SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT))
                .thenReturn(
                        new SynGuidanceProvider.AiReply("SQL joins combine rows. [K1]", List.of()));
        var run = agent.respond(7L, true, request(), dashboard, null);
        assertThat(run.response().provenance()).isEqualTo("AI");
        verify(provider, times(2)).generate(any(), any(), any());
        verify(provider)
                .generate(any(), any(), argThat(c -> c.toolData().containsKey("RESPONSE_REPAIR")));
        verify(retrieval).retrieve(anyString(), anyBoolean());
    }

    @Test
    void fabricatedCitationIsReplacedByReviewedExtract() {
        try (var scope = SynExecutionScope.open(java.time.Duration.ofSeconds(20), () -> true)) {
            arrangeSource();
            when(provider.generate(any(), any(), any()))
                    .thenReturn(
                            new SynGuidanceProvider.AiReply(
                                    "A SQL join combines rows. [K999]", List.of()));
            var run = agent.respond(7L, true, request(), dashboard, null);
            assertThat(run.response().provenance()).isEqualTo("STANDARD");
            assertThat(run.response().text()).contains("[K1]").doesNotContain("[K999]");
            assertThat(run.response().evidenceReferences())
                    .anyMatch(ref -> "K1".equals(ref.referenceId()) && "2".equals(ref.version()));
            assertThat(SynExecutionScope.trace().get("outcome"))
                    .isEqualTo(
                            Map.of(
                                    "executionReason",
                                    "MALFORMED_OUTPUT",
                                    "responseMode",
                                    "REVIEWED_EXTRACT"));
        }
    }

    @Test
    void validCitationRetainsAiProvenanceAndTypedKnowledgeContext() {
        arrangeSource();
        when(provider.generate(any(), any(), any()))
                .thenReturn(
                        new SynGuidanceProvider.AiReply(
                                "A SQL join combines matching rows. [K1]", List.of()));
        var run = agent.respond(7L, true, request(), dashboard, null);
        assertThat(run.response().provenance()).isEqualTo("AI");
        assertThat(run.response().evidenceReferences())
                .anyMatch(ref -> "K1".equals(ref.referenceId()))
                .noneMatch(
                        ref -> "A81".equals(ref.referenceId()) || "S91".equals(ref.referenceId()));
        assertThat(run.response().simulationCards()).isEmpty();
        verify(provider)
                .generate(
                        any(),
                        any(),
                        argThat(
                                context ->
                                        context.toolData().containsKey("SEARCH_KNOWLEDGE")
                                                && "NOT_SELECTED_FOR_THIS_REQUEST"
                                                        .equals(
                                                                context.toolData()
                                                                        .get(
                                                                                "PERSONAL_EVIDENCE_SCOPE"))));
    }

    private void arrangeSource() {
        when(retrieval.retrieve(anyString(), anyBoolean()))
                .thenReturn(
                        new KnowledgeRetrievalService.Retrieval("LEXICAL_ONLY", List.of(source)));
        when(retrieval.published(1L)).thenReturn(Optional.of(source));
    }

    private SynMessageRequest request() {
        return new SynMessageRequest("What is a SQL join?", "FREE_TEXT", session, "EN", null);
    }
}
