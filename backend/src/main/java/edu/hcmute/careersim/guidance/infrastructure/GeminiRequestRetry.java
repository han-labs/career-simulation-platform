package edu.hcmute.careersim.guidance.infrastructure;

import edu.hcmute.careersim.guidance.service.SynExecutionScope;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.springframework.web.client.RestClientResponseException;

/** At most one retry, only for a temporary HTTP rejection and within the request deadline. */
final class GeminiRequestRetry {
    private GeminiRequestRetry() {}

    static <T> T run(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException failure) {
            int status = failure.getStatusCode().value();
            if (status != 429 && status != 503) throw failure;
            String header =
                    failure.getResponseHeaders() == null
                            ? null
                            : failure.getResponseHeaders().getFirst("Retry-After");
            long delay = delayMillis(header);
            if (delay > 1500
                    || SynExecutionScope.remaining(Duration.ofSeconds(20)).toMillis()
                            < delay + 3000) throw failure;
            SynExecutionScope.checkExternal();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw failure;
            }
            SynExecutionScope.checkExternal();
            return SynExecutionScope.observe("PROVIDER_RETRY", request);
        }
    }

    static long delayMillis(String header) {
        if (header == null)
            return java.util.concurrent.ThreadLocalRandom.current().nextLong(200, 401);
        try {
            return Math.max(0, Math.multiplyExact(Long.parseLong(header.trim()), 1000));
        } catch (NumberFormatException | ArithmeticException ignored) {
            try {
                return Math.max(
                        0,
                        Duration.between(
                                        Instant.now(),
                                        ZonedDateTime.parse(
                                                        header,
                                                        DateTimeFormatter.RFC_1123_DATE_TIME)
                                                .toInstant())
                                .toMillis());
            } catch (RuntimeException invalid) {
                return Long.MAX_VALUE;
            }
        }
    }
}
