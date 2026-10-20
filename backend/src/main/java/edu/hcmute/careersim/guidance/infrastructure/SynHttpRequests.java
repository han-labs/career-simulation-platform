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

    /**
     * Incremental bounded SSE transport, sharing Syn admission, cancellation and connection limits.
     */
    public static void stream(
            URI uri,
            String provider,
            java.util.Map<String, String> headers,
            byte[] body,
            java.util.function.Consumer<String> data) {
        SynExecutionScope.checkProvider(provider);
        if (!IN_FLIGHT.tryAcquire())
            throw new IllegalStateException("Provider concurrency limit reached");
        CompletableFuture<HttpResponse<Flow.Publisher<List<ByteBuffer>>>> pending = null;
        final var subscription =
                new java.util.concurrent.atomic.AtomicReference<Flow.Subscription>();
        try {
            Duration timeout = SynExecutionScope.remaining(Duration.ofSeconds(60));
            var request =
                    HttpRequest.newBuilder(uri)
                            .timeout(timeout)
                            .header("Content-Type", "application/json")
                            .header("Accept", "text/event-stream");
            headers.forEach(request::header);
            pending =
                    SynExecutionScope.admit(
                            provider,
                            () ->
                                    CLIENT.sendAsync(
                                            request.POST(
                                                            HttpRequest.BodyPublishers.ofByteArray(
                                                                    body))
                                                    .build(),
                                            HttpResponse.BodyHandlers.ofPublisher()));
            var response = pending.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
            if (response.statusCode() != 200) {
                response.body()
                        .subscribe(
                                new Flow.Subscriber<List<ByteBuffer>>() {
                                    public void onSubscribe(Flow.Subscription s) {
                                        s.cancel();
                                    }

                                    public void onNext(List<ByteBuffer> b) {}

                                    public void onError(Throwable t) {}

                                    public void onComplete() {}
                                });
                throw new edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                        .ProviderException(
                        response.statusCode() == 429
                                ? edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                        .FailureKind.PROVIDER_RATE_LIMITED
                                : response.statusCode() >= 500
                                        ? edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                                .FailureKind.PROVIDER_UNAVAILABLE
                                        : edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                                .FailureKind.PROVIDER_REJECTED);
            }
            var queue = new ArrayBlockingQueue<Object>(32);
            Object eof = new Object();
            response.body()
                    .subscribe(
                            new Flow.Subscriber<>() {
                                int bytes;

                                public void onSubscribe(Flow.Subscription s) {
                                    subscription.set(s);
                                    s.request(1);
                                }

                                public void onNext(List<ByteBuffer> buffers) {
                                    var out = new ByteArrayOutputStream();
                                    for (var b : buffers) {
                                        bytes += b.remaining();
                                        if (bytes > MAX_RESPONSE_BYTES) {
                                            onError(new IOException("Provider response too large"));
                                            subscription.get().cancel();
                                            return;
                                        }
                                        byte[] a = new byte[b.remaining()];
                                        b.get(a);
                                        out.writeBytes(a);
                                    }
                                    if (!queue.offer(out.toByteArray())) {
                                        subscription.get().cancel();
                                        onError(
                                                new IOException(
                                                        "Provider stream exceeded queue budget"));
                                    }
                                }

                                public void onError(Throwable e) {
                                    queue.clear();
                                    queue.offer(e);
                                }

                                public void onComplete() {
                                    if (!queue.offer(eof)) {
                                        queue.clear();
                                        queue.offer(
                                                new IOException(
                                                        "Provider stream exceeded queue budget"));
                                    }
                                }
                            });
            var input =
                    new InputStream() {
                        byte[] current = new byte[0];
                        int index;
                        boolean ended;

                        public int read(byte[] bytes, int offset, int length) throws IOException {
                            if (length == 0) return 0;
                            int first = read();
                            if (first < 0) return -1;
                            bytes[offset] = (byte) first;
                            int extra = Math.min(length - 1, current.length - index);
                            System.arraycopy(current, index, bytes, offset + 1, extra);
                            index += extra;
                            return 1 + extra;
                        }

                        public int read() throws IOException {
                            if (ended) return -1;
                            while (index >= current.length) {
                                if (current.length > 0) {
                                    current = new byte[0];
                                    index = 0;
                                    subscription.get().request(1);
                                }
                                SynExecutionScope.checkExternal();
                                SynExecutionScope.checkProvider(provider);
                                if (SynExecutionScope.remaining(Duration.ofSeconds(60)).isZero())
                                    throw new IOException("Stream deadline");
                                Object next;
                                try {
                                    next = queue.poll(100, TimeUnit.MILLISECONDS);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new IOException(e);
                                }
                                if (next == null) continue;
                                if (next == eof) {
                                    ended = true;
                                    return -1;
                                }
                                if (next instanceof Throwable t)
                                    throw new IOException("Provider stream interrupted", t);
                                current = (byte[]) next;
                                index = 0;
                            }
                            return current[index++] & 255;
                        }
                    };
            try (var reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                var event = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        if (!event.isEmpty()) {
                            String value = event.toString();
                            event.setLength(0);
                            if ("[DONE]".equals(value)) break;
                            data.accept(value);
                        }
                    } else if (line.startsWith("data:")) {
                        if (!event.isEmpty()) event.append('\n');
                        event.append(line.substring(5).stripLeading());
                    }
                }
                if (!event.isEmpty() && !"[DONE]".contentEquals(event))
                    data.accept(event.toString());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Provider stream interrupted");
        } catch (IOException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Provider stream unavailable");
        } finally {
            if (subscription.get() != null) subscription.get().cancel();
            if (pending != null && !pending.isDone()) pending.cancel(true);
            IN_FLIGHT.release();
        }
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
