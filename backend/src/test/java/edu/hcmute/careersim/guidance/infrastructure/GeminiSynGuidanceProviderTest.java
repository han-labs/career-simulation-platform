package edu.hcmute.careersim.guidance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.service.SynGuidanceProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiSynGuidanceProviderTest {

    private MockRestServiceServer server;
    private GeminiSynGuidanceProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("https://gemini.test/v1beta").build();
        provider =
                new GeminiSynGuidanceProvider(
                        new ObjectMapper(), restClient, "gemini", "test-key", "test-model", 240);
    }

    @Test
    void returnsValidatedStructuredGuidanceFromGenerateContentApi() {
        server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(
                        request -> {
                            String body = ((MockClientHttpRequest) request).getBodyAsString();
                            assertThat(body).contains("\"responseMimeType\":\"application/json\"");
                            assertThat(body).contains("\"responseJsonSchema\"");
                            assertThat(body).contains("\"maxOutputTokens\":240");
                            assertThat(body).contains("\"systemInstruction\"");
                            assertThat(body).doesNotContain("Mai Student");
                            assertThat(body).doesNotContain("student@example.test");
                        })
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "candidates": [{
                                    "finishReason": "STOP",
                                    "content": {
                                      "parts": [{
                                        "text": "{\\\"text\\\":\\\"Compare the evidence, then choose a small experiment.\\\",\\\"suggestions\\\":[\\\"What felt engaging?\\\"]}"
                                      }]
                                    }
                                  }]
                                }
                                """,
                                MediaType.APPLICATION_JSON));

        SynGuidanceProvider.AiReply reply =
                provider.generate("How do these results fit together?", dashboard());

        assertThat(reply.text()).contains("Compare the evidence");
        assertThat(reply.suggestions()).containsExactly("What felt engaging?");
        server.verify();
    }

    @Test
    void malformedProviderOutputIsClassifiedForFallback() {
        server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
                .andRespond(
                        withSuccess(
                                """
                                {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"not-json"}]}}]}
                                """,
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.generate("Help me reflect.", dashboard()))
                .isInstanceOf(SynGuidanceProvider.ProviderException.class)
                .extracting("kind")
                .isEqualTo(SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
    }

    @Test
    void blockedOrIncompleteResponseIsClassifiedForFallback() {
        server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
                .andRespond(
                        withSuccess(
                                """
                                {"candidates":[{"finishReason":"SAFETY","content":{"parts":[]}}]}
                                """,
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.generate("Help me reflect.", dashboard()))
                .isInstanceOf(SynGuidanceProvider.ProviderException.class)
                .extracting("kind")
                .isEqualTo(SynGuidanceProvider.FailureKind.PROVIDER_REJECTED);
    }

    @Test
    void missingKeyKeepsProviderUnavailable() {
        var disabled =
                new GeminiSynGuidanceProvider(
                        new ObjectMapper(),
                        RestClient.create("https://gemini.test/v1beta"),
                        "gemini",
                        "",
                        "test-model",
                        240);

        assertThat(disabled.isAvailable()).isFalse();
    }

    private DashboardResponse dashboard() {
        return new DashboardResponse(
                "LIVE",
                "Mai Student",
                new DashboardResponse.Progress(2, 3, "Two steps"),
                List.of(new DashboardResponse.InterestSignal("I", "Investigative", 18)),
                List.of(
                        new DashboardResponse.SkillSummary(
                                "SQL querying", "NEEDS_MORE_EVIDENCE", "Backend API Triage")),
                List.of(
                        new DashboardResponse.RecentResult(
                                42L,
                                "Backend API Triage",
                                80,
                                "Completed simulation",
                                List.of(
                                        new DashboardResponse.ResultOutcome(
                                                "SQL querying", "NEEDS_MORE_EVIDENCE")))),
                null);
    }
}
