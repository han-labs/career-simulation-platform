package edu.hcmute.careersim.guidance.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.guidance.service.SynExecutionScope;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiWebSearchProviderTest {
    @Test
    void quotaRejectionIsDistinctFromNoSearchResults() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var provider = new GeminiWebSearchProvider(builder.build(), "test-key", "test-model", true);
        server.expect(requestTo("https://provider.test/models/test-model:generateContent"))
                .andRespond(
                        org.springframework.test.web.client.response.MockRestResponseCreators
                                .withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(20), () -> true)) {
            assertThat(provider.search("devops").status()).isEqualTo("RATE_LIMITED");
        }
        server.verify();
    }

    private static final String FIXTURE =
            """
        {"candidates":[{"finishReason":"STOP","groundingMetadata":{
          "webSearchQueries":["backend documentation"],
          "searchEntryPoint":{"renderedContent":"<div>Search suggestions</div>"},
          "groundingChunks":[{"web":{"uri":"https://spring.io/guides","title":"Spring"}},
            {"web":{"uri":"http://127.0.0.1/private","title":"Unsafe"}},
            {"web":{"uri":"https://example.test/invented","title":"Unsupported"}}],
          "groundingSupports":[{"segment":{"text":"Practice creating a REST endpoint."},"groundingChunkIndices":[0,1]}]
        }}]}
        """;

    @Test
    void onlyGroundedHttpsSourcesAreReturned() throws Exception {
        var result =
                GeminiWebSearchProvider.parse(
                        new ObjectMapper().readTree(FIXTURE), "2026-09-29T00:00:00Z");
        assertThat(result.status()).isEqualTo("LIVE_WEB");
        assertThat(result.sources()).hasSize(1);
        assertThat(result.sources().get(0).url()).isEqualTo("https://spring.io/guides");
        assertThat(result.sources().get(0).excerpt()).contains("REST");
        assertThat(result.searchSuggestionsHtml()).contains("Search suggestions");
        assertThat(
                        GeminiWebSearchProvider.parse(new ObjectMapper().readTree("{}"), "now")
                                .sources())
                .isEmpty();
    }

    @Test
    void noConsentNeverContactsProviderAndProviderFailureIsExplicit() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var provider = new GeminiWebSearchProvider(builder.build(), "test-key", "test-model", true);
        assertThat(provider.search("backend").status()).isEqualTo("NO_CONSENT");
        server.verify();
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(20), () -> true)) {
            server.expect(requestTo("https://provider.test/models/test-model:generateContent"))
                    .andRespond(withServerError());
            assertThat(provider.search("backend").status()).isEqualTo("UNAVAILABLE");
            server.verify();
        }
    }

    @Test
    void publicQueryUsesGoogleSearchAndMetadata() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var provider = new GeminiWebSearchProvider(builder.build(), "test-key", "test-model", true);
        server.expect(requestTo("https://provider.test/models/test-model:generateContent"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("google_search")))
                .andRespond(withSuccess(FIXTURE, MediaType.APPLICATION_JSON));
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(20), () -> true)) {
            assertThat(provider.search("backend").sources()).hasSize(1);
        }
        server.verify();
    }
}
