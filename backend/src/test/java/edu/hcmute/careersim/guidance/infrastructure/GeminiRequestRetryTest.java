package edu.hcmute.careersim.guidance.infrastructure;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.guidance.service.SynExecutionScope;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

class GeminiRequestRetryTest {
    private RestClientResponseException failure(int code, String retryAfter) {
        var headers = new HttpHeaders();
        headers.add("Retry-After", retryAfter);
        return new RestClientResponseException("synthetic", code, "synthetic", headers, null, null);
    }

    @Test
    void retriesTemporaryRejectionOnlyOnce() {
        for (int code : new int[] {429, 503}) {
            AtomicInteger calls = new AtomicInteger();
            assertThat(
                            GeminiRequestRetry.run(
                                    () -> {
                                        if (calls.incrementAndGet() == 1) throw failure(code, "0");
                                        return "ready";
                                    }))
                    .isEqualTo("ready");
            assertThat(calls).hasValue(2);
            calls.set(0);
            assertThatThrownBy(
                            () ->
                                    GeminiRequestRetry.run(
                                            () -> {
                                                calls.incrementAndGet();
                                                throw failure(code, "0");
                                            }))
                    .isInstanceOf(RestClientResponseException.class);
            assertThat(calls).hasValue(2);
        }
    }

    @Test
    void noRetryForPermanentErrorLongRetryAfterOrInsufficientBudget() {
        for (int code : new int[] {400, 401, 403}) {
            AtomicInteger calls = new AtomicInteger();
            assertThatThrownBy(
                            () ->
                                    GeminiRequestRetry.run(
                                            () -> {
                                                calls.incrementAndGet();
                                                throw failure(code, "0");
                                            }))
                    .isInstanceOf(RestClientResponseException.class);
            assertThat(calls).hasValue(1);
        }
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(
                        () ->
                                GeminiRequestRetry.run(
                                        () -> {
                                            calls.incrementAndGet();
                                            throw failure(429, "60");
                                        }))
                .isInstanceOf(RestClientResponseException.class);
        assertThat(calls).hasValue(1);
        calls.set(0);
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(3), () -> true)) {
            assertThatThrownBy(
                            () ->
                                    GeminiRequestRetry.run(
                                            () -> {
                                                calls.incrementAndGet();
                                                throw failure(503, "0");
                                            }))
                    .isInstanceOf(RestClientResponseException.class);
            assertThat(calls).hasValue(1);
        }
    }
}
