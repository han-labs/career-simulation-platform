package edu.hcmute.careersim.guidance.agent.controller;

import edu.hcmute.careersim.common.config.BrowserCredentialCookies;
import edu.hcmute.careersim.guidance.agent.service.StudentAgentService;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import jakarta.servlet.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class AgentEventStreamController {
    private final StudentAgentService service;
    private final BrowserAuthentication authentication;
    private final BrowserCredentialCookies cookies;
    private final ScheduledExecutorService scheduler;
    private final ActorAccess actors;

    public AgentEventStreamController(
            StudentAgentService service,
            BrowserAuthentication authentication,
            BrowserCredentialCookies cookies,
            ActorAccess actors,
            @Qualifier("agentEventScheduler") ScheduledExecutorService scheduler) {
        this.service = service;
        this.authentication = authentication;
        this.cookies = cookies;
        this.scheduler = scheduler;
        this.actors = actors;
    }

    @Bean(name = "agentEventScheduler", destroyMethod = "shutdownNow")
    static ScheduledExecutorService scheduler() {
        return Executors.newScheduledThreadPool(
                2,
                r -> {
                    var t = new Thread(r, "agent-event-replay");
                    t.setDaemon(true);
                    return t;
                });
    }

    @GetMapping(value = "/v2/agent-executions/{id}/events", produces = "text/event-stream")
    public SseEmitter events(
            @PathVariable UUID id,
            @RequestHeader(value = "Last-Event-ID", defaultValue = "0") long after,
            HttpServletRequest request,
            HttpServletResponse response) {
        String credential = cookies.read(request);
        long owner = actors.student(authentication.verify(credential)).id();
        service.read(owner, id);
        if (after < 0) throw new IllegalArgumentException("Invalid event cursor");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Accel-Buffering", "no");
        var emitter = new SseEmitter(75000L);
        var cursor = new AtomicLong(after);
        var closed = new AtomicBoolean();
        var future = new AtomicReference<ScheduledFuture<?>>();
        Runnable stop =
                () -> {
                    closed.set(true);
                    var f = future.get();
                    if (f != null) f.cancel(false);
                };
        emitter.onCompletion(stop);
        emitter.onTimeout(stop);
        emitter.onError(e -> stop.run());
        Runnable tick =
                () -> {
                    if (closed.get()) return;
                    try {
                        if (authentication.verify(credential).id() != owner)
                            throw new IllegalStateException("Actor changed");
                        var run = service.read(owner, id);
                        var events = service.events(owner, id, cursor.get());
                        for (var e : events) {
                            emitter.send(
                                    SseEmitter.event()
                                            .id(Long.toString(e.sequenceNumber()))
                                            .name(e.type())
                                            .data(e.payload()));
                            cursor.set(e.sequenceNumber());
                        }
                        if (!Set.of("ACCEPTED", "RUNNING").contains(run.status())
                                && cursor.get() >= run.lastEventSequence()) {
                            stop.run();
                            emitter.complete();
                        } else if (events.isEmpty())
                            emitter.send(SseEmitter.event().comment("keepalive"));
                    } catch (Exception denied) {
                        stop.run();
                        emitter.complete();
                    }
                };
        future.set(scheduler.scheduleWithFixedDelay(tick, 0, 150, TimeUnit.MILLISECONDS));
        if (closed.get()) stop.run();
        return emitter;
    }

    /** A disconnected SSE socket has no JSON response body to serialize. */
    @ExceptionHandler(java.io.IOException.class)
    public void disconnected() {}

    @ExceptionHandler(edu.hcmute.careersim.common.exception.ApiException.class)
    public void denied(
            edu.hcmute.careersim.common.exception.ApiException error,
            HttpServletRequest request,
            HttpServletResponse response)
            throws java.io.IOException {
        if (response.isCommitted()) return;
        response.setStatus(error.getErrorCode().getStatus().value());
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValue(
                        response.getOutputStream(),
                        edu.hcmute.careersim.common.response.ErrorResponse.of(
                                error.getErrorCode().getCode(),
                                error.getMessage(),
                                request.getRequestURI()));
    }
}
