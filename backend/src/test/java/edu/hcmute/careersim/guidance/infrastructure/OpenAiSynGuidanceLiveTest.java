package edu.hcmute.careersim.guidance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

@EnabledIfEnvironmentVariable(named = "RUN_LIVE_AI_TEST", matches = "true")
class OpenAiSynGuidanceLiveTest {

    @Test
    void configuredKeyCanReachOpenAiWithSyntheticEvidence() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = System.getenv().getOrDefault("AI_MODEL", "gpt-5.6-luna");
        var provider =
                new OpenAiSynGuidanceProvider(
                        new ObjectMapper(),
                        RestClient.builder(),
                        "openai",
                        apiKey == null ? "" : apiKey,
                        model,
                        "https://api.openai.com/v1",
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
