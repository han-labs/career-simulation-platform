package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.*;
import edu.hcmute.careersim.guidance.knowledge.service.KnowledgeStudioService;
import edu.hcmute.careersim.guidance.service.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.agent-platform-enabled=true",
            "app.core-v2-enabled=true",
            "app.legacy-write-enabled=false",
            "app.ai.provider=disabled",
            "app.syn.cleanup-enabled=false",
            "server.servlet.session.cookie.secure=false"
        })
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentPlatformIntegrationTest {
    static final String
            FACT = "Synthetic backend practice includes API triage and careful validation.",
            MODEL = "gemini/synthetic-agent";

    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:0.8.2-pg17");

    @TempDir static Path storage;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.user", PG::getUsername);
        r.add("spring.flyway.password", PG::getPassword);
        r.add("app.storage.root", () -> storage.toString());
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired KnowledgeStudioService knowledge;
    @Autowired AgentRegistryService registry;
    @Autowired ExecutionService executions;
    @MockitoBean EmbeddingProvider embedding;
    @MockitoBean AgentModelProvider model;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired edu.hcmute.careersim.identity.service.BrowserAuthentication auth;
    @Autowired edu.hcmute.careersim.guidance.dao.KnowledgeDao legacy;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    long admin, student;

    jakarta.servlet.http.Cookie cookie(long id) {
        return new jakarta.servlet.http.Cookie("CAREERSIM_AUTH", auth.issue(id).value());
    }

    edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Test query(
            UUID request, boolean retrieval) {
        return new edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Test(
                request,
                "Synthetic backend practice",
                retrieval,
                5,
                retrieval ? null : MODEL,
                null);
    }

    edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Job upload(
            UUID kb, UUID request, String text) {
        return knowledge.upload(
                admin,
                kb,
                new Upload(
                        "guide",
                        "Synthetic guide",
                        "Original fixture",
                        null,
                        "EN",
                        "BACKEND",
                        true,
                        request),
                "guide.txt",
                "text/plain",
                text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    edu.hcmute.careersim.guidance.knowledge.dao.KnowledgeStudioDao studioDao;

    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired AgentUsageQuery agentUsage;

    @Test
    void kbTestUsageCountsExecutionsNotIngestionOrReloadAndPreservesUnknownMetrics() {
        var from = java.time.Instant.now().minusSeconds(3600);
        var to = java.time.Instant.now().plusSeconds(300);
        long before = usageCount(agentUsage.query(admin, from, to, "KB_TEST"));
        UUID kb = publishedKb();
        UUID request = UUID.randomUUID();
        clearInvocations(model);
        var retrieval = terminal(executions.test(admin, kb, query(request, true)));
        assertThat(retrieval.costStatus()).isEqualTo("NOT_APPLICABLE");
        assertThat(retrieval.inputTokens()).isNull();
        assertThat(retrieval.outputTokens()).isNull();
        verify(model, never())
                .decide(anyString(), anyString(), anyString(), anyList(), anyBoolean());
        assertThat(executions.test(admin, kb, query(request, true)).id()).isEqualTo(retrieval.id());
        executions.read(admin, UUID.fromString(retrieval.id()));
        assertThatThrownBy(
                        () ->
                                executions.test(
                                        admin,
                                        kb,
                                        new edu.hcmute.careersim.guidance.knowledge.dto
                                                .KnowledgeContracts.Test(
                                                request, "Conflicting input", true, 5, null, null)))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenThrow(
                        new SynGuidanceProvider.ProviderException(
                                SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT));
        var failed = terminal(executions.test(admin, kb, query(UUID.randomUUID(), false)));
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.estimatedCost()).isNull();
        assertThat(failed.toolStepsAndSources().toString()).contains(FACT);
        var ingestion = upload(kb, UUID.randomUUID(), "Synthetic unrelated ingestion");
        knowledge.retry(admin, kb, UUID.fromString(ingestion.id()));
        var summary = agentUsage.query(admin, from, to, "KB_TEST");
        assertThat(usageCount(summary)).isEqualTo(before + 2);
        assertThat(summary.toString())
                .contains(
                        "KB_TEST",
                        "FAILED",
                        "SUCCEEDED",
                        "latency_samples",
                        "not_applicable_executions")
                .doesNotContain(
                        FACT,
                        "extractionSnapshot",
                        "Synthetic unrelated ingestion",
                        "inputSnapshot",
                        "citations");
    }

    private long usageCount(Map<String, Object> summary) {
        return ((List<?>) summary.get("groups"))
                .stream()
                        .mapToLong(
                                row -> ((Number) ((Map<?, ?>) row).get("executions")).longValue())
                        .sum();
    }

    @Test
    void extractionSurvivesEmbeddingFailureReloadAndRetryWithoutLeakingIntoLists() {
        UUID kb = UUID.fromString(knowledge.create(admin, new BaseInput("Snapshot KB", null)).id());
        var job = upload(kb, UUID.randomUUID(), "Khám phá 🔎\n\n" + FACT);
        when(embedding.embed(anyString(), anyBoolean()))
                .thenAnswer(
                        call -> {
                            var saved = knowledge.job(admin, kb, UUID.fromString(job.id()));
                            assertThat(
                                            saved.attempts()
                                                    .get(0)
                                                    .path("extractionSnapshot")
                                                    .path("blocks")
                                                    .size())
                                    .isEqualTo(2);
                            throw new SynGuidanceProvider.ProviderException(
                                    SynGuidanceProvider.FailureKind.PROVIDER_UNAVAILABLE);
                        });
        var failed = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        assertThat(failed.state()).isEqualTo("FAILED");
        assertThat(failed.attempts()).hasSize(1);
        assertThat(failed.attempts().get(0).path("stage").asText()).isEqualTo("EMBEDDING");
        var extraction = failed.attempts().get(0).path("extractionSnapshot");
        assertThat(extraction.path("blocks").get(0).path("text").asText()).isEqualTo("Khám phá 🔎");
        assertThat(knowledge.jobs(admin, kb).get(0).attempts().toString())
                .doesNotContain("extractionSnapshot", FACT);
        reset(embedding);
        when(embedding.available()).thenReturn(true);
        when(embedding.profile()).thenReturn("synthetic:768:v1");
        var vector = new double[768];
        vector[0] = 1;
        when(embedding.embed(anyString(), anyBoolean())).thenReturn(vector);
        var ready = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        assertThat(ready.state()).isEqualTo("READY");
        assertThat(ready.attempts()).hasSize(2);
        assertThat(ready.attempts().get(0).path("extractionSnapshot")).isEqualTo(extraction);
        assertThat(ready.attempts().get(1).path("stage").asText()).isEqualTo("COMPLETE");
        var version =
                knowledge.version(admin, kb, "guide", Long.parseLong(ready.producedVersionId()));
        assertThat(version.provenance().path("extractionSnapshot"))
                .isEqualTo(ready.attempts().get(1).path("extractionSnapshot"));
        assertThat(knowledge.versions(admin, kb).get(0).provenance().has("extractionSnapshot"))
                .isFalse();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE knowledge_asset_metadata SET locator_metadata=jsonb_set(locator_metadata,'{extractionSnapshot,representationVersion}','2') WHERE version_id=?",
                                        Long.parseLong(version.id())))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void snapshotStorageFailureStopsBeforeEmbeddingAndReportsItsStage() {
        UUID kb =
                UUID.fromString(knowledge.create(admin, new BaseInput("Storage fault", null)).id());
        var job = upload(kb, UUID.randomUUID(), FACT);
        doThrow(
                        new org.springframework.dao.DataAccessResourceFailureException(
                                "synthetic storage failure"))
                .when(studioDao)
                .snapshot(eq(kb), eq(UUID.fromString(job.id())), anyLong(), any());
        clearInvocations(embedding);
        var failed = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        assertThat(failed.state()).isEqualTo("FAILED");
        assertThat(failed.attempts().get(0).path("stage").asText()).isEqualTo("SNAPSHOT_STORAGE");
        assertThat(failed.attempts().get(0).has("extractionSnapshot")).isFalse();
        verify(embedding, never()).embed(anyString(), anyBoolean());
        assertThat(knowledge.versions(admin, kb)).isEmpty();
    }

    @Test
    void attemptSnapshotsAreImmutableFencedAndMergeLateUsageIntoTheirOwnGeneration() {
        UUID kb =
                UUID.fromString(
                        knowledge.create(admin, new BaseInput("Generation guards", null)).id());
        var job = upload(kb, UUID.randomUUID(), FACT);
        UUID id = UUID.fromString(job.id());
        var tx =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        long gen =
                tx.execute(
                        t -> {
                            studioDao.claim(id);
                            return studioDao.job(kb, id, false).revision();
                        });
        var snapshot = CoreValues.tree(Map.of("representationVersion", 1, "blocks", List.of(FACT)));
        tx.executeWithoutResult(
                t -> assertThat(studioDao.snapshot(kb, id, gen, snapshot)).isTrue());
        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        t ->
                                                studioDao.snapshot(
                                                        kb,
                                                        id,
                                                        gen,
                                                        CoreValues.tree(
                                                                Map.of("different", true)))))
                .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("Attempt extraction is immutable");
        knowledge.cancel(admin, kb, id);
        long next =
                tx.execute(
                        t -> {
                            studioDao.claim(id);
                            return studioDao.job(kb, id, false).revision();
                        });
        tx.executeWithoutResult(
                t -> {
                    assertThat(studioDao.snapshot(kb, id, gen, snapshot)).isFalse();
                    studioDao.recordAttempt(id, gen, Map.of("generation", gen, "actualUsage", 7));
                    assertThat(studioDao.snapshot(kb, id, next, snapshot)).isTrue();
                });
        var history = knowledge.job(admin, kb, id).attempts();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).path("actualUsage").asInt()).isEqualTo(7);
        assertThat(history.get(0).path("extractionSnapshot")).isEqualTo(snapshot);
        assertThat(history.get(1).has("actualUsage")).isFalse();
    }

    @Test
    void reindexAIsUnchangedWithoutEmbeddingAndRetiredVersionsCannotResurrect() {
        UUID kb = UUID.fromString(knowledge.create(admin, new BaseInput("Reindex A", null)).id());
        var first = upload(kb, UUID.randomUUID(), FACT);
        var ready = knowledge.retry(admin, kb, UUID.fromString(first.id()));
        knowledge.publish(
                admin,
                kb,
                "guide",
                Long.parseLong(ready.producedVersionId()),
                knowledge.get(admin, kb).revision(),
                true);
        clearInvocations(embedding);
        var duplicate = upload(kb, UUID.randomUUID(), FACT);
        var unchanged = knowledge.retry(admin, kb, UUID.fromString(duplicate.id()));
        assertThat(unchanged.producedVersionId()).isEqualTo(ready.producedVersionId());
        assertThat(unchanged.attempts().get(0).path("outcome").asText()).isEqualTo("UNCHANGED");
        assertThat(knowledge.versions(admin, kb)).hasSize(1);
        verify(embedding, never()).embed(anyString(), anyBoolean());
        knowledge.retire(admin, kb, "guide", Long.parseLong(ready.producedVersionId()));
        assertThat(knowledge.search(admin, null, Set.of(kb), FACT, 5, false)).isEmpty();
        long retiredChunk =
                Long.parseLong(
                        knowledge
                                .version(
                                        admin,
                                        kb,
                                        "guide",
                                        Long.parseLong(ready.producedVersionId()))
                                .chunks()
                                .get(0)
                                .chunkId());
        assertThatThrownBy(() -> knowledge.source(admin, kb, retiredChunk))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
        assertThatThrownBy(() -> knowledge.retry(admin, kb, UUID.fromString(duplicate.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        var replacement = upload(kb, UUID.randomUUID(), FACT);
        assertThat(knowledge.retry(admin, kb, UUID.fromString(replacement.id())).state())
                .isEqualTo("FAILED");
        assertThat(knowledge.versions(admin, kb))
                .allSatisfy(v -> assertThat(v.status()).isEqualTo("RETIRED"));
    }

    @Test
    void stagingAndPublicationFaultsRollBackWithoutReplacingOldSourcesOrExtraction() {
        UUID kb = publishedKb();
        var original = knowledge.versions(admin, kb).get(0);
        var job = upload(kb, UUID.randomUUID(), "A changed synthetic source.");
        doAnswer(
                        call -> {
                            call.callRealMethod();
                            throw new org.springframework.dao.DataIntegrityViolationException(
                                    "synthetic stage rollback");
                        })
                .when(studioDao)
                .stage(any(), anyString(), anyString(), anyList(), anyList(), any());
        var failed = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        assertThat(failed.state()).isEqualTo("FAILED");
        assertThat(failed.attempts().get(0).path("stage").asText()).isEqualTo("STAGING_STORAGE");
        assertThat(failed.attempts().get(0).has("extractionSnapshot")).isTrue();
        assertThat(knowledge.versions(admin, kb)).hasSize(1);
        doCallRealMethod()
                .when(studioDao)
                .stage(any(), anyString(), anyString(), anyList(), anyList(), any());
        var ready = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        long revision = knowledge.get(admin, kb).revision();
        knowledge.update(admin, kb, revision, new BaseInput("Other admin revision", "ACTIVE"));
        assertThatThrownBy(
                        () ->
                                knowledge.publish(
                                        admin,
                                        kb,
                                        "guide",
                                        Long.parseLong(ready.producedVersionId()),
                                        revision,
                                        true))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        doAnswer(
                        call -> {
                            call.callRealMethod();
                            throw new org.springframework.dao.DataIntegrityViolationException(
                                    "synthetic publication rollback");
                        })
                .when(studioDao)
                .publish(any());
        assertThatThrownBy(
                        () ->
                                knowledge.publish(
                                        admin,
                                        kb,
                                        "guide",
                                        Long.parseLong(ready.producedVersionId()),
                                        knowledge.get(admin, kb).revision(),
                                        true))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(
                        knowledge.versions(admin, kb).stream()
                                .filter(v -> v.status().equals("PUBLISHED"))
                                .findFirst()
                                .orElseThrow()
                                .id())
                .isEqualTo(original.id());
        assertThat(
                        knowledge
                                .version(
                                        admin,
                                        kb,
                                        "guide",
                                        Long.parseLong(ready.producedVersionId()))
                                .status())
                .isEqualTo("READY");
    }

    @BeforeEach
    void fixture() {
        String suffix = UUID.randomUUID().toString();
        admin =
                jdbc.queryForObject(
                        "INSERT INTO app_users(email,password_hash,display_name,role) VALUES (?,'synthetic-only','Synthetic reviewer','ADMINISTRATOR') RETURNING id",
                        Long.class,
                        "admin-" + suffix + "@example.test");
        student =
                jdbc.queryForObject(
                        "INSERT INTO app_users(email,password_hash,display_name,role) VALUES (?,'synthetic-only','Synthetic Student','STUDENT') RETURNING id",
                        Long.class,
                        "student-" + suffix + "@example.test");
        jdbc.update("INSERT INTO student_profiles(user_id) VALUES (?)", student);
        when(embedding.available()).thenReturn(true);
        when(embedding.profile()).thenReturn("synthetic:768:v1");
        when(embedding.embed(anyString(), anyBoolean()))
                .thenAnswer(
                        call -> {
                            double[] v = new double[768];
                            v[0] = 1;
                            return v;
                        });
        when(model.models())
                .thenReturn(
                        List.of(
                                new AgentModelProvider.Model(
                                        MODEL, "gemini", "synthetic-agent", true)));
        when(model.modelKeys()).thenReturn(Set.of(MODEL));
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenAnswer(
                        call -> {
                            SynExecutionScope.recordUsage("gemini", "synthetic-agent", 80L, 20L);
                            if (call.getArgument(4, Boolean.class))
                                return new AgentModelProvider.Decision(
                                        CoreValues.tree(
                                                Map.of("role", "model", "parts", List.of())),
                                        List.of(
                                                new AgentModelProvider.Call(
                                                        "synthetic-call",
                                                        "kb.search",
                                                        CoreValues.tree(
                                                                Map.of(
                                                                        "query",
                                                                        "Synthetic backend practice")))),
                                        null);
                            String quote = FACT;
                            String reference =
                                    jdbc.queryForObject(
                                            "SELECT 'K'||c.id FROM knowledge_chunks c JOIN knowledge_versions v ON v.id=c.version_id JOIN knowledge_documents d ON d.id=v.document_id JOIN knowledge_asset_metadata m ON m.version_id=v.id WHERE d.active_version_id=v.id AND m.publication_status='PUBLISHED' ORDER BY c.id DESC LIMIT 1",
                                            String.class);
                            return new AgentModelProvider.Decision(
                                    CoreValues.tree(Map.of()),
                                    List.of(),
                                    CoreValues.tree(
                                            Map.of(
                                                    "schemaVersion",
                                                    1,
                                                    "outcome",
                                                    "ANSWER",
                                                    "text",
                                                    "This source describes API practice, not professional competence.",
                                                    "citations",
                                                    List.of(
                                                            Map.of(
                                                                    "referenceId",
                                                                    reference,
                                                                    "quote",
                                                                    quote)),
                                                    "suggestions",
                                                    List.of())));
                        });
    }

    UUID publishedKb() {
        var b = knowledge.create(admin, new BaseInput("Synthetic KB", null));
        UUID kb = UUID.fromString(b.id());
        var job =
                knowledge.upload(
                        admin,
                        kb,
                        new Upload(
                                "guide",
                                "Synthetic guide",
                                "Original synthetic test fixture",
                                null,
                                "EN",
                                "BACKEND",
                                true,
                                UUID.randomUUID()),
                        "guide.txt",
                        "text/plain",
                        FACT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var ready = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        assertThat(ready.state()).isEqualTo("READY");
        knowledge.publish(
                admin,
                kb,
                "guide",
                Long.parseLong(ready.producedVersionId()),
                knowledge.get(admin, kb).revision(),
                true);
        return kb;
    }

    Execution terminal(Execution e) {
        var id = UUID.fromString(e.id());
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(
                        () ->
                                Set.of("SUCCEEDED", "FAILED", "REFUSED", "CANCELLED")
                                        .contains(executions.read(admin, id).status()));
        return executions.read(admin, id);
    }

    ConfigurationInput config(UUID kb) {
        return new ConfigurationInput(
                MODEL,
                "Explain authorized career practice without making personal judgments.",
                null,
                CoreValues.tree(
                        Map.of(
                                "enabled",
                                List.of("kb.search"),
                                "maxRounds",
                                2,
                                "maxToolCalls",
                                8,
                                "topK",
                                5,
                                "timeoutSeconds",
                                30)),
                CoreValues.tree(Map.of("enabled", false)),
                CoreValues.tree(
                        Map.of("schemaVersion", 1, "grounding", "KB_ONLY", "maxCharacters", 4000)),
                List.of(kb));
    }

    @Test
    void standaloneKbTestPersistsEvidenceWithoutInventedSession() {
        int sessionsBefore =
                jdbc.queryForObject("SELECT count(*) FROM agent_session", Integer.class);
        UUID kb = publishedKb();
        var e =
                terminal(
                        executions.test(
                                admin,
                                kb,
                                new edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts
                                        .Test(
                                        UUID.randomUUID(),
                                        "Synthetic backend practice",
                                        false,
                                        5,
                                        MODEL,
                                        null)));
        assertThat(e.status()).isEqualTo("SUCCEEDED");
        assertThat(e.executionMode()).isEqualTo("KB_TEST");
        assertThat(e.sessionId()).isNull();
        assertThat(e.configurationId()).isNull();
        assertThat(e.inputInteractionId()).isNull();
        assertThat(e.inputTokens()).isEqualTo(80);
        assertThat(e.outputTokens()).isEqualTo(20);
        assertThat(e.estimatedCost()).isNull();
        assertThat(e.costStatus()).isEqualTo("UNKNOWN");
        assertThat(e.toolStepsAndSources().toString()).contains(FACT, "SERVER_RETRIEVAL");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agent_session", Integer.class))
                .isEqualTo(sessionsBefore);
    }

    @Test
    void exactSavedPreviewPublishesAndExplicitAssignmentControlsVisibility() {
        UUID kb = publishedKb();
        var a =
                registry.create(
                        admin, new AgentInput("Synthetic Agent", "Public exploration", null));
        UUID aid = UUID.fromString(a.id());
        var c = registry.save(admin, aid, a.revision(), config(kb));
        UUID cid = UUID.fromString(c.id());
        verify(model, never())
                .decide(anyString(), anyString(), anyString(), anyList(), anyBoolean());
        assertThatThrownBy(
                        () ->
                                registry.publish(
                                        admin, aid, cid, registry.get(admin, aid).revision(), true))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        var preview =
                terminal(
                        executions.preview(
                                admin,
                                aid,
                                cid,
                                new Preview(UUID.randomUUID(), "Synthetic backend practice")));
        assertThat(preview.status()).isEqualTo("SUCCEEDED");
        assertThat(preview.executionMode()).isEqualTo("ADMIN_PREVIEW");
        assertThat(preview.configurationHash()).isEqualTo(c.contentHash());
        assertThat(preview.sessionId()).isNull();
        assertThat(preview.toolStepsAndSources().toString()).contains("ACTUAL_NATIVE_CALL");
        registry.publish(admin, aid, cid, registry.get(admin, aid).revision(), true);
        assertThat(registry.assigned(student)).isEmpty();
        var assignment = registry.assign(admin, aid, student);
        assertThat(registry.assign(admin, aid, student).id()).isEqualTo(assignment.id());
        assertThat(registry.assigned(student)).hasSize(1);
        registry.revoke(admin, aid, UUID.fromString(assignment.id()));
        assertThat(registry.assigned(student)).isEmpty();
        assertThatThrownBy(() -> registry.requireAssignedConfiguration(student, aid, cid))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
    }

    @Test
    void stagedFailuresAndCancellationNeverReplacePublishedEvidenceAndRetriesDeduplicate() {
        UUID kb = publishedKb();
        var listed = knowledge.versions(admin, kb).get(0);
        var prior = knowledge.version(admin, kb, listed.assetKey(), Long.parseLong(listed.id()));
        UUID request = UUID.randomUUID();
        var job = upload(kb, request, "Changed synthetic content");
        assertThat(upload(kb, request, "Changed synthetic content").id()).isEqualTo(job.id());
        assertThatThrownBy(() -> upload(kb, request, "Different payload"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        when(embedding.embed(anyString(), anyBoolean()))
                .thenThrow(new IllegalStateException("synthetic provider down"));
        var failed = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        assertThat(failed.state()).isEqualTo("FAILED");
        assertThat(failed.producedVersionId()).isNull();
        assertThat(
                        knowledge
                                .source(admin, kb, Long.parseLong(prior.chunks().get(0).chunkId()))
                                .text())
                .contains(FACT);
        knowledge.cancel(admin, kb, UUID.fromString(job.id()));
        assertThat(knowledge.job(admin, kb, UUID.fromString(job.id())).state())
                .isEqualTo("CANCELLED");
        assertThat(knowledge.versions(admin, kb)).hasSize(1);
        var bad = upload(kb, UUID.randomUUID(), "```unclosed");
        assertThat(knowledge.retry(admin, kb, UUID.fromString(bad.id())).errorCode())
                .isEqualTo("UNCLOSED_CODE_FENCE");
        assertThat(knowledge.versions(admin, kb)).hasSize(1);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE knowledge_chunks SET content='tampered' WHERE id=?",
                                        Long.parseLong(prior.chunks().get(0).chunkId())))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void legacyAndScopedRetrievalStayIsolatedAndConfigScopesAreExplicit() {
        UUID kb = publishedKb(), other = publishedKb();
        double[] vector = new double[768];
        vector[0] = 1;
        String key = "legacy-" + UUID.randomUUID();
        legacy.publish(
                new edu.hcmute.careersim.guidance.dao.KnowledgeDao.Document(
                        key,
                        "Legacy fixture",
                        null,
                        "Original fixture",
                        "EN",
                        "BACKEND",
                        "Synthetic",
                        true,
                        FACT),
                "f".repeat(64),
                "synthetic:768:v1",
                List.of(
                        new edu.hcmute.careersim.guidance.dao.KnowledgeDao.EmbeddedChunk(
                                "Legacy", FACT, vector)));
        var chunks = legacy.lexical("backend & practice", 100);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks)
                .allSatisfy(
                        c ->
                                assertThat(
                                                jdbc.queryForObject(
                                                        "SELECT count(*) FROM knowledge_document_scope s JOIN knowledge_versions v ON v.document_id=s.document_id JOIN knowledge_chunks c ON c.version_id=v.id WHERE c.id=?",
                                                        Integer.class,
                                                        c.id()))
                                        .isZero());
        var a = registry.create(admin, new AgentInput("Scoped", "Public only", null));
        UUID aid = UUID.fromString(a.id());
        var c = registry.save(admin, aid, a.revision(), config(kb));
        UUID cid = UUID.fromString(c.id());
        assertThatThrownBy(() -> knowledge.search(admin, cid, Set.of(other), "backend", 5, false))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThatThrownBy(
                        () ->
                                knowledge.source(
                                        admin,
                                        other,
                                        knowledge
                                                .search(
                                                        admin,
                                                        null,
                                                        Set.of(kb),
                                                        "backend practice",
                                                        5,
                                                        false)
                                                .stream()
                                                .mapToLong(p -> Long.parseLong(p.chunkId()))
                                                .findFirst()
                                                .orElseThrow()))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
        assertThat(knowledge.search(admin, cid, Set.of(kb), "no-lexical-match-zzzz", 5, false))
                .isEmpty();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "DELETE FROM agent_config_kb_scope WHERE configuration_id=?",
                                        cid))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE agent_configuration SET prompt_template='changed' WHERE id=?",
                                        cid))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void retrievalOnlyIsDurableIdempotentAndUnknownGenerationUsageIsNotZero() {
        UUID kb = publishedKb(), request = UUID.randomUUID();
        var input = query(request, true);
        var one = terminal(executions.test(admin, kb, input));
        assertThat(one.status()).isEqualTo("SUCCEEDED");
        assertThat(one.inputTokens()).isNull();
        assertThat(one.estimatedCost()).isNull();
        assertThat(one.costStatus()).isEqualTo("NOT_APPLICABLE");
        assertThat(executions.test(admin, kb, input).id()).isEqualTo(one.id());
        verify(model, never())
                .decide(anyString(), anyString(), anyString(), anyList(), anyBoolean());
        assertThatThrownBy(() -> executions.test(admin, kb, query(request, false)))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenAnswer(
                        call ->
                                new AgentModelProvider.Decision(
                                        CoreValues.tree(Map.of()),
                                        List.of(),
                                        CoreValues.tree(
                                                Map.of(
                                                        "schemaVersion",
                                                        1,
                                                        "outcome",
                                                        "INSUFFICIENT_EVIDENCE",
                                                        "text",
                                                        "There is insufficient evidence.",
                                                        "citations",
                                                        List.of(),
                                                        "suggestions",
                                                        List.of()))));
        var answer = terminal(executions.test(admin, kb, query(UUID.randomUUID(), false)));
        assertThat(answer.status()).isEqualTo("REFUSED");
        assertThat(answer.inputTokens()).isNull();
        assertThat(answer.outputTokens()).isNull();
        assertThat(answer.costStatus()).isEqualTo("UNKNOWN");
        assertThat(answer.toolStepsAndSources().toString()).contains(FACT);
    }

    @Test
    void malformedOutputAndNativeUnknownToolAreBoundedAndNeverPublished() {
        UUID kb = publishedKb();
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(
                        new AgentModelProvider.Decision(
                                CoreValues.tree(Map.of()),
                                List.of(),
                                CoreValues.tree(Map.of("text", "invented output"))));
        var invalid = terminal(executions.test(admin, kb, query(UUID.randomUUID(), false)));
        assertThat(invalid.status()).isEqualTo("FAILED");
        assertThat(invalid.errorCode()).isEqualTo("MALFORMED_OUTPUT");
        assertThat(invalid.toolStepsAndSources().toString()).contains(FACT);
        var a = registry.create(admin, new AgentInput("Bounded", "Public", null));
        UUID aid = UUID.fromString(a.id());
        var c = registry.save(admin, aid, a.revision(), config(kb));
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(
                        new AgentModelProvider.Decision(
                                CoreValues.tree(Map.of()),
                                List.of(
                                        new AgentModelProvider.Call(
                                                "unknown-call",
                                                "write.private",
                                                CoreValues.tree(Map.of()))),
                                null));
        var denied =
                terminal(
                        executions.preview(
                                admin,
                                aid,
                                UUID.fromString(c.id()),
                                new Preview(UUID.randomUUID(), "Synthetic backend practice")));
        assertThat(denied.status()).isEqualTo("FAILED");
        assertThat(denied.errorCode()).isEqualTo("ITERATION_LIMIT");
        assertThat(denied.toolStepsAndSources().toString()).contains("DENIED", "write.private");
        assertThatThrownBy(
                        () ->
                                registry.publish(
                                        admin,
                                        aid,
                                        UUID.fromString(c.id()),
                                        registry.get(admin, aid).revision(),
                                        true))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @Test
    void cancelDuringProviderCallPreservesUsageButDiscardsLateAnswer() throws Exception {
        UUID kb = publishedKb();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenAnswer(
                        call -> {
                            entered.countDown();
                            assertThat(release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                    .isTrue();
                            SynExecutionScope.recordUsage("gemini", "synthetic-agent", 12L, 3L);
                            return new AgentModelProvider.Decision(
                                    CoreValues.tree(Map.of()),
                                    List.of(),
                                    CoreValues.tree(
                                            Map.of(
                                                    "schemaVersion",
                                                    1,
                                                    "outcome",
                                                    "REFUSED",
                                                    "text",
                                                    "Late answer must not publish.",
                                                    "citations",
                                                    List.of(),
                                                    "suggestions",
                                                    List.of())));
                        });
        var e = executions.test(admin, kb, query(UUID.randomUUID(), false));
        try {
            assertThat(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(executions.cancel(admin, UUID.fromString(e.id())).cancellationRequested())
                    .isTrue();
        } finally {
            release.countDown();
        }
        var cancelled = terminal(e);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.outputSnapshot()).isNull();
        assertThat(cancelled.inputTokens()).isEqualTo(12);
        assertThat(cancelled.outputTokens()).isEqualTo(3);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE agent_execution SET status='SUCCEEDED' WHERE id=?",
                                        UUID.fromString(e.id())))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void hidingKnowledgeDuringProviderCallBlocksLateCommitAndReplay() throws Exception {
        UUID kb = publishedKb();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenAnswer(
                        call -> {
                            entered.countDown();
                            release.await(10, java.util.concurrent.TimeUnit.SECONDS);
                            SynExecutionScope.recordUsage("gemini", "synthetic-agent", 7L, 2L);
                            return new AgentModelProvider.Decision(
                                    CoreValues.tree(Map.of()),
                                    List.of(),
                                    CoreValues.tree(
                                            Map.of(
                                                    "schemaVersion",
                                                    1,
                                                    "outcome",
                                                    "REFUSED",
                                                    "text",
                                                    "Do not replay hidden evidence.",
                                                    "citations",
                                                    List.of(),
                                                    "suggestions",
                                                    List.of())));
                        });
        var e = executions.test(admin, kb, query(UUID.randomUUID(), false));
        try {
            assertThat(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            knowledge.update(
                    admin,
                    kb,
                    knowledge.get(admin, kb).revision(),
                    new BaseInput("Hidden", "HIDDEN"));
        } finally {
            release.countDown();
        }
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(
                        () ->
                                "CANCELLED"
                                        .equals(
                                                jdbc.queryForObject(
                                                        "SELECT status FROM agent_execution WHERE id=?",
                                                        String.class,
                                                        UUID.fromString(e.id()))));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT output_snapshot IS NULL FROM agent_execution WHERE id=?",
                                Boolean.class,
                                UUID.fromString(e.id())))
                .isTrue();
        assertThatThrownBy(() -> executions.read(admin, UUID.fromString(e.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
    }

    @Test
    void oldPreviewCannotPublishNewConfigurationAndInactiveAgentIsUnavailable() {
        UUID kb = publishedKb();
        var a = registry.create(admin, new AgentInput("Versioned", "Public", null));
        UUID aid = UUID.fromString(a.id());
        var c1 = registry.save(admin, aid, a.revision(), config(kb));
        terminal(
                executions.preview(
                        admin,
                        aid,
                        UUID.fromString(c1.id()),
                        new Preview(UUID.randomUUID(), "Synthetic backend practice")));
        var input = config(kb);
        var c2 =
                registry.save(
                        admin,
                        aid,
                        registry.get(admin, aid).revision(),
                        new ConfigurationInput(
                                input.modelKey(),
                                input.promptTemplate() + " Be concise.",
                                input.promptTemplateRef(),
                                input.toolPolicy(),
                                input.memoryPolicy(),
                                input.outputPolicy(),
                                input.knowledgeBaseIds()));
        assertThat(c2.contentHash()).isNotEqualTo(c1.contentHash());
        assertThatThrownBy(
                        () ->
                                registry.publish(
                                        admin,
                                        aid,
                                        UUID.fromString(c2.id()),
                                        registry.get(admin, aid).revision(),
                                        true))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThat(registry.configuration(admin, aid, UUID.fromString(c1.id())).promptTemplate())
                .isEqualTo(input.promptTemplate());
        registry.publish(
                admin, aid, UUID.fromString(c1.id()), registry.get(admin, aid).revision(), true);
        registry.assign(admin, aid, student);
        registry.archive(admin, aid, registry.get(admin, aid).revision());
        assertThat(registry.assigned(student)).isEmpty();
        assertThatThrownBy(
                        () ->
                                registry.requireAssignedConfiguration(
                                        student, aid, UUID.fromString(c1.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
    }

    @Test
    void browserContractsRequireCurrentActorRoleCsrfAndPrivateExecutionOwner() throws Exception {
        var root = "/v2/admin/knowledge-bases";
        var adminCookie = cookie(admin);
        var studentCookie = cookie(student);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(root))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isUnauthorized());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        root)
                                .cookie(studentCookie))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isForbidden());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        root)
                                .cookie(adminCookie)
                                .contentType("application/json")
                                .content("{\"name\":\"Synthetic\"}"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isForbidden());
        var response =
                mvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .post(root)
                                        .cookie(adminCookie)
                                        .with(
                                                org.springframework.security.test.web.servlet
                                                        .request
                                                        .SecurityMockMvcRequestPostProcessors
                                                        .csrf())
                                        .contentType("application/json")
                                        .content("{\"name\":\"Synthetic\"}"))
                        .andExpect(
                                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                        .status()
                                        .isOk())
                        .andReturn();
        UUID kb =
                UUID.fromString(
                        json.readTree(response.getResponse().getContentAsString())
                                .path("data")
                                .path("id")
                                .asText());
        var e = terminal(executions.test(admin, kb, query(UUID.randomUUID(), true)));
        long second =
                jdbc.queryForObject(
                        "INSERT INTO app_users(email,password_hash,display_name,role) VALUES (?,'synthetic','Other Admin','ADMINISTRATOR') RETURNING id",
                        Long.class,
                        UUID.randomUUID() + "@example.test");
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/v2/admin/agent-executions/" + e.id())
                                .cookie(cookie(second)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isNotFound());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/v2/agents")
                                .cookie(studentCookie))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .json("{\"data\":[]}"));
        jdbc.update("UPDATE app_users SET status='LOCKED' WHERE id=?", admin);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        root)
                                .cookie(adminCookie))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isForbidden());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"txt", "md", "pdf", "docx", "xlsx"})
    void eachFormatStagesPublishesAndRetrievesItsExactProvenance(String format) throws Exception {
        var source =
                edu.hcmute.careersim.guidance.knowledge.KnowledgeParserContractTest.fixture(format);
        var b = knowledge.create(admin, new BaseInput("Format fixture", null));
        UUID kb = UUID.fromString(b.id());
        var job =
                knowledge.upload(
                        admin,
                        kb,
                        new Upload(
                                "fixture",
                                "Reviewed fixture",
                                "Original synthetic fixture",
                                null,
                                "EN",
                                "BACKEND",
                                true,
                                UUID.randomUUID()),
                        "fixture." + format,
                        edu.hcmute.careersim.guidance.knowledge.KnowledgeParserContractTest.mime(
                                format),
                        source);
        var ready = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        assertThat(ready.state()).isEqualTo("READY");
        var v = knowledge.version(admin, kb, "fixture", Long.parseLong(ready.producedVersionId()));
        assertThat(v.chunks()).isNotEmpty();
        assertThat(v.reviewer()).isEqualTo("Synthetic reviewer");
        assertThat(knowledge.search(admin, null, Set.of(kb), "backend practice", 5, false))
                .isEmpty();
        knowledge.publish(
                admin,
                kb,
                "fixture",
                Long.parseLong(v.id()),
                knowledge.get(admin, kb).revision(),
                true);
        var found = knowledge.search(admin, null, Set.of(kb), "backend practice", 5, false);
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).sourceLocator().path("format").asText()).isEqualTo(format);
        assertThat(found.get(0).versionId()).isEqualTo(v.id());
        assertThat(knowledge.source(admin, kb, Long.parseLong(found.get(0).chunkId())).text())
                .isEqualTo(found.get(0).text());
    }

    @Test
    void modeSpecificDatabaseConstraintsRejectCrossSessionAndForeignActors() {
        UUID kb = publishedKb();
        var a = registry.create(admin, new AgentInput("Mode fixture", "Public", null));
        UUID aid = UUID.fromString(a.id());
        var c = registry.save(admin, aid, a.revision(), config(kb));
        UUID cid = UUID.fromString(c.id());
        terminal(
                executions.preview(
                        admin,
                        aid,
                        cid,
                        new Preview(UUID.randomUUID(), "Synthetic backend practice")));
        registry.publish(admin, aid, cid, registry.get(admin, aid).revision(), true);
        registry.assign(admin, aid, student);
        UUID s1 = UUID.randomUUID(),
                s2 = UUID.randomUUID(),
                message = UUID.randomUUID(),
                e = UUID.randomUUID();
        for (UUID session : List.of(s1, s2))
            jdbc.update(
                    "INSERT INTO agent_session(id,owner_user_id,agent_id,configuration_id,title) VALUES (?,?,?,?,'Synthetic schema fixture')",
                    session,
                    student,
                    aid,
                    cid);
        jdbc.update(
                "INSERT INTO interaction_record(id,session_id,owner_user_id,sequence_number,actor_type,content) VALUES (?,?,?,1,'USER','{\"text\":\"Synthetic input\"}')",
                message,
                s1,
                student);
        String insert =
                "INSERT INTO agent_execution(id,execution_mode,task_type,initiated_by_user_id,session_id,configuration_id,input_interaction_id,input_snapshot,request_id,request_hash,configuration_hash,privacy_version,context_epoch) VALUES (?,'STUDENT_CHAT','CHAT',?,?,?,?,'{}',?,repeat('a',64),?,0,0)";
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        insert,
                                        UUID.randomUUID(),
                                        admin,
                                        s1,
                                        cid,
                                        message,
                                        UUID.randomUUID(),
                                        c.contentHash()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        insert,
                                        UUID.randomUUID(),
                                        student,
                                        s2,
                                        cid,
                                        message,
                                        UUID.randomUUID(),
                                        c.contentHash()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        jdbc.update(insert, e, student, s1, cid, message, UUID.randomUUID(), c.contentHash());
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        insert,
                                        UUID.randomUUID(),
                                        student,
                                        s1,
                                        cid,
                                        message,
                                        UUID.randomUUID(),
                                        c.contentHash()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO interaction_record(id,session_id,owner_user_id,sequence_number,actor_type,content,execution_id) VALUES (?,?,?,1,'ASSISTANT','{}',?)",
                                        UUID.randomUUID(),
                                        s2,
                                        student,
                                        e))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO agent_execution(id,execution_mode,task_type,initiated_by_user_id,knowledge_base_id,input_snapshot,request_id,request_hash,privacy_version,input_tokens,cost_status) VALUES (?,'KB_TEST','KB_QUERY',?,?, '{}',?,repeat('b',64),0,0,'NOT_APPLICABLE')",
                                        UUID.randomUUID(),
                                        admin,
                                        kb,
                                        UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO agent_execution(id,execution_mode,task_type,initiated_by_user_id,input_snapshot,request_id,request_hash,privacy_version) VALUES (?,'ADMIN_PREVIEW','CHAT',?,'{}',?,repeat('c',64),0)",
                                        UUID.randomUUID(),
                                        admin,
                                        UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        jdbc.update(
                "UPDATE agent_execution SET status='CANCELLED',finished_at=CURRENT_TIMESTAMP WHERE id=?",
                e);
    }

    java.net.http.HttpClient browser(long actor, String origin) {
        var cookies = new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
        var c = new java.net.HttpCookie("CAREERSIM_AUTH", auth.issue(actor).value());
        c.setPath("/api");
        c.setVersion(0);
        cookies.getCookieStore().add(java.net.URI.create(origin), c);
        return java.net.http.HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    com.fasterxml.jackson.databind.JsonNode gateway(
            java.net.http.HttpClient http,
            String origin,
            String method,
            String path,
            Object body,
            Long revision,
            int expected)
            throws Exception {
        var request =
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(origin + "/api" + path))
                        .timeout(Duration.ofSeconds(30));
        if (!method.equals("GET")) {
            var csrf =
                    http.send(
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(origin + "/api/v2/auth/csrf"))
                                    .GET()
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(csrf.statusCode()).isEqualTo(200);
            request.header(
                    "X-CSRF-TOKEN", json.readTree(csrf.body()).path("data").path("token").asText());
        }
        if (revision != null) request.header("If-Match", Long.toString(revision));
        if (body == null) request.method(method, java.net.http.HttpRequest.BodyPublishers.noBody());
        else
            request.header("Content-Type", "application/json")
                    .method(
                            method,
                            java.net.http.HttpRequest.BodyPublishers.ofString(
                                    json.writeValueAsString(body)));
        var response =
                http.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as(method + " " + path + ": " + response.body())
                .isEqualTo(expected);
        return json.readTree(response.body()).path("data");
    }

    com.fasterxml.jackson.databind.JsonNode gatewayUpload(
            java.net.http.HttpClient http, String origin, UUID kb, String key, String text)
            throws Exception {
        var csrf = gateway(http, origin, "GET", "/v2/auth/csrf", null, null, 200);
        String boundary = "SyntheticBoundary" + UUID.randomUUID();
        String metadata =
                json.writeValueAsString(
                        new Upload(
                                key,
                                "Synthetic gateway source",
                                "Original fixture",
                                null,
                                "EN",
                                "BACKEND",
                                true,
                                UUID.randomUUID()));
        String body =
                "--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json\r\n\r\n"
                        + metadata
                        + "\r\n--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"guide.txt\"\r\nContent-Type: text/plain\r\n\r\n"
                        + text
                        + "\r\n--"
                        + boundary
                        + "--\r\n";
        var response =
                http.send(
                        java.net.http.HttpRequest.newBuilder(
                                        java.net.URI.create(
                                                origin
                                                        + "/api/v2/admin/knowledge-bases/"
                                                        + kb
                                                        + "/assets"))
                                .timeout(Duration.ofSeconds(20))
                                .header("X-CSRF-TOKEN", csrf.path("token").asText())
                                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                                .build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("Multipart through production Nginx").isEqualTo(200);
        return json.readTree(response.body()).path("data");
    }

    com.fasterxml.jackson.databind.JsonNode gatewayTerminal(
            java.net.http.HttpClient http, String origin, String id) {
        var result =
                new java.util.concurrent.atomic.AtomicReference<
                        com.fasterxml.jackson.databind.JsonNode>();
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .until(
                        () -> {
                            var e =
                                    gateway(
                                            http,
                                            origin,
                                            "GET",
                                            "/v2/admin/agent-executions/" + id,
                                            null,
                                            null,
                                            200);
                            result.set(e);
                            return Set.of("SUCCEEDED", "REFUSED", "FAILED", "CANCELLED")
                                    .contains(e.path("status").asText());
                        });
        return result.get();
    }

    @Test
    void realProductionGatewayKbBuilderAssignmentAndIsolatedBackupRestore() throws Exception {
        org.testcontainers.Testcontainers.exposeHostPorts(port);
        String config =
                java.nio.file.Files.readString(Path.of("../frontend/nginx.conf"))
                        .replace(
                                "http://backend:8080/",
                                "http://host.testcontainers.internal:" + port + "/");
        Path bundle = Path.of("../frontend/dist").toAbsolutePath().normalize();
        assertThat(java.nio.file.Files.exists(bundle.resolve("index.html"))).isTrue();
        try (var nginx =
                new org.testcontainers.containers.GenericContainer<>("nginx:1.29-alpine")
                        .withExposedPorts(80)
                        .withCopyToContainer(
                                org.testcontainers.images.builder.Transferable.of(config),
                                "/etc/nginx/conf.d/default.conf")
                        .withCopyFileToContainer(
                                org.testcontainers.utility.MountableFile.forHostPath(bundle),
                                "/usr/share/nginx/html")) {
            nginx.start();
            String origin = "http://localhost:" + nginx.getMappedPort(80);
            var http = browser(admin, origin);
            assertThat(
                            http.send(
                                            java.net.http.HttpRequest.newBuilder(
                                                            java.net.URI.create(
                                                                    origin + "/admin/knowledge"))
                                                    .GET()
                                                    .build(),
                                            java.net.http.HttpResponse.BodyHandlers.ofString())
                                    .body())
                    .contains("<div id=\"root\">");
            gateway(http, origin, "GET", "/health", null, null, 200);
            var b =
                    gateway(
                            http,
                            origin,
                            "POST",
                            "/v2/admin/knowledge-bases",
                            new BaseInput("Gateway fixture", null),
                            null,
                            200);
            UUID kb = UUID.fromString(b.path("id").asText());
            String root = "/v2/admin/knowledge-bases/" + kb;
            gatewayUpload(
                    http,
                    origin,
                    kb,
                    "large-staged",
                    " ".repeat(1100000)
                            + FACT); // proves >1 MiB survives both proxy and multipart limits; no
            // parser execution for this oversized text.
            var upload = gatewayUpload(http, origin, kb, "guide", FACT);
            var ready =
                    gateway(
                            http,
                            origin,
                            "POST",
                            root + "/ingestions/" + upload.path("id").asText() + "/retry",
                            Map.of(),
                            null,
                            200);
            assertThat(ready.path("state").asText()).isEqualTo("READY");
            String v = ready.path("producedVersionId").asText();
            var inspected =
                    gateway(
                            http,
                            origin,
                            "GET",
                            root + "/assets/guide/versions/" + v,
                            null,
                            null,
                            200);
            assertThat(inspected.path("chunks").toString()).contains(FACT, "sourceLocator");
            long revision =
                    gateway(http, origin, "GET", root, null, null, 200).path("revision").asLong();
            gateway(
                    http,
                    origin,
                    "POST",
                    root + "/assets/guide/versions/" + v + "/publish",
                    Map.of("confirmed", true),
                    revision,
                    200);
            var test =
                    gateway(
                            http,
                            origin,
                            "POST",
                            root + "/tests",
                            query(UUID.randomUUID(), false),
                            null,
                            202);
            var tested = gatewayTerminal(http, origin, test.path("id").asText());
            assertThat(tested.path("status").asText()).isEqualTo("SUCCEEDED");
            assertThat(tested.path("toolStepsAndSources").toString()).contains(FACT);
            var a =
                    gateway(
                            http,
                            origin,
                            "POST",
                            "/v2/admin/agents",
                            new AgentInput("Gateway Agent", "Public practice", null),
                            null,
                            200);
            String agentRoot = "/v2/admin/agents/" + a.path("id").asText();
            var c =
                    gateway(
                            http,
                            origin,
                            "POST",
                            agentRoot + "/configurations",
                            config(kb),
                            a.path("revision").asLong(),
                            200);
            var preview =
                    gateway(
                            http,
                            origin,
                            "POST",
                            agentRoot + "/configurations/" + c.path("id").asText() + "/previews",
                            new Preview(UUID.randomUUID(), "Synthetic backend practice"),
                            null,
                            202);
            var done = gatewayTerminal(http, origin, preview.path("id").asText());
            assertThat(done.path("status").asText()).isEqualTo("SUCCEEDED");
            assertThat(done.path("configurationHash").asText())
                    .isEqualTo(c.path("contentHash").asText());
            revision =
                    gateway(http, origin, "GET", agentRoot, null, null, 200)
                            .path("revision")
                            .asLong();
            gateway(
                    http,
                    origin,
                    "POST",
                    agentRoot + "/configurations/" + c.path("id").asText() + "/publish",
                    Map.of("confirmed", true),
                    revision,
                    200);
            var assignment =
                    gateway(
                            http,
                            origin,
                            "POST",
                            agentRoot + "/assignments",
                            Map.of("studentUserId", Long.toString(student)),
                            null,
                            200);
            var studentBrowser = browser(student, origin);
            var publicAgents =
                    gateway(studentBrowser, origin, "GET", "/v2/agents", null, null, 200);
            assertThat(publicAgents.toString())
                    .contains("Gateway Agent")
                    .doesNotContain("promptTemplate", "knowledgeBaseIds");
            gateway(studentBrowser, origin, "GET", root + "/assets", null, null, 403);
            gateway(
                    http,
                    origin,
                    "POST",
                    agentRoot + "/assignments/" + assignment.path("id").asText() + "/revoke",
                    Map.of(),
                    null,
                    200);
            assertThat(gateway(studentBrowser, origin, "GET", "/v2/agents", null, null, 200))
                    .isEmpty();
        }
        assertThat(
                        PG.execInContainer(
                                        "pg_dump",
                                        "-U",
                                        PG.getUsername(),
                                        "-d",
                                        PG.getDatabaseName(),
                                        "-Fc",
                                        "-f",
                                        "/tmp/p3-platform.dump")
                                .getExitCode())
                .isZero();
        assertThat(
                        PG.execInContainer("createdb", "-U", PG.getUsername(), "p3_restore")
                                .getExitCode())
                .isZero();
        assertThat(
                        PG.execInContainer(
                                        "pg_restore",
                                        "-U",
                                        PG.getUsername(),
                                        "-d",
                                        "p3_restore",
                                        "--exit-on-error",
                                        "/tmp/p3-platform.dump")
                                .getExitCode())
                .isZero();
        var restored =
                new JdbcTemplate(
                        new org.springframework.jdbc.datasource.DriverManagerDataSource(
                                PG.getJdbcUrl()
                                        .replace("/" + PG.getDatabaseName() + "?", "/p3_restore?"),
                                PG.getUsername(),
                                PG.getPassword()));
        for (String table :
                List.of(
                        "knowledge_chunks",
                        "knowledge_asset_metadata",
                        "agent_configuration",
                        "agent_config_kb_scope",
                        "agent_assignment",
                        "agent_execution",
                        "consent_grant")) {
            String fingerprint =
                    "SELECT md5(COALESCE(string_agg(data,E'\\n' ORDER BY data),'')) FROM (SELECT to_jsonb(t)::text data FROM public."
                            + table
                            + " t) q";
            assertThat(restored.queryForObject(fingerprint, String.class))
                    .as("Restore " + table)
                    .isEqualTo(jdbc.queryForObject(fingerprint, String.class));
        }
        assertThat(
                        restored.queryForObject(
                                "SELECT max(version::int) FROM flyway_schema_history",
                                Integer.class))
                .isEqualTo(
                        jdbc.queryForObject(
                                "SELECT max(version::int) FROM flyway_schema_history",
                                Integer.class));
    }

    @Test
    void cancelledIngestionCannotStageLateChunksButRetainsActualAttemptUsage() throws Exception {
        UUID kb = publishedKb();
        var job = upload(kb, UUID.randomUUID(), "Replacement synthetic source");
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(embedding.embed(anyString(), anyBoolean()))
                .thenAnswer(
                        call -> {
                            entered.countDown();
                            release.await(10, java.util.concurrent.TimeUnit.SECONDS);
                            SynExecutionScope.recordUsage(
                                    "gemini", "synthetic-embedding", 11L, null);
                            double[] v = new double[768];
                            v[0] = 1;
                            return v;
                        });
        var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var future = pool.submit(() -> knowledge.retry(admin, kb, UUID.fromString(job.id())));
            assertThat(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            knowledge.cancel(admin, kb, UUID.fromString(job.id()));
            release.countDown();
            assertThat(future.get(15, java.util.concurrent.TimeUnit.SECONDS).state())
                    .isEqualTo("CANCELLED");
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
        var stored = knowledge.job(admin, kb, UUID.fromString(job.id()));
        assertThat(stored.attempts()).hasSize(1);
        assertThat(stored.attempts().toString())
                .contains(
                        "inputTokens", "11", "extractionSnapshot", "Replacement synthetic source");
        assertThat(knowledge.versions(admin, kb)).hasSize(1);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {
                "PROVIDER_UNAVAILABLE",
                "TIMEOUT_OR_TRANSPORT",
                "MALFORMED_OUTPUT",
                "EMPTY_EVIDENCE"
            })
    void providerFailureRetainsRetrievedEvidenceAndHonestUnknownUsage(String kind) {
        UUID kb = publishedKb();
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenThrow(
                        new SynGuidanceProvider.ProviderException(
                                SynGuidanceProvider.FailureKind.valueOf(kind)));
        var e = terminal(executions.test(admin, kb, query(UUID.randomUUID(), false)));
        assertThat(e.status()).isEqualTo(kind.equals("EMPTY_EVIDENCE") ? "REFUSED" : "FAILED");
        assertThat(e.errorCode()).isEqualTo(kind);
        assertThat(e.toolStepsAndSources().toString()).contains(FACT);
        assertThat(e.inputTokens()).isNull();
        assertThat(e.estimatedCost()).isNull();
        assertThat(e.costStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    void deploymentGatewayAcceptsLargePrivateMultipartWithoutRedirect() throws Exception {
        org.testcontainers.Testcontainers.exposeHostPorts(port);
        String config =
                java.nio.file.Files.readString(Path.of("../deployment/nginx/default.conf"))
                        .replace(
                                "http://backend:8080",
                                "http://host.testcontainers.internal:" + port)
                        .replace(
                                "http://frontend:80",
                                "http://host.testcontainers.internal:" + port);
        try (var nginx =
                new org.testcontainers.containers.GenericContainer<>("nginx:1.29-alpine")
                        .withExposedPorts(80)
                        .withCopyToContainer(
                                org.testcontainers.images.builder.Transferable.of(config),
                                "/etc/nginx/conf.d/default.conf")) {
            nginx.start();
            String origin = "http://localhost:" + nginx.getMappedPort(80);
            var http = browser(admin, origin);
            gateway(http, origin, "GET", "/health", null, null, 200);
            var b =
                    gateway(
                            http,
                            origin,
                            "POST",
                            "/v2/admin/knowledge-bases",
                            new BaseInput("Deployment fixture", null),
                            null,
                            200);
            var job =
                    gatewayUpload(
                            http,
                            origin,
                            UUID.fromString(b.path("id").asText()),
                            "large",
                            " ".repeat(1100000) + FACT);
            assertThat(job.path("state").asText()).isEqualTo("QUEUED");
        }
    }

    @Test
    void cancelCannotReplayCompletedEvidenceAfterKnowledgeIsHidden() throws Exception {
        UUID kb = publishedKb();
        var e = terminal(executions.test(admin, kb, query(UUID.randomUUID(), true)));
        assertThat(e.toolStepsAndSources().toString()).contains(FACT);
        knowledge.update(
                admin,
                kb,
                knowledge.get(admin, kb).revision(),
                new BaseInput("Hidden after completion", "HIDDEN"));
        assertThatThrownBy(() -> executions.read(admin, UUID.fromString(e.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
        assertThatThrownBy(() -> executions.cancel(admin, UUID.fromString(e.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/v2/admin/agent-executions/" + e.id() + "/cancel")
                                .cookie(cookie(admin))
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isNotFound());
    }
}
