package edu.hcmute.careersim.guidance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

@EnabledIfEnvironmentVariable(named = "RUN_LIVE_GEMINI_TEST", matches = "true")
class GeminiSynGuidanceLiveTest {

    @Test
    void configuredKeyCanReachGeminiWithSyntheticEvidence() {
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
                        15,
                        240);

        var reply = provider.generate("What is one useful reflection question?", dashboard());

        assertThat(reply.text()).isNotBlank().hasSizeLessThanOrEqualTo(1200);
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
