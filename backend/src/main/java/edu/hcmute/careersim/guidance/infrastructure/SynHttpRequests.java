package edu.hcmute.careersim.guidance.infrastructure;

import edu.hcmute.careersim.guidance.service.SynExecutionScope;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.springframework.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.*;

/** Bounds the entire response, including a stalled body; cancellation releases the connection. */
public final class SynHttpRequests {
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final Semaphore IN_FLIGHT = new Semaphore(8);
    private static final HttpClient CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

    private SynHttpRequests() {}

    public static ClientHttpRequestFactory deadlineAware(int seconds) {
        return deadlineAware(seconds, null);
    }

    public static ClientHttpRequestFactory deadlineAware(int seconds, String provider) {
        Duration maximum = Duration.ofSeconds(Math.max(1, Math.min(seconds, 20)));
        return (uri, method) ->
                new ClientHttpRequest() {
                    private final HttpHeaders headers = new HttpHeaders();
                    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    private final java.util.Map<String, Object> attributes =
                            new java.util.HashMap<>();

                    public java.util.Map<String, Object> getAttributes() {
                        return attributes;
                    }

                    public HttpHeaders getHeaders() {
                        return headers;
                    }

                    public OutputStream getBody() {
                        return buffer;
                    }

                    @Override
                    public HttpMethod getMethod() {
                        return method;
                    }

                    @Override
                    public URI getURI() {
                        return uri;
                    }

                    @Override
                    public ClientHttpResponse execute() throws IOException {
                        byte[] body = buffer.toByteArray();
                        SynExecutionScope.checkExternal();
                        if (provider != null) SynExecutionScope.checkProvider(provider);
                        Duration timeout = SynExecutionScope.remaining(maximum);
                        if (!IN_FLIGHT.tryAcquire())
                            throw new IOException("Provider concurrency limit reached");
                        CompletableFuture<HttpResponse<byte[]>> pending = null;
                        try {
                            var builder =
                                    HttpRequest.newBuilder(uri)
                                            .timeout(timeout)
                                            .method(
                                                    method.name(),
                                                    HttpRequest.BodyPublishers.ofByteArray(body));
                            headers.forEach(
                                    (name, values) -> {
                                        if (!name.equalsIgnoreCase("Content-Length")
                                                && !name.equalsIgnoreCase("Host"))
                                            values.forEach(value -> builder.header(name, value));
                                    });
                            pending =
                                    SynExecutionScope.admit(
                                            provider,
                                            () ->
                                                    CLIENT.sendAsync(
                                                            builder.build(),
                                                            info -> limitedBody()));
                            var response = pending.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
                            var responseHeaders = new HttpHeaders();
                            response.headers().map().forEach(responseHeaders::put);
                            return new ClientHttpResponse() {
                                public HttpStatusCode getStatusCode() {
                                    return HttpStatusCode.valueOf(response.statusCode());
                                }

                                public String getStatusText() {
                                    return Integer.toString(response.statusCode());
                                }

                                public HttpHeaders getHeaders() {
                                    return responseHeaders;
                                }

                                public InputStream getBody() {
                                    return new ByteArrayInputStream(response.body());
                                }

                                public void close() {}
                            };
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Provider request interrupted", e);
                        } catch (ExecutionException | TimeoutException e) {
                            throw new IOException("Provider response unavailable within budget", e);
                        } finally {
                            if (pending != null && !pending.isDone()) pending.cancel(true);
                            IN_FLIGHT.release();
                        }
                    }
                };
    }

    private static HttpResponse.BodySubscriber<byte[]> limitedBody() {
        var delegate = HttpResponse.BodySubscribers.ofByteArray();
        return new HttpResponse.BodySubscriber<>() {
            private Flow.Subscription subscription;
            private int received;
            private boolean failed;

            public CompletionStage<byte[]> getBody() {
                return delegate.getBody();
            }

            public void onSubscribe(Flow.Subscription value) {
                subscription = value;
                delegate.onSubscribe(value);
            }

            public void onNext(List<ByteBuffer> buffers) {
                if (failed) return;
                for (var buffer : buffers) {
                    received += buffer.remaining();
                    if (received > MAX_RESPONSE_BYTES) {
                        failed = true;
                        subscription.cancel();
                        delegate.onError(new IOException("Provider response too large"));
                        return;
                    }
                }
                delegate.onNext(buffers);
            }

            public void onError(Throwable error) {
                if (!failed) delegate.onError(error);
            }

            public void onComplete() {
                if (!failed) delegate.onComplete();
            }
        };
    }
}
