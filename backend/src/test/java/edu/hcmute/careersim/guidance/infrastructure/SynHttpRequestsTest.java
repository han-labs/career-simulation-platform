package edu.hcmute.careersim.guidance.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SynHttpRequestsTest {
    @Test
    void agentAttemptBudgetAlsoBoundsAStreamBodyAfterSuccessfulHeaders() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var executor = Executors.newCachedThreadPool();
        var release = new CountDownLatch(1);
        server.setExecutor(executor);
        server.createContext(
                "/stream",
                e -> {
                    e.getResponseHeaders().set("Content-Type", "text/event-stream");
                    e.sendResponseHeaders(200, 0);
                    e.getResponseBody()
                            .write(
                                    ": connected\n\n"
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    e.getResponseBody().flush();
                    try {
                        release.await(3, TimeUnit.SECONDS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } finally {
                        e.close();
                    }
                });
        server.start();
        try (var scope =
                edu.hcmute.careersim.guidance.service.SynExecutionScope.open(
                        Duration.ofSeconds(10), () -> true)) {
            long started = System.nanoTime();
            assertThatThrownBy(
                            () ->
                                    AgentTransportBudget.within(
                                            Duration.ofMillis(500),
                                            () -> {
                                                SynHttpRequests.stream(
                                                        java.net.URI.create(
                                                                "http://127.0.0.1:"
                                                                        + server.getAddress()
                                                                                .getPort()
                                                                        + "/stream"),
                                                        "gemini",
                                                        java.util.Map.of(),
                                                        "{}".getBytes(),
                                                        ignored -> {});
                                                return null;
                                            }))
                    .isInstanceOfSatisfying(
                            edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                    .ProviderException.class,
                            failure ->
                                    assertThat(failure.diagnostics().get("networkCategory"))
                                            .isEqualTo("TRANSPORT_DEADLINE"));
            assertThat(Duration.ofNanos(System.nanoTime() - started))
                    .isLessThan(Duration.ofSeconds(2));
        } finally {
            release.countDown();
            server.stop(0);
            executor.shutdownNow();
        }
    }

    @Test
    void agentBudgetUsesRemainingExecutionWithMarginWithoutChangingLegacyCap() {
        try (var scope =
                edu.hcmute.careersim.guidance.service.SynExecutionScope.open(
                        Duration.ofSeconds(60), () -> true)) {
            assertThat(SynHttpRequests.agentBudget())
                    .isBetween(Duration.ofSeconds(55), Duration.ofSeconds(58));
        }
        try (var scope =
                edu.hcmute.careersim.guidance.service.SynExecutionScope.open(
                        Duration.ofSeconds(5), () -> true)) {
            assertThat(SynHttpRequests.agentBudget())
                    .isBetween(Duration.ofSeconds(1), Duration.ofSeconds(3));
        }
        assertThat(SynHttpRequests.agentBudget()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void agentCancellationInterruptsAStalledResponseBeforeTheTransportDeadline() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var active = new java.util.concurrent.atomic.AtomicBoolean(true);
        var executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext(
                "/",
                exchange -> {
                    entered.countDown();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try {
            var client =
                    RestClient.builder()
                            .requestFactory(SynHttpRequests.agentDeadlineAware(null))
                            .build();
            var future =
                    executor.submit(
                            () -> {
                                try (var scope =
                                        edu.hcmute.careersim.guidance.service.SynExecutionScope
                                                .open(Duration.ofSeconds(60), active::get)) {
                                    client.get()
                                            .uri(
                                                    "http://127.0.0.1:"
                                                            + server.getAddress().getPort())
                                            .retrieve()
                                            .body(String.class);
                                }
                            });
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            active.set(false);
            assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class);
        } finally {
            release.countDown();
            server.stop(0);
            executor.shutdownNow();
        }
    }

    @Test
    void bodyStallingAfterHeadersStillTimesOutAndReleasesTheRequest() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.createContext(
                "/slow",
                exchange -> {
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().write('a');
                    exchange.getResponseBody().flush();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try {
            var client =
                    RestClient.builder()
                            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                            .requestFactory(SynHttpRequests.deadlineAware(1))
                            .build();
            long started = System.nanoTime();
            assertThatThrownBy(() -> client.get().uri("/slow").retrieve().body(String.class))
                    .isInstanceOf(org.springframework.web.client.ResourceAccessException.class);
            assertThat(Duration.ofNanos(System.nanoTime() - started))
                    .isLessThan(Duration.ofSeconds(3));
        } finally {
            release.countDown();
            server.stop(0);
            executor.shutdownNow();
        }
    }

    @Test
    void oversizedProviderBodyIsRejected() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/large",
                exchange -> {
                    byte[] bytes = new byte[300000];
                    exchange.sendResponseHeaders(200, bytes.length);
                    try {
                        exchange.getResponseBody().write(bytes);
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try {
            var client =
                    RestClient.builder()
                            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                            .requestFactory(SynHttpRequests.deadlineAware(3))
                            .build();
            assertThatThrownBy(() -> client.get().uri("/large").retrieve().body(String.class))
                    .isInstanceOf(org.springframework.web.client.ResourceAccessException.class);
        } finally {
            server.stop(0);
        }
    }
}
