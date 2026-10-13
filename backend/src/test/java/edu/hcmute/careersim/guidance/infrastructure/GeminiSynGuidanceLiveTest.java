package edu.hcmute.careersim.guidance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.domain.SynIntent;
import edu.hcmute.careersim.guidance.domain.SynToolIdentifier;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.service.SynGuidanceProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

@EnabledIfEnvironmentVariable(named = "RUN_LIVE_GEMINI_TEST", matches = "true")
class GeminiSynGuidanceLiveTest {

    @Test
    void configuredKeyCanReachGeminiWithSyntheticEvidence() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-3.1-flash-lite");
        var provider =
                new GeminiSynGuidanceProvider(
                        new ObjectMapper(),
                        RestClient.builder(),
                        "gemini",
                        apiKey == null ? "" : apiKey,
                        model,
                        "https://generativelanguage.googleapis.com/v1beta",
                        15,
                        240);

        var reply = provider.generate("What is one useful reflection question?", dashboard());

        assertThat(reply.text()).isNotBlank().hasSizeLessThanOrEqualTo(1200);
        assertThat(reply.suggestions()).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_LIVE_GEMINI_TOOL_TEST", matches = "true")
    void configuredModelCompletesBoundedToolCallingWithSyntheticEvidence() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-3.5-flash-lite");
        var provider =
                new GeminiSynGuidanceProvider(
                        new ObjectMapper(),
                        RestClient.builder(),
                        "gemini",
                        apiKey == null ? "" : apiKey,
                        model,
                        "https://generativelanguage.googleapis.com/v1beta",
                        20,
                        450);
        var context =
                new SynGuidanceProvider.AgentContext(
                        SynIntent.LEARNING_RESOURCES,
                        "EN",
                        "PRACTISING",
                        "STANDARD",
                        "v=2|lang=EN|goal=LEARNING_RESOURCES",
                        List.of("REVIEW_LATEST"),
                        List.of());

        var plan =
                provider.requestTools(
                        "Suggest a backend simulation and a reviewed learning resource.",
                        context,
                        List.of(SynToolIdentifier.values()));
        var reply =
                provider.synthesize(
                        "Suggest a backend simulation and a reviewed learning resource.",
                        dashboard(),
                        context,
                        plan,
                        new SynGuidanceProvider.ToolExecution(
                                plan.tools(),
                                List.of(),
                                List.of(
                                        "Synthetic reviewed simulations and resources are ready.")));

        assertThat(plan.tools()).isNotEmpty();
        assertThat(plan.providerContinuation()).isNotBlank();
        assertThat(reply.text()).isNotBlank().hasSizeLessThanOrEqualTo(2400);
        assertThat(reply.suggestions()).hasSizeLessThanOrEqualTo(3);
    }

    private DashboardResponse dashboard() {
        return new DashboardResponse(
                "LIVE",
                "Synthetic Student",
                new DashboardResponse.Progress(2, 3, "Two steps"),
                List.of(new DashboardResponse.InterestSignal("I", "Investigative", 18)),
                List.of(
                        new DashboardResponse.SkillSummary(
                                "SQL querying", "NEEDS_MORE_EVIDENCE", "Synthetic simulation")),
                List.of(
                        new DashboardResponse.RecentResult(
                                42L,
                                "Synthetic backend task",
                                80,
                                "Completed simulation",
                                List.of(
                                        new DashboardResponse.ResultOutcome(
                                                "SQL querying", "NEEDS_MORE_EVIDENCE")))),
                null);
    }
}
