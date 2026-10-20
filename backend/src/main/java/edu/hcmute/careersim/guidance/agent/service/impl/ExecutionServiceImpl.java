package edu.hcmute.careersim.guidance.agent.service.impl;

import static edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.dao.*;
import edu.hcmute.careersim.guidance.agent.domain.*;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.guidance.knowledge.domain.KnowledgeSourcePolicy;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Passage;
import edu.hcmute.careersim.guidance.knowledge.service.KnowledgeStudioService;
import edu.hcmute.careersim.guidance.service.*;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * One bounded runtime for standalone KB tests and exact saved Admin previews. No Student
 * impersonation.
 */
@Service
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class ExecutionServiceImpl implements ExecutionService {
    private final AgentExecutionDao dao;
    private final AgentRegistryDao registry;
    private final ActorAccess actors;
    private final AgentModelProvider model;
    private final StudentEvidenceTools domainTools;
    private final KnowledgeStudioService knowledge;
    private final AgentConfigurationPolicy prompts;
    private final SynUsagePricing pricing;
    private final ExecutorService workers;
    private final TransactionTemplate tx;

    public ExecutionServiceImpl(
            AgentExecutionDao dao,
            AgentRegistryDao registry,
            ActorAccess actors,
            AgentModelProvider model,
            StudentEvidenceTools domainTools,
            KnowledgeStudioService knowledge,
            AgentConfigurationPolicy prompts,
            SynUsagePricing pricing,
            @Qualifier("agentExecutionExecutor") ExecutorService workers,
            PlatformTransactionManager tm) {
        this.dao = dao;
        this.registry = registry;
        this.actors = actors;
        this.model = model;
        this.domainTools = domainTools;
        this.knowledge = knowledge;
        this.prompts = prompts;
        this.pricing = pricing;
        this.workers = workers;
        tx = new TransactionTemplate(tm);
    }

    private void admin(long id) {
        ActorAccess.requireRole(actors.requireId(id), "ADMIN");
    }

    private String minimized(String query) {
        String q = KnowledgeSourcePolicy.query(query);
        return q.replaceAll(
                "(?i)[\\w.+-]+@[\\w.-]+|eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+|(?:bearer\\s+|(?:api[_ -]?key|password|secret)\\s*[:=]\\s*)\\S+",
                "[redacted]");
    }

    public Map<String, Object> options(long admin) {
        admin(admin);
        return Map.of(
                "models",
                model.models(),
                "tools",
                StudentEvidenceTools.NAMES.stream()
                        .sorted()
                        .map(
                                n ->
                                        Map.of(
                                                "code",
                                                n,
                                                "description",
                                                n.equals("simulation.myEvidence")
                                                        ? "Owner-only Student evidence; denied in Admin preview"
                                                        : "Read-only authorized evidence",
                                                "readOnly",
                                                true))
                        .toList(),
                "promptTemplateRef",
                prompts.reference(),
                "defaultToolPolicy",
                Map.of(
                        "enabled",
                        List.of(),
                        "maxRounds",
                        2,
                        "maxToolCalls",
                        8,
                        "topK",
                        5,
                        "timeoutSeconds",
                        30),
                "defaultMemoryPolicy",
                Map.of("enabled", false),
                "defaultOutputPolicy",
                Map.of("schemaVersion", 1, "grounding", "KB_ONLY", "maxCharacters", 4000),
                "limits",
                Map.of(
                        "maxKnowledgeBases",
                        10,
                        "maxQueryCharacters",
                        2000,
                        "maxSourceBytes",
                        8 * 1024 * 1024,
                        "maxInputBytes",
                        64000));
    }

    public Execution preview(long admin, UUID agent, UUID configuration, Preview r) {
        admin(admin);
        var c = registry.configuration(agent, configuration, false);
        if ("ARCHIVED".equals(registry.get(agent, false).status()) || "RETIRED".equals(c.status()))
            throw new NotFoundException("Preview candidate unavailable.");
        return accept(
                admin,
                "ADMIN_PREVIEW",
                c,
                null,
                r.requestId(),
                r.query(),
                false,
                c.toolPolicy().path("topK").asInt(),
                c.modelKey());
    }

    public Execution test(long admin, UUID kb, KnowledgeContracts.Test r) {
        admin(admin);
        Configuration c =
                r.configurationId() == null ? null : registry.configuration(r.configurationId());
        if (c != null
                && ("RETIRED".equals(c.status())
                        || "ARCHIVED"
                                .equals(
                                        registry.get(UUID.fromString(c.agentId()), false)
                                                .status())))
            throw new NotFoundException("Configuration unavailable.");
        if (c != null && r.modelKey() != null && !r.modelKey().equals(c.modelKey()))
            CoreValues.invalid("A configuration-bound KB Test cannot override its model.");
        String key =
                r.retrievalOnly()
                        ? null
                        : c != null
                                ? c.modelKey()
                                : r.modelKey() != null
                                        ? r.modelKey()
                                        : model.models().stream()
                                                .filter(AgentModelProvider.Model::available)
                                                .findFirst()
                                                .orElseGet(() -> model.models().get(0))
                                                .key();
        return accept(
                admin,
                "KB_TEST",
                c,
                kb,
                r.requestId(),
                r.query(),
                r.retrievalOnly(),
                KnowledgeSourcePolicy.topK(r.topK()),
                key);
    }

    private String instructions(String mode, Configuration c) {
        if ("ADMIN_PREVIEW".equals(mode))
            return prompts.instructions(c.promptTemplateRef(), c.promptTemplate())
                    + "\nBackend output policy: "
                    + c.outputPolicy();
        return prompts.instructions(
                        prompts.reference(),
                        "Answer this independent KB question only from the supplied published passages. No Student context exists.")
                + "\nBackend output policy: "
                + defaultOutput();
    }

    private JsonNode defaultOutput() {
        return CoreValues.tree(
                Map.of("schemaVersion", 1, "grounding", "KB_ONLY", "maxCharacters", 4000));
    }

    private record Admission(Execution execution, boolean fresh) {}

    private Execution accept(
            long admin,
            String mode,
            Configuration c,
            UUID kb,
            UUID request,
            String query,
            boolean retrievalOnly,
            int topK,
            String modelKey) {
        if (request == null) CoreValues.invalid("A request ID is required.");
        String q = minimized(query);
        if (modelKey != null && !model.modelKeys().contains(modelKey))
            CoreValues.invalid("Unknown model key.");
        var bases =
                kb == null
                        ? c.knowledgeBaseIds().stream()
                                .map(UUID::fromString)
                                .collect(java.util.stream.Collectors.toSet())
                        : Set.of(kb);
        UUID cid = c == null ? null : UUID.fromString(c.id());
        knowledge.requireAvailable(admin, cid, bases);
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("query", q);
        snapshot.put("retrievalOnly", retrievalOnly);
        snapshot.put("topK", topK);
        snapshot.put("scope", bases.stream().map(UUID::toString).sorted().toList());
        snapshot.put("context", "PUBLIC_ADMIN_TEST_NO_STUDENT_DATA");
        String hash =
                CoreValues.hash(Arrays.asList(mode, cid, kb, query, retrievalOnly, topK, modelKey));
        String promptHash =
                CoreValues.bytesHash(
                        instructions(mode, c).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var admission =
                tx.execute(
                        s -> {
                            var actor = actors.lock(admin);
                            ActorAccess.requireRole(actor, "ADMIN");
                            knowledge.requireAvailable(admin, cid, bases);
                            var prior = dao.request(admin, mode, request);
                            if (prior.isPresent()) {
                                if (!hash.equals(dao.hash(UUID.fromString(prior.get().id()))))
                                    throw new ApiException(
                                            ErrorCode.CONFLICT,
                                            "Request ID already has different execution input.");
                                return new Admission(prior.get(), false);
                            }
                            return new Admission(
                                    dao.accept(
                                            admin,
                                            mode,
                                            cid,
                                            kb,
                                            request,
                                            hash,
                                            snapshot,
                                            modelKey,
                                            promptHash,
                                            c == null ? null : c.contentHash(),
                                            actor.privacyVersion()),
                                    true);
                        });
        if (!admission.fresh()) return read(admin, UUID.fromString(admission.execution().id()));
        var e = admission.execution();
        int seconds =
                c != null && "ADMIN_PREVIEW".equals(mode)
                        ? c.toolPolicy().path("timeoutSeconds").asInt()
                        : 30;
        var context =
                new ExecutionContext(
                        UUID.fromString(e.id()),
                        admin,
                        mode,
                        c,
                        bases,
                        q,
                        topK,
                        retrievalOnly,
                        modelKey,
                        actors.requireId(admin).privacyVersion(),
                        e.startedAt().plusSeconds(seconds));
        try {
            workers.execute(() -> run(context));
        } catch (RejectedExecutionException busy) {
            tx.executeWithoutResult(
                    s ->
                            dao.finish(
                                    context.executionId(),
                                    "FAILED",
                                    "EXECUTION_CAPACITY",
                                    List.of(),
                                    fallback("UNAVAILABLE"),
                                    null,
                                    null,
                                    null,
                                    "NOT_APPLICABLE",
                                    0,
                                    Map.of()));
        }
        return e;
    }

    private boolean active(ExecutionContext c) {
        try {
            var actor = actors.requireId(c.actor());
            if (!"ADMIN".equals(actor.role())
                    || actor.privacyVersion() != c.privacyVersion()
                    || !c.deadline().isAfter(Instant.now())) return false;
            var e = dao.get(c.executionId(), false);
            if (!Set.of("ACCEPTED", "RUNNING").contains(e.status()) || e.cancellationRequested())
                return false;
            if (c.configuration() != null) {
                var current = registry.configuration(UUID.fromString(c.configuration().id()));
                if ("RETIRED".equals(current.status())
                        || !current.contentHash().equals(c.configuration().contentHash())
                        || "ARCHIVED"
                                .equals(
                                        registry.get(UUID.fromString(current.agentId()), false)
                                                .status())) return false;
            }
            knowledge.requireAvailable(
                    c.actor(),
                    c.configuration() == null ? null : UUID.fromString(c.configuration().id()),
                    c.knowledgeBases());
            return true;
        } catch (ApiException denied) {
            return false;
        }
    }

    private void checkpoint(ExecutionContext c, List<Passage> evidence) {
        if (!active(c)
                || !knowledge.evidenceAvailable(
                        c.actor(),
                        c.configuration() == null ? null : UUID.fromString(c.configuration().id()),
                        evidence))
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.CONSENT_REVOKED);
    }

    private void run(ExecutionContext c) {
        var steps = new ArrayList<Map<String, Object>>();
        var evidence = new LinkedHashMap<String, Passage>();
        var domainEvidence = new LinkedHashMap<String, DomainEvidence>();
        JsonNode output = null;
        String status = "FAILED", error = null;
        Duration remaining = Duration.between(Instant.now(), c.deadline());
        try (var scope =
                SynExecutionScope.open(
                                remaining.isNegative() ? Duration.ofMillis(1) : remaining,
                                () -> active(c))
                        .withDispatchAdmission(
                                new SynExecutionScope.DispatchAdmission() {
                                    public <T> T admit(java.util.function.Supplier<T> start) {
                                        return tx.execute(
                                                s -> {
                                                    ActorAccess.requireRole(
                                                            actors.lock(c.actor()), "ADMIN");
                                                    if (c.configuration() != null)
                                                        registry.get(
                                                                UUID.fromString(
                                                                        c.configuration()
                                                                                .agentId()),
                                                                true);
                                                    knowledge.lockAvailable(
                                                            c.actor(),
                                                            c.configuration() == null
                                                                    ? null
                                                                    : UUID.fromString(
                                                                            c.configuration().id()),
                                                            c.knowledgeBases(),
                                                            new ArrayList<>(evidence.values()));
                                                    checkpoint(
                                                            c, new ArrayList<>(evidence.values()));
                                                    return start.get();
                                                });
                                    }
                                })) {
            try {
                tx.executeWithoutResult(
                        s -> {
                            var e = dao.get(c.executionId(), true);
                            if (!e.cancellationRequested()) dao.begin(c.executionId());
                        });
                checkpoint(c, List.of());
                if ("KB_TEST".equals(c.mode())) {
                    long at = System.nanoTime();
                    var found =
                            knowledge.search(
                                    c.actor(),
                                    c.configuration() == null
                                            ? null
                                            : UUID.fromString(c.configuration().id()),
                                    c.knowledgeBases(),
                                    c.query(),
                                    c.topK(),
                                    true);
                    for (var p : found) evidence.put(p.referenceId(), p);
                    steps.add(
                            Map.of(
                                    "type",
                                    "SERVER_RETRIEVAL",
                                    "name",
                                    "kb.search",
                                    "status",
                                    "SUCCEEDED",
                                    "elapsedMs",
                                    (System.nanoTime() - at) / 1_000_000,
                                    "sources",
                                    found));
                    if (c.retrievalOnly()) {
                        output =
                                CoreValues.tree(
                                        Map.of(
                                                "schemaVersion",
                                                1,
                                                "outcome",
                                                "RETRIEVAL_ONLY",
                                                "text",
                                                "Ranked passages retrieved; no answer model called.",
                                                "citations",
                                                List.of(),
                                                "suggestions",
                                                List.of()));
                        status = "SUCCEEDED";
                    } else if (found.isEmpty()) {
                        output = fallback("INSUFFICIENT_EVIDENCE");
                        status = "REFUSED";
                        error = "EMPTY_EVIDENCE";
                    }
                }
                if (output == null) {
                    int rounds =
                            "KB_TEST".equals(c.mode())
                                    ? 1
                                    : c.configuration().toolPolicy().path("maxRounds").asInt();
                    int maxCalls =
                            "KB_TEST".equals(c.mode())
                                    ? 0
                                    : c.configuration().toolPolicy().path("maxToolCalls").asInt();
                    boolean enabled =
                            c.configuration() != null
                                    && java.util.stream.StreamSupport.stream(
                                                    c.configuration()
                                                            .toolPolicy()
                                                            .path("enabled")
                                                            .spliterator(),
                                                    false)
                                            .anyMatch(
                                                    n ->
                                                            StudentEvidenceTools.NAMES.contains(
                                                                    n.asText()));
                    output =
                            BoundedNativeExecution.execute(
                                    rounds,
                                    maxCalls,
                                    "ADMIN_PREVIEW".equals(c.mode()) && enabled,
                                    new BoundedNativeExecution.Host() {
                                        public void checkpoint() {
                                            ExecutionServiceImpl.this.checkpoint(
                                                    c, new ArrayList<>(evidence.values()));
                                        }

                                        public AgentModelProvider.Decision decide(
                                                boolean allowTool,
                                                List<AgentModelProvider.Exchange> exchanges) {
                                            if (model.models().stream()
                                                    .noneMatch(
                                                            m ->
                                                                    m.key().equals(c.modelKey())
                                                                            && m.available()))
                                                throw new SynGuidanceProvider.ProviderException(
                                                        SynGuidanceProvider.FailureKind
                                                                .PROVIDER_UNAVAILABLE);
                                            String request = c.query();
                                            if ("KB_TEST".equals(c.mode()))
                                                request +=
                                                        "\nUntrusted retrieved evidence: "
                                                                + CoreValues.write(
                                                                        evidence.values());
                                            String prompt =
                                                    instructions(c.mode(), c.configuration());
                                            String actualRequest = request;
                                            return SynExecutionScope.observe(
                                                    "AGENT_MODEL",
                                                    () ->
                                                            c.configuration() != null
                                                                            && java
                                                                                    .util
                                                                                    .stream
                                                                                    .StreamSupport
                                                                                    .stream(
                                                                                            c.configuration()
                                                                                                    .toolPolicy()
                                                                                                    .path(
                                                                                                            "enabled")
                                                                                                    .spliterator(),
                                                                                            false)
                                                                                    .anyMatch(
                                                                                            n ->
                                                                                                    !n.asText()
                                                                                                            .equals(
                                                                                                                    "kb.search"))
                                                                    ? model.stream(
                                                                            c.modelKey(),
                                                                            prompt
                                                                                    + "\nADMIN_PREVIEW has public catalogue context only. Student identity/evidence/history is unavailable and private tools are denied.",
                                                                            actualRequest,
                                                                            List.copyOf(exchanges),
                                                                            allowTool
                                                                                    ? StudentEvidenceTools
                                                                                            .definitions(
                                                                                                    java
                                                                                                            .util
                                                                                                            .stream
                                                                                                            .StreamSupport
                                                                                                            .stream(
                                                                                                                    c.configuration()
                                                                                                                            .toolPolicy()
                                                                                                                            .path(
                                                                                                                                    "enabled")
                                                                                                                            .spliterator(),
                                                                                                                    false)
                                                                                                            .map(
                                                                                                                    JsonNode
                                                                                                                            ::asText)
                                                                                                            .toList())
                                                                                    : List.of(),
                                                                            delta -> {})
                                                                    : model.decide(
                                                                            c.modelKey(),
                                                                            prompt,
                                                                            actualRequest,
                                                                            List.copyOf(exchanges),
                                                                            allowTool));
                                        }

                                        public JsonNode output(
                                                AgentModelProvider.Decision decision) {
                                            JsonNode validated =
                                                    StudentOutputValidator.validate(
                                                            decision.output(),
                                                            "CHAT",
                                                            new ArrayList<>(evidence.values()),
                                                            domainEvidence.values(),
                                                            "KB_TEST".equals(c.mode())
                                                                    ? defaultOutput()
                                                                    : c.configuration()
                                                                            .outputPolicy());
                                            return validated;
                                        }

                                        public List<JsonNode> tools(
                                                AgentModelProvider.Decision decision) {
                                            var observations = new ArrayList<JsonNode>();
                                            for (var call : decision.calls()) {
                                                long at = System.nanoTime();
                                                var step = new LinkedHashMap<String, Object>();
                                                step.put("type", "ACTUAL_NATIVE_CALL");
                                                step.put(
                                                        "name",
                                                        call.name() != null
                                                                        && call.name()
                                                                                .matches(
                                                                                        "[a-zA-Z0-9_.-]{1,80}")
                                                                ? call.name()
                                                                : "UNKNOWN");
                                                step.put("arguments", Map.of());
                                                JsonNode observation;
                                                try {
                                                    checkpoint();
                                                    boolean granted =
                                                            c.configuration() != null
                                                                    && java
                                                                            .util
                                                                            .stream
                                                                            .StreamSupport
                                                                            .stream(
                                                                                    c.configuration()
                                                                                            .toolPolicy()
                                                                                            .path(
                                                                                                    "enabled")
                                                                                            .spliterator(),
                                                                                    false)
                                                                            .anyMatch(
                                                                                    n ->
                                                                                            n.asText()
                                                                                                    .equals(
                                                                                                            call
                                                                                                                    .name()));
                                                    if (!granted)
                                                        throw new ApiException(
                                                                ErrorCode.FORBIDDEN,
                                                                "Tool is not enabled.");
                                                    if (!"kb.search".equals(call.name())) {
                                                        var found =
                                                                domainTools.preview(
                                                                        call.name(),
                                                                        call.arguments());
                                                        found.forEach(
                                                                d ->
                                                                        domainEvidence.put(
                                                                                d.referenceId(),
                                                                                d));
                                                        step.put("status", "SUCCEEDED");
                                                        step.put("evidence", found);
                                                        observation =
                                                                CoreValues.tree(
                                                                        Map.of(
                                                                                "status",
                                                                                "SUCCEEDED",
                                                                                "evidence",
                                                                                found));
                                                    } else {
                                                        var args = call.arguments();
                                                        if (args == null
                                                                || !args.isObject()
                                                                || args.size() < 1
                                                                || args.size() > 2
                                                                || !args.path("query").isTextual())
                                                            CoreValues.invalid(
                                                                    "Invalid KB search arguments.");
                                                        args.fieldNames()
                                                                .forEachRemaining(
                                                                        k -> {
                                                                            if (!Set.of(
                                                                                            "query",
                                                                                            "topK")
                                                                                    .contains(k))
                                                                                CoreValues.invalid(
                                                                                        "Unknown KB search argument.");
                                                                        });
                                                        if (args.has("topK")
                                                                && (!args.path("topK")
                                                                                .isIntegralNumber()
                                                                        || !args.path("topK")
                                                                                .canConvertToInt()))
                                                            CoreValues.invalid("Invalid Top K.");
                                                        String query =
                                                                minimized(
                                                                        args.path("query")
                                                                                .asText());
                                                        int top =
                                                                KnowledgeSourcePolicy.topK(
                                                                        args.has("topK")
                                                                                ? args.path("topK")
                                                                                        .asInt()
                                                                                : c.topK());
                                                        step.put(
                                                                "arguments",
                                                                Map.of(
                                                                        "query", query, "topK",
                                                                        top));
                                                        var found =
                                                                knowledge.search(
                                                                        c.actor(),
                                                                        UUID.fromString(
                                                                                c.configuration()
                                                                                        .id()),
                                                                        c.knowledgeBases(),
                                                                        query,
                                                                        top,
                                                                        true);
                                                        for (var p : found)
                                                            evidence.put(p.referenceId(), p);
                                                        step.put("status", "SUCCEEDED");
                                                        step.put("sources", found);
                                                        observation =
                                                                CoreValues.tree(
                                                                        Map.of(
                                                                                "status",
                                                                                "SUCCEEDED",
                                                                                "passages",
                                                                                found));
                                                    }
                                                } catch (ApiException denied) {
                                                    step.put("status", "DENIED");
                                                    step.put(
                                                            "errorCode",
                                                            denied.getErrorCode().getCode());
                                                    observation =
                                                            CoreValues.tree(
                                                                    Map.of(
                                                                            "status",
                                                                            "DENIED",
                                                                            "errorCode",
                                                                            denied.getErrorCode()
                                                                                    .getCode()));
                                                }
                                                step.put(
                                                        "elapsedMs",
                                                        (System.nanoTime() - at) / 1_000_000);
                                                steps.add(step);
                                                observations.add(observation);
                                            }
                                            return observations;
                                        }
                                    });
                    status =
                            "ANSWER".equals(output.path("outcome").asText())
                                    ? "SUCCEEDED"
                                    : "REFUSED";
                    if ("REFUSED".equals(status)) error = output.path("outcome").asText();
                }
            } catch (SynGuidanceProvider.ProviderException failure) {
                error = failure.kind().name();
                boolean denied = failure.kind() == SynGuidanceProvider.FailureKind.CONSENT_REVOKED;
                status =
                        denied
                                ? "CANCELLED"
                                : failure.kind() == SynGuidanceProvider.FailureKind.EMPTY_EVIDENCE
                                        ? "REFUSED"
                                        : "FAILED";
                output =
                        fallback(
                                "REFUSED".equals(status) ? "INSUFFICIENT_EVIDENCE" : "UNAVAILABLE");
            } catch (RuntimeException failure) {
                status = "FAILED";
                error = "EXECUTION_FAILED";
                output = fallback("UNAVAILABLE");
            }
            var metrics = usage(c.retrievalOnly());
            String finishedStatus = status, finishedError = error;
            JsonNode finishedOutput = output;
            tx.executeWithoutResult(
                    s -> {
                        boolean locked = true;
                        try {
                            ActorAccess.requireRole(actors.lock(c.actor()), "ADMIN");
                            if (c.configuration() != null)
                                registry.get(UUID.fromString(c.configuration().agentId()), true);
                            knowledge.lockAvailable(
                                    c.actor(),
                                    c.configuration() == null
                                            ? null
                                            : UUID.fromString(c.configuration().id()),
                                    c.knowledgeBases(),
                                    new ArrayList<>(evidence.values()));
                        } catch (ApiException denied) {
                            locked = false;
                        }
                        var current = dao.get(c.executionId(), true);
                        if (!Set.of("ACCEPTED", "RUNNING").contains(current.status())) return;
                        boolean available =
                                locked
                                        && active(c)
                                        && knowledge.evidenceAvailable(
                                                c.actor(),
                                                c.configuration() == null
                                                        ? null
                                                        : UUID.fromString(c.configuration().id()),
                                                new ArrayList<>(evidence.values()));
                        String finalStatus = available ? finishedStatus : "CANCELLED";
                        String finalError =
                                available
                                        ? finishedError
                                        : current.cancellationRequested()
                                                ? "CANCELLED"
                                                : "AUTHORITY_OR_DEADLINE_CHANGED";
                        dao.finish(
                                c.executionId(),
                                finalStatus,
                                finalError,
                                available ? steps : redactedSteps(steps),
                                available ? finishedOutput : null,
                                metrics.input(),
                                metrics.output(),
                                metrics.cost(),
                                metrics.status(),
                                Math.max(
                                        0,
                                        Duration.between(current.startedAt(), Instant.now())
                                                .toMillis()),
                                metrics.detail());
                    });
        }
    }

    private record Metrics(
            Long input, Long output, BigDecimal cost, String status, Map<String, Object> detail) {}

    private List<Map<String, Object>> redactedSteps(List<Map<String, Object>> steps) {
        return steps.stream()
                .map(
                        step -> {
                            var safe = new LinkedHashMap<String, Object>();
                            for (String key :
                                    List.of("type", "name", "status", "errorCode", "elapsedMs"))
                                if (step.containsKey(key)) safe.put(key, step.get(key));
                            safe.put("contentRedacted", true);
                            return Map.copyOf(safe);
                        })
                .toList();
    }

    private Metrics usage(boolean retrievalOnly) {
        var trace = SynExecutionScope.trace();
        var calls = (List<?>) trace.get("calls");
        var reported = trace.get("tokenUsage") instanceof List<?> list ? list : List.of();
        var generationCalls =
                calls.stream()
                        .filter(
                                x ->
                                        x instanceof Map<?, ?> m
                                                && "AGENT_MODEL".equals(m.get("phase")))
                        .toList();
        var generationUsage =
                reported.stream()
                        .filter(
                                x ->
                                        x instanceof Map<?, ?> m
                                                && "AGENT_MODEL".equals(m.get("phase")))
                        .toList();
        var priced =
                pricing.enrich(Map.of("calls", generationCalls, "tokenUsage", generationUsage));
        JsonNode cost = CoreValues.tree(priced.get("cost"));
        String state =
                retrievalOnly || generationCalls.isEmpty()
                        ? "NOT_APPLICABLE"
                        : cost.path("status").asText().equals("ESTIMATED")
                                ? "ESTIMATED"
                                : cost.path("status").asText().equals("PARTIAL")
                                        ? "PARTIAL"
                                        : "UNKNOWN";
        boolean complete =
                !generationCalls.isEmpty()
                        && generationCalls.size() == generationUsage.size()
                        && generationUsage.stream()
                                .allMatch(
                                        x ->
                                                x instanceof Map<?, ?> m
                                                        && m.get("inputTokens") instanceof Number
                                                        && m.get("outputTokens") instanceof Number);
        Long input =
                complete
                        ? generationUsage.stream()
                                .mapToLong(
                                        x ->
                                                ((Number) ((Map<?, ?>) x).get("inputTokens"))
                                                        .longValue())
                                .sum()
                        : null;
        Long output =
                complete
                        ? generationUsage.stream()
                                .mapToLong(
                                        x ->
                                                ((Number) ((Map<?, ?>) x).get("outputTokens"))
                                                        .longValue())
                                .sum()
                        : null;
        BigDecimal estimate =
                Set.of("ESTIMATED", "PARTIAL").contains(state)
                        ? cost.path(
                                        state.equals("ESTIMATED")
                                                ? "totalEstimateUsd"
                                                : "estimatedSubtotalUsd")
                                .decimalValue()
                        : null;
        if (estimate != null && estimate.compareTo(new BigDecimal("9999999999.99999999")) > 0) {
            state = "UNKNOWN";
            estimate = null;
        }
        var resource =
                pricing.enrich(
                        Map.of(
                                "calls",
                                calls.stream()
                                        .filter(
                                                x ->
                                                        x instanceof Map<?, ?> m
                                                                && !"AGENT_MODEL"
                                                                        .equals(m.get("phase")))
                                        .toList(),
                                "tokenUsage",
                                reported.stream()
                                        .filter(
                                                x ->
                                                        x instanceof Map<?, ?> m
                                                                && !"AGENT_MODEL"
                                                                        .equals(m.get("phase")))
                                        .toList()));
        return new Metrics(
                retrievalOnly ? null : input,
                retrievalOnly ? null : output,
                retrievalOnly ? null : estimate,
                state,
                Map.of(
                        "generation",
                        priced,
                        "embeddingResources",
                        resource,
                        "denominator",
                        "MODEL_GENERATION_ONLY; EMBEDDING_RESOURCE_USAGE_SEPARATE"));
    }

    private JsonNode fallback(String kind) {
        return CoreValues.tree(
                Map.of(
                        "schemaVersion",
                        1,
                        "outcome",
                        kind,
                        "text",
                        "INSUFFICIENT_EVIDENCE".equals(kind)
                                ? "There is insufficient authorized knowledge to answer this question."
                                : "The Agent could not complete this request safely. Core assessments and simulations remain available.",
                        "citations",
                        List.of(),
                        "suggestions",
                        List.of()));
    }

    public Execution read(long admin, UUID id) {
        admin(admin);
        var e = dao.get(id, false);
        if (!e.initiatedByUserId().equals(Long.toString(admin))
                || !Set.of("KB_TEST", "ADMIN_PREVIEW").contains(e.executionMode()))
            throw new NotFoundException("Execution not found.");
        UUID cid = e.configurationId() == null ? null : UUID.fromString(e.configurationId());
        var bases = new HashSet<UUID>();
        for (var value : e.inputSnapshot().path("scope"))
            bases.add(UUID.fromString(value.asText()));
        knowledge.requireAvailable(admin, cid, bases);
        for (var step : e.toolStepsAndSources())
            for (var source : step.path("sources")) {
                var visible =
                        knowledge.source(
                                admin,
                                UUID.fromString(source.path("knowledgeBaseId").asText()),
                                Long.parseLong(source.path("chunkId").asText()));
                if (!visible.versionId().equals(source.path("versionId").asText()))
                    throw new NotFoundException("Execution source is no longer available.");
            }
        if (Set.of("ACCEPTED", "RUNNING").contains(e.status())
                && e.startedAt().isBefore(Instant.now().minusSeconds(90))) {
            tx.executeWithoutResult(
                    s -> {
                        var current = dao.get(id, true);
                        if (Set.of("ACCEPTED", "RUNNING").contains(current.status()))
                            dao.finish(
                                    id,
                                    current.cancellationRequested() ? "CANCELLED" : "FAILED",
                                    "INTERRUPTED_OR_EXPIRED",
                                    List.of(),
                                    null,
                                    null,
                                    null,
                                    null,
                                    "UNKNOWN",
                                    Duration.between(e.startedAt(), Instant.now()).toMillis(),
                                    Map.of());
                    });
            return dao.get(id, false);
        }
        return e;
    }

    public Execution cancel(long admin, UUID id) {
        admin(admin);
        tx.executeWithoutResult(
                s -> {
                    ActorAccess.requireRole(actors.lock(admin), "ADMIN");
                    var e = dao.get(id, true);
                    if (!e.initiatedByUserId().equals(Long.toString(admin))
                            || !Set.of("KB_TEST", "ADMIN_PREVIEW").contains(e.executionMode()))
                        throw new NotFoundException("Execution not found.");
                    dao.cancel(id);
                });
        // Cancellation must not become an alternative replay path around current source scope.
        return read(admin, id);
    }
}
