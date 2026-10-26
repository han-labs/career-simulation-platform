package edu.hcmute.careersim.guidance.knowledge.service.impl;

import static edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.*;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.storage.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.domain.HybridRetrievalPolicy;
import edu.hcmute.careersim.guidance.knowledge.dao.KnowledgeStudioDao;
import edu.hcmute.careersim.guidance.knowledge.domain.KnowledgeSourcePolicy;
import edu.hcmute.careersim.guidance.knowledge.infrastructure.KnowledgeParser;
import edu.hcmute.careersim.guidance.knowledge.service.KnowledgeStudioService;
import edu.hcmute.careersim.guidance.service.*;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.time.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class KnowledgeStudioServiceImpl implements KnowledgeStudioService {
    private final KnowledgeStudioDao dao;
    private final ActorAccess actors;
    private final edu.hcmute.careersim.guidance.agent.dao.AgentRegistryDao registry;
    private final FileAssetService files;
    private final KnowledgeParser parser;
    private final EmbeddingProvider embedding;
    private final SynUsagePricing pricing;
    private final AuditWriter audit;
    private final TransactionTemplate tx;

    public KnowledgeStudioServiceImpl(
            KnowledgeStudioDao dao,
            ActorAccess actors,
            edu.hcmute.careersim.guidance.agent.dao.AgentRegistryDao registry,
            FileAssetService files,
            KnowledgeParser parser,
            EmbeddingProvider embedding,
            SynUsagePricing pricing,
            AuditWriter audit,
            PlatformTransactionManager tm) {
        this.dao = dao;
        this.actors = actors;
        this.registry = registry;
        this.files = files;
        this.parser = parser;
        this.embedding = embedding;
        this.pricing = pricing;
        this.audit = audit;
        tx = new TransactionTemplate(tm);
    }

    private void admin(long id) {
        ActorAccess.requireRole(actors.requireId(id), "ADMIN");
    }

    private void lockAdmin(long id) {
        ActorAccess.requireRole(actors.lock(id), "ADMIN");
    }

    private Base available(UUID kb, boolean lock) {
        var b = dao.base(kb, lock);
        if (!"ACTIVE".equals(b.status()))
            throw new NotFoundException("Knowledge base unavailable.");
        return b;
    }

    public List<BaseSummary> list(long admin) {
        admin(admin);
        return dao.list();
    }

    public Base create(long admin, BaseInput input) {
        String name = CoreValues.text(input.name(), 180);
        return tx.execute(
                s -> {
                    lockAdmin(admin);
                    var b = dao.create(admin, name);
                    audit.recordKey(
                            admin,
                            "KB_CREATED",
                            "knowledge_base",
                            b.id(),
                            Map.of("status", "ACTIVE"));
                    return b;
                });
    }

    public Base get(long admin, UUID kb) {
        admin(admin);
        return dao.base(kb, false);
    }

    public Base archive(long admin, UUID kb, long revision) {
        var b = get(admin, kb);
        return update(admin, kb, revision, new BaseInput(b.name(), "ARCHIVED"));
    }

    public Base update(long admin, UUID kb, long revision, BaseInput input) {
        String name = CoreValues.text(input.name(), 180);
        if (input.status() == null
                || !Set.of("ACTIVE", "HIDDEN", "ARCHIVED").contains(input.status()))
            CoreValues.invalid("Invalid knowledge base status.");
        return tx.execute(
                s -> {
                    lockAdmin(admin);
                    var b = dao.base(kb, true);
                    CoreValues.match(revision, b.revision());
                    dao.update(kb, name, input.status());
                    audit.recordKey(
                            admin,
                            "KB_UPDATED",
                            "knowledge_base",
                            kb.toString(),
                            Map.of("status", input.status()));
                    return dao.base(kb, false);
                });
    }

    public Job upload(long admin, UUID kb, Upload input, String name, String mime, byte[] bytes) {
        admin(admin);
        KnowledgeSourcePolicy.validate(input);
        parser.validateType(name, mime, bytes);
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("assetKey", input.assetKey());
        metadata.put("title", input.title());
        metadata.put("license", input.license());
        metadata.put("sourceUri", input.sourceUri());
        metadata.put("language", input.language());
        metadata.put("track", input.track());
        metadata.put("reviewer", actors.requireId(admin).displayName());
        metadata.put("reviewed", true);
        metadata.put("originalName", name);
        metadata.put("mimeType", mime);
        metadata.put("fileHash", CoreValues.bytesHash(bytes));
        metadata.put("parserProfile", KnowledgeParser.PROFILE);
        String hash = CoreValues.hash(Map.of("kb", kb, "metadata", metadata));
        return tx.execute(
                s -> {
                    lockAdmin(admin);
                    available(kb, true);
                    var existing = dao.request(admin, input.requestId());
                    if (existing.isPresent()) {
                        if (!hash.equals(dao.inputHash(UUID.fromString(existing.get().id()))))
                            throw new ApiException(
                                    ErrorCode.CONFLICT,
                                    "Request ID already has different upload content.");
                        return existing.get();
                    }
                    var asset = files.uploadKnowledgeSource(admin, name, mime, bytes);
                    var job =
                            dao.createJob(
                                    kb, admin, Long.parseLong(asset.id()), input, hash, metadata);
                    audit.recordKey(
                            admin,
                            "KB_SOURCE_STAGED",
                            "knowledge_ingestion_job",
                            job.id(),
                            Map.of("hash", hash));
                    return job;
                });
    }

    public Job job(long admin, UUID kb, UUID id) {
        admin(admin);
        dao.base(kb, false);
        return dao.job(kb, id, false);
    }

    public List<Job> jobs(long admin, UUID kb) {
        admin(admin);
        dao.base(kb, false);
        return dao.jobs(kb);
    }

    private boolean active(long admin, UUID kb, UUID id, long generation) {
        try {
            admin(admin);
            available(kb, false);
            var j = dao.job(kb, id, false);
            return j.revision() == generation && Set.of("PARSING", "EMBEDDING").contains(j.state());
        } catch (ApiException denied) {
            return false;
        }
    }

    public Job retry(long admin, UUID kb, UUID id) {
        return process(admin, kb, id, null);
    }

    public Job reprocess(long admin, UUID kb, UUID id, long revision) {
        return process(admin, kb, id, revision);
    }

    private Job process(long admin, UUID kb, UUID id, Long expectedRevision) {
        Job claimed =
                tx.execute(
                        s -> {
                            lockAdmin(admin);
                            available(kb, true);
                            var j = dao.job(kb, id, true);
                            if (expectedRevision != null) {
                                CoreValues.match(expectedRevision, j.revision());
                                if (j.producedVersionId() != null
                                        && "RETIRED"
                                                .equals(
                                                        dao.version(
                                                                        kb,
                                                                        j.assetKey(),
                                                                        Long.parseLong(
                                                                                j
                                                                                        .producedVersionId()),
                                                                        true)
                                                                .status()))
                                    throw new ApiException(
                                            ErrorCode.CONFLICT,
                                            "Retired content requires a new source version.");
                                // A new attempt must never revive a retired source. Check the
                                // original file's versions even after a failed attempt cleared
                                // producedVersionId. The existing base lock serializes retire.
                                var versions = dao.versions(kb);
                                var sources =
                                        versions.stream()
                                                .filter(
                                                        v ->
                                                                v.assetKey().equals(j.assetKey())
                                                                        && v.sourceFileId()
                                                                                .equals(
                                                                                        j
                                                                                                .sourceFileId()))
                                                .toList();
                                if (!sources.isEmpty()
                                        && sources.stream()
                                                .allMatch(v -> "RETIRED".equals(v.status())))
                                    throw new ApiException(
                                            ErrorCode.CONFLICT,
                                            "Retired content requires a new source version.");
                                if (j.producedVersionId() == null
                                        && sources.isEmpty()
                                        && versions.size() >= 500)
                                    throw new ApiException(
                                            ErrorCode.CONFLICT,
                                            "Source history cannot be resolved safely. Upload a replacement.");
                            }
                            if (expectedRevision == null
                                    && Set.of("READY", "PUBLISHED").contains(j.state())) {
                                var version =
                                        dao.version(
                                                kb,
                                                j.assetKey(),
                                                Long.parseLong(j.producedVersionId()),
                                                true);
                                if ("RETIRED".equals(version.status())
                                        || dao.chunks(Long.parseLong(version.id())).isEmpty())
                                    throw new ApiException(
                                            ErrorCode.CONFLICT,
                                            "The previous index is no longer reusable.");
                                return j;
                            }
                            if (Set.of("PARSING", "EMBEDDING").contains(j.state())
                                    && j.startedAt() != null
                                    && j.startedAt().isAfter(Instant.now().minusSeconds(180)))
                                throw new ApiException(
                                        ErrorCode.CONFLICT, "Ingestion is already running.");
                            dao.claim(id);
                            if (expectedRevision != null)
                                audit.recordKey(
                                        admin,
                                        "KB_SOURCE_REPROCESS_REQUESTED",
                                        "knowledge_ingestion_job",
                                        id.toString(),
                                        Map.of("revision", expectedRevision));
                            return dao.job(kb, id, false);
                        });
        if (Set.of("READY", "PUBLISHED").contains(claimed.state())) return claimed;
        long generation = claimed.revision(), started = System.nanoTime();
        String[] phase = {"SOURCE_STORAGE"};
        try (var scope =
                SynExecutionScope.open(
                                Duration.ofSeconds(90), () -> active(admin, kb, id, generation))
                        .withDispatchAdmission(
                                new SynExecutionScope.DispatchAdmission() {
                                    public <T> T admit(java.util.function.Supplier<T> start) {
                                        return tx.execute(
                                                s -> {
                                                    lockAdmin(admin);
                                                    available(kb, true);
                                                    var current = dao.job(kb, id, true);
                                                    if (current.revision() != generation
                                                            || !Set.of("PARSING", "EMBEDDING")
                                                                    .contains(current.state()))
                                                        throw new SynGuidanceProvider
                                                                .ProviderException(
                                                                SynGuidanceProvider.FailureKind
                                                                        .CONSENT_REVOKED);
                                                    return start.get();
                                                });
                                    }
                                })) {
            try {
                var file =
                        files.download(
                                admin,
                                Long.parseLong(claimed.sourceFileId()),
                                "KNOWLEDGE_SOURCE",
                                Long.parseLong(claimed.sourceFileId()));
                phase[0] = "PARSING";
                var parsed =
                        parser.parse(
                                file.asset().originalName(),
                                file.asset().mimeType(),
                                file.bytes(),
                                claimed.metadata().path("title").asText());
                phase[0] = "SNAPSHOT_STORAGE";
                var extraction = parsed.snapshot(dao.inputHash(id));
                boolean stored =
                        tx.execute(
                                s -> {
                                    lockAdmin(admin);
                                    available(kb, true);
                                    return dao.snapshot(kb, id, generation, extraction);
                                });
                if (!stored) return dao.job(kb, id, false);
                String hash =
                        CoreValues.hash(
                                Map.of(
                                        "inputHash",
                                        dao.inputHash(id),
                                        "parser",
                                        parsed.profile(),
                                        "embedding",
                                        embedding.profile()));
                var previous = dao.existing(kb, claimed.assetKey(), hash, embedding.profile());
                var vectors = new ArrayList<double[]>();
                phase[0] = "EMBEDDING";
                if (previous.isEmpty()) {
                    if (!embedding.available())
                        throw new KnowledgeParser.ParseFailure("EMBEDDING_UNAVAILABLE");
                    if (!dao.embedding(id, generation, embedding.profile()))
                        return dao.job(kb, id, false);
                    for (var c : parsed.chunks()) {
                        SynExecutionScope.checkExternal();
                        var v =
                                embedding.embed(
                                        claimed.metadata().path("title").asText() + "\n" + c.text(),
                                        false);
                        KnowledgeStudioDao.vector(v);
                        vectors.add(v);
                    }
                }
                phase[0] = "STAGING_STORAGE";
                return tx.execute(
                        s -> {
                            lockAdmin(admin);
                            available(kb, true);
                            var current = dao.job(kb, id, true);
                            if (current.revision() != generation
                                    || !Set.of("PARSING", "EMBEDDING").contains(current.state()))
                                return current;
                            long version =
                                    previous.orElseGet(
                                            () ->
                                                    dao.stage(
                                                            claimed,
                                                            hash,
                                                            embedding.profile(),
                                                            parsed.chunks(),
                                                            vectors,
                                                            extraction));
                            var saved = dao.version(kb, claimed.assetKey(), version, true);
                            if ("RETIRED".equals(saved.status()))
                                throw new ApiException(
                                        ErrorCode.CONFLICT,
                                        "Retired content requires a new source version.");
                            if (previous.isPresent()) {
                                var storedChunks = dao.chunks(version);
                                boolean matches = storedChunks.size() == parsed.chunks().size();
                                for (int i = 0; matches && i < storedChunks.size(); i++) {
                                    var storedChunk = storedChunks.get(i);
                                    var parsedChunk = parsed.chunks().get(i);
                                    matches =
                                            storedChunk.text().equals(parsedChunk.text())
                                                    && storedChunk
                                                            .section()
                                                            .equals(parsedChunk.section())
                                                    && storedChunk
                                                            .sourceLocator()
                                                            .equals(parsedChunk.locator());
                                }
                                if (!matches)
                                    throw new ApiException(
                                            ErrorCode.CONFLICT,
                                            "The previous index is no longer reusable.");
                            }
                            var metrics =
                                    new LinkedHashMap<String, Object>(
                                            pricing.enrich(SynExecutionScope.trace()));
                            metrics.put("stage", "COMPLETE");
                            metrics.put("outcome", previous.isPresent() ? "UNCHANGED" : "STAGED");
                            metrics.put("generation", generation);
                            metrics.put("latencyMs", (System.nanoTime() - started) / 1_000_000);
                            dao.finish(
                                    id,
                                    generation,
                                    "PUBLISHED".equals(saved.status()) ? "PUBLISHED" : "READY",
                                    version,
                                    null,
                                    metrics);
                            return dao.job(kb, id, false);
                        });
            } catch (RuntimeException failure) {
                String error =
                        failure instanceof KnowledgeParser.ParseFailure
                                ? failure.getMessage()
                                : failure instanceof SynGuidanceProvider.ProviderException p
                                        ? p.kind().name()
                                        : "INGESTION_FAILED";
                var metrics =
                        new LinkedHashMap<String, Object>(
                                pricing.enrich(SynExecutionScope.trace()));
                metrics.put("stage", phase[0]);
                metrics.put("errorCode", error);
                metrics.put("generation", generation);
                metrics.put("latencyMs", (System.nanoTime() - started) / 1_000_000);
                tx.executeWithoutResult(
                        s -> {
                            var current = dao.job(kb, id, true);
                            if (current.revision() == generation
                                    && Set.of("PARSING", "EMBEDDING").contains(current.state()))
                                dao.finish(id, generation, "FAILED", null, error, metrics);
                            else {
                                metrics.put(
                                        "outcome",
                                        "CANCELLED".equals(current.state())
                                                ? "CANCELLED"
                                                : "SUPERSEDED");
                                dao.recordAttempt(id, generation, metrics);
                            }
                        });
                return dao.job(kb, id, false);
            } finally {
                // A cancelled/superseded worker cannot stage content, but its actual resource
                // observations still belong to that attempt. Never overwrite a newer lease.
                var observed =
                        new LinkedHashMap<String, Object>(
                                pricing.enrich(SynExecutionScope.trace()));
                observed.put("generation", generation);
                observed.put("latencyMs", (System.nanoTime() - started) / 1_000_000);
                tx.executeWithoutResult(s -> dao.recordAttempt(id, generation, observed));
            }
        }
    }

    public Job cancel(long admin, UUID kb, UUID id) {
        return tx.execute(
                s -> {
                    lockAdmin(admin);
                    dao.base(kb, true);
                    dao.job(kb, id, true);
                    dao.cancel(id);
                    return dao.job(kb, id, false);
                });
    }

    public List<AssetVersion> versions(long admin, UUID kb) {
        admin(admin);
        dao.base(kb, false);
        return dao.versions(kb);
    }

    public AssetVersion version(long admin, UUID kb, String key, long version) {
        admin(admin);
        dao.base(kb, false);
        var v = dao.version(kb, key, version, false);
        return new AssetVersion(
                v.id(),
                v.documentId(),
                v.knowledgeBaseId(),
                v.assetKey(),
                v.versionNumber(),
                v.title(),
                v.status(),
                v.contentHash(),
                v.embeddingProfile(),
                v.mimeType(),
                v.sourceFileId(),
                v.sourceUri(),
                v.license(),
                v.reviewer(),
                v.language(),
                v.provenance(),
                v.publishedAt(),
                dao.chunks(version));
    }

    public AssetVersion publish(
            long admin, UUID kb, String key, long version, long revision, boolean confirmed) {
        if (!confirmed) CoreValues.invalid("Explicit source publication confirmation is required.");
        return tx.execute(
                s -> {
                    lockAdmin(admin);
                    var b = available(kb, true);
                    CoreValues.match(revision, b.revision());
                    var v = dao.version(kb, key, version, true);
                    if ("RETIRED".equals(v.status()))
                        throw new ApiException(
                                ErrorCode.CONFLICT, "Retired source cannot be republished.");
                    if (!"PUBLISHED".equals(v.status())) dao.publish(v);
                    audit.record(
                            admin,
                            "KB_SOURCE_PUBLISHED",
                            "knowledge_versions",
                            version,
                            Map.of("hash", v.contentHash()));
                    return version(admin, kb, key, version);
                });
    }

    public AssetVersion retire(long admin, UUID kb, String key, long version) {
        return tx.execute(
                s -> {
                    lockAdmin(admin);
                    dao.base(kb, true);
                    var v = dao.version(kb, key, version, true);
                    if (!"RETIRED".equals(v.status())) dao.retire(v);
                    return version(admin, kb, key, version);
                });
    }

    public Passage source(long admin, UUID kb, long chunk) {
        admin(admin);
        available(kb, false);
        return dao.source(kb, chunk)
                .orElseThrow(() -> new NotFoundException("Published source unavailable."));
    }

    private void reader(long actor, UUID configuration) {
        var who = actors.requireId(actor);
        if ("ADMIN".equals(who.role())) return;
        if (!"STUDENT".equals(who.role()) || configuration == null)
            throw new NotFoundException("Knowledge scope unavailable.");
        var c = registry.configuration(configuration);
        if (!registry.assignedConfiguration(actor, UUID.fromString(c.agentId()), configuration))
            throw new NotFoundException("Knowledge scope unavailable.");
    }

    public void requireAvailable(long admin, UUID configuration, Set<UUID> bases) {
        reader(admin, configuration);
        if (configuration != null && !dao.grants(configuration).containsAll(bases))
            throw new ApiException(
                    ErrorCode.FORBIDDEN,
                    "Configuration does not grant the requested knowledge scope.");
        for (var kb : bases) available(kb, false);
    }

    public void lockAvailable(
            long admin, UUID configuration, Set<UUID> bases, List<Passage> evidence) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive())
            throw new IllegalStateException("Authority locking requires a caller transaction.");
        reader(admin, configuration);
        for (var kb : bases.stream().sorted().toList()) available(kb, true);
        requireAvailable(admin, configuration, bases);
        for (var p : evidence) {
            var current =
                    dao.lockSource(
                                    UUID.fromString(p.knowledgeBaseId()),
                                    Long.parseLong(p.chunkId()))
                            .orElseThrow(
                                    () -> new NotFoundException("Published source unavailable."));
            if (!current.versionId().equals(p.versionId()))
                throw new NotFoundException("Published source unavailable.");
        }
    }

    public List<Passage> search(
            long admin,
            UUID configuration,
            Set<UUID> bases,
            String query,
            int topK,
            boolean useEmbedding) {
        requireAvailable(admin, configuration, bases);
        String q = KnowledgeSourcePolicy.query(query);
        int k = KnowledgeSourcePolicy.topK(topK);
        double[] vector = null;
        if (useEmbedding && embedding.available()) {
            try {
                SynExecutionScope.checkExternal();
                vector = embedding.embed(q, true);
            } catch (RuntimeException unavailable) {
                vector = null;
            }
        }
        var result = new ArrayList<Passage>();
        for (var kb : bases) {
            var candidates =
                    dao.candidates(kb, HybridRetrievalPolicy.query(q), vector, embedding.profile());
            result.addAll(
                    edu.hcmute.careersim.guidance.knowledge.domain.KnowledgeRanking.merge(
                            candidates.lexical(), candidates.vector(), k, vector != null));
        }
        requireAvailable(admin, configuration, bases);
        var visible =
                result.stream()
                        .filter(
                                p ->
                                        dao.source(
                                                        UUID.fromString(p.knowledgeBaseId()),
                                                        Long.parseLong(p.chunkId()))
                                                .isPresent())
                        .sorted(
                                Comparator.comparingDouble(Passage::score)
                                        .reversed()
                                        .thenComparingLong(p -> Long.parseLong(p.chunkId())))
                        .limit(k)
                        .toList();
        int size = 0;
        var bounded = new ArrayList<Passage>();
        for (var p : visible) {
            if ((size += p.text().length()) > 12000) break;
            bounded.add(p);
        }
        return List.copyOf(bounded);
    }

    public boolean evidenceAvailable(long admin, UUID configuration, List<Passage> evidence) {
        try {
            var bases = new HashSet<UUID>();
            for (var p : evidence) bases.add(UUID.fromString(p.knowledgeBaseId()));
            requireAvailable(admin, configuration, bases);
            return evidence.stream()
                    .allMatch(
                            p ->
                                    dao.source(
                                                    UUID.fromString(p.knowledgeBaseId()),
                                                    Long.parseLong(p.chunkId()))
                                            .filter(
                                                    now ->
                                                            now.versionId().equals(p.versionId())
                                                                    && now.text().equals(p.text()))
                                            .isPresent());
        } catch (ApiException denied) {
            return false;
        }
    }
}
