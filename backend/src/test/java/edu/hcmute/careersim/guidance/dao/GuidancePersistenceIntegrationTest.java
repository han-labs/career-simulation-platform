package edu.hcmute.careersim.guidance.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.hcmute.careersim.simulation.access.SimulationEvidenceAccess;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class GuidancePersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                            org.testcontainers.utility.DockerImageName.parse(
                                            "pgvector/pgvector:0.8.2-pg17")
                                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("careersim_test")
                    .withUsername("careersim")
                    .withPassword("careersim_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private GuidanceDao guidanceDao;
    @Autowired private SynAgentDao synAgentDao;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private SimulationEvidenceAccess simulationEvidenceAccess;

    @Autowired private SynTurnDao turns;
    @Autowired private KnowledgeDao knowledge;
    @Autowired private edu.hcmute.careersim.guidance.service.SynTurnLifecycleService lifecycle;
    @Autowired private edu.hcmute.careersim.guidance.service.GuidanceService guidance;
    private long studentId;
    private long assessmentAttemptId;

    @BeforeEach
    void insertSyntheticEvidence() {
        studentId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO app_users
                            (email, password_hash, display_name, role, status)
                        VALUES ('guidance-test@example.test', 'test-only',
                                'Guidance Test Student', 'STUDENT', 'ACTIVE')
                        RETURNING id
                        """,
                        Long.class);
        assessmentAttemptId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO assessment_attempts
                            (student_id, status, completed_at)
                        VALUES (?, 'COMPLETED', CURRENT_TIMESTAMP)
                        RETURNING id
                        """,
                        Long.class,
                        studentId);
    }

    @AfterEach
    void removeSyntheticEvidence() {
        jdbcTemplate.update(
                "UPDATE knowledge_documents SET active_version_id=NULL WHERE source_key='integration-fixture'");
        jdbcTemplate.update(
                "DELETE FROM knowledge_chunks WHERE version_id IN (SELECT v.id FROM knowledge_versions v JOIN knowledge_documents d ON d.id=v.document_id WHERE d.source_key='integration-fixture')");
        jdbcTemplate.update(
                "DELETE FROM knowledge_versions WHERE document_id IN (SELECT id FROM knowledge_documents WHERE source_key='integration-fixture')");
        jdbcTemplate.update(
                "DELETE FROM knowledge_documents WHERE source_key='integration-fixture'");
        jdbcTemplate.update("DELETE FROM guidance_reports WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM syn_sessions WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM exploration_plans WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM assessment_attempts WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM student_profiles WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM app_users WHERE id = ?", studentId);
    }

    @Test
    void flywaySchemaSupportsPlanUpsertAndGuidanceAudit() {
        guidanceDao.saveCurrentPlan(
                studentId, "First plan", List.of("Complete one focused simulation"));
        GuidanceDao.CurrentPlan updated =
                guidanceDao.saveCurrentPlan(
                        studentId,
                        "Updated plan",
                        List.of("Review the evidence", "Write a reflection"));
        guidanceDao.recordGuidance(
                studentId,
                assessmentAttemptId,
                null,
                "EXPLAIN_RIASEC",
                "FALLBACK",
                "READY",
                null,
                Map.of("text", "Synthetic Standard guidance", "provenance", "STANDARD"),
                3);

        assertThat(updated.title()).isEqualTo("Updated plan");
        assertThat(updated.steps()).containsExactly("Review the evidence", "Write a reflection");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM exploration_plans WHERE student_id = ?",
                                Integer.class,
                                studentId))
                .isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                                SELECT source || ':' || action_type
                                FROM guidance_reports
                                WHERE student_id = ?
                                """,
                                String.class,
                                studentId))
                .isEqualTo("FALLBACK:EXPLAIN_RIASEC");
    }

    @Test
    void flywaySeedsACompleteSyntheticDemoJourney() {
        Long demoStudentId =
                jdbcTemplate.queryForObject(
                        "SELECT id FROM app_users WHERE email = 'student@demo.com'", Long.class);

        assertThat(demoStudentId).isNotNull();
        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                                SELECT COUNT(*)
                                FROM riasec_questions
                                WHERE active = TRUE
                                """,
                                Integer.class))
                .isEqualTo(42);
        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                                SELECT COUNT(*)
                                FROM career_simulations simulation
                                WHERE simulation.status = 'PUBLISHED'
                                  AND 3 = (
                                      SELECT COUNT(*)
                                      FROM simulation_tasks task
                                      WHERE task.simulation_id = simulation.id
                                  )
                                """,
                                Integer.class))
                .isEqualTo(5);
        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                                SELECT consented_to_ai_at IS NOT NULL
                                FROM student_profiles
                                WHERE user_id = ?
                                """,
                                Boolean.class,
                                demoStudentId))
                .isTrue();
        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                                SELECT COUNT(*)
                                FROM assessment_answers answer
                                JOIN assessment_attempts attempt ON attempt.id = answer.attempt_id
                                WHERE attempt.student_id = ? AND attempt.status = 'COMPLETED'
                                """,
                                Integer.class,
                                demoStudentId))
                .isEqualTo(42);

        var simulations = simulationEvidenceAccess.findRecentEvaluated(demoStudentId, 3);
        assertThat(simulations).hasSize(1);
        assertThat(simulations.get(0).title()).isEqualTo("Backend Developer");
        assertThat(simulations.get(0).score()).isEqualTo(67);
        assertThat(simulations.get(0).careerTrack()).isEqualTo("BACKEND_DEVELOPMENT");
        assertThat(simulations.get(0).evaluatedAt()).isNotNull();
        assertThat(simulations.get(0).outcomes())
                .extracting(SimulationEvidenceAccess.TaskOutcome::status)
                .containsExactly("OBSERVED_STRENGTH", "OBSERVED_STRENGTH", "NEEDS_MORE_EVIDENCE");
        assertThat(synAgentDao.findSkillEvidence(List.of(simulations.get(0).attemptId())))
                .extracting(SynAgentDao.SkillEvidence::skillCode)
                .containsExactly("api_reasoning", "backend_debugging", "sql");
    }

    @Test
    void synSessionIsOwnedBoundedAndCanBeCleared() {
        var created = synAgentDao.loadOrCreateSession(studentId, null);
        synAgentDao.updateSession(
                studentId,
                created.id(),
                "Last intent: COMPARE_PATHS.",
                "COMPARE_PATHS",
                List.of("BACKEND_DEVELOPMENT", "FRONTEND_DEVELOPMENT"));

        var loaded = synAgentDao.loadOrCreateSession(studentId, created.id());

        assertThat(loaded.lastPaths())
                .containsExactly("BACKEND_DEVELOPMENT", "FRONTEND_DEVELOPMENT");
        assertThat(synAgentDao.findPaths(loaded.lastPaths()))
                .allSatisfy(
                        path -> {
                            assertThat(path.summary()).isNotBlank();
                            assertThat(path.riasecCodes()).isNotBlank();
                            assertThat(path.skillCodes()).isNotEmpty();
                        });
        assertThat(synAgentDao.findResources(List.of("BACKEND_DEVELOPMENT"), 3)).isNotEmpty();
        assertThat(synAgentDao.findResources(List.of("BACKEND_DEVELOPMENT"), List.of("sql"), 3))
                .extracting(SynAgentDao.LearningResource::skill)
                .containsExactly("sql");
        assertThat(
                        synAgentDao.findResources(
                                List.of("BACKEND_DEVELOPMENT"), List.of("not_reviewed_skill"), 3))
                .isEmpty();
        assertThat(synAgentDao.findSimulations(List.of("BACKEND_DEVELOPMENT"), 1))
                .singleElement()
                .satisfies(
                        simulation -> {
                            assertThat(simulation.summary()).isNotBlank();
                            assertThat(simulation.difficulty()).isEqualTo("INTRODUCTORY");
                            assertThat(simulation.estimatedMinutes()).isPositive();
                        });
        assertThat(synAgentDao.clearSession(studentId, created.id())).isTrue();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM syn_sessions WHERE id = ?",
                                Integer.class,
                                created.id()))
                .isZero();
    }

    @Test
    void foreignSessionIdNeverLoadsAnotherStudentsMemory() {
        UUID foreignId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO syn_sessions (id, student_id, summary) VALUES (?, ?, ?)",
                foreignId,
                studentId,
                "Private structured context");

        long otherStudent =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO app_users
                            (email, password_hash, display_name, role, status)
                        VALUES ('guidance-other@example.test', 'test-only', 'Other', 'STUDENT', 'ACTIVE')
                        RETURNING id
                        """,
                        Long.class);
        try {
            var loaded = synAgentDao.loadOrCreateSession(otherStudent, foreignId);
            assertThat(loaded.id()).isNotEqualTo(foreignId);
            assertThat(loaded.summary()).isEmpty();
            synAgentDao.clearSession(otherStudent, loaded.id());
        } finally {
            jdbcTemplate.update("DELETE FROM app_users WHERE id = ?", otherStudent);
        }
    }

    @Test
    void pgvectorPublishesVersionsAtomicallyAndExcludesRevokedAndMismatchedProfiles() {
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT extversion FROM pg_extension WHERE extname='vector'",
                                String.class))
                .isEqualTo("0.8.2");
        long first = publishFixture("hash-one", "First SQL fixture");
        var found = knowledge.vector(vector(0), "synthetic:768:v1", 5, .5);
        assertThat(found).hasSize(1);
        long oldChunk = found.get(0).id();
        assertThat(knowledge.vector(vector(0), "other-profile", 5, .5)).isEmpty();
        assertThat(knowledge.lexical("sql", 5)).hasSize(1);
        assertThat(publishFixture("hash-one", "First SQL fixture")).isEqualTo(first);
        long second = publishFixture("hash-two", "Second SQL fixture");
        assertThat(second).isNotEqualTo(first);
        assertThat(knowledge.published(oldChunk)).isEmpty();
        assertThat(knowledge.vector(vector(0), "synthetic:768:v1", 5, .5))
                .extracting("content")
                .containsExactly("Second SQL fixture");
        knowledge.revoke("integration-fixture");
        assertThat(knowledge.lexical("sql", 5)).isEmpty();
        assertThat(knowledge.vector(vector(0), "synthetic:768:v1", 5, .5)).isEmpty();
    }

    @Test
    void failedPublishKeepsThePreviousVersionAndRollsBackStagingRows() {
        long previous = publishFixture("good", "Valid SQL fixture");
        assertThatThrownBy(
                        () ->
                                knowledge.publish(
                                        fixtureDocument(),
                                        "bad",
                                        "synthetic:768:v1",
                                        List.of(
                                                new KnowledgeDao.EmbeddedChunk(
                                                        "SQL", "Invalid fixture", new double[2]))))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThat(knowledge.lexical("sql", 5)).extracting("versionId").containsExactly(previous);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM knowledge_versions v JOIN knowledge_documents d ON d.id=v.document_id WHERE d.source_key='integration-fixture'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void duplicateRequestsReplayOnceAndDifferentPayloadConflicts() {
        var request = request(null, UUID.randomUUID());
        var turn = lifecycle.begin(studentId, request);
        assertThatThrownBy(() -> lifecycle.begin(studentId, request))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        var completed =
                lifecycle.finish("guidance-test@example.test", turn, run(turn, List.of()), 5);
        assertThat(lifecycle.begin(studentId, request).response()).isEqualTo(completed);
        var different =
                new edu.hcmute.careersim.guidance.dto.SynMessageRequest(
                        "different", "GET_STARTED", null, "EN", null, request.clientMessageId());
        assertThatThrownBy(() -> lifecycle.begin(studentId, different))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM syn_turns WHERE student_id=?",
                                Integer.class,
                                studentId))
                .isEqualTo(1);
    }

    @Test
    void clearIsIdempotentAndLateCompletionCannotRestoreMemory() {
        var turn = lifecycle.begin(studentId, request(null, UUID.randomUUID()));
        assertThat(lifecycle.clear(studentId, turn.sessionId())).isTrue();
        assertThat(lifecycle.clear(studentId, turn.sessionId())).isTrue();
        assertThatThrownBy(
                        () ->
                                lifecycle.finish(
                                        "guidance-test@example.test",
                                        turn,
                                        run(turn, List.of()),
                                        5))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThat(turns.find(studentId, turn.id()).orElseThrow().state()).isEqualTo("CANCELLED");
        assertThat(synAgentDao.findSession(studentId, turn.sessionId())).isEmpty();
        assertThat(turns.find(studentId + 100000, turn.id())).isEmpty();
    }

    @Test
    void revokedKnowledgeIsRejectedAtFinalizationWithoutAdvancingSession() {
        publishFixture("good", "SQL fixture");
        var chunk = knowledge.lexical("sql", 5).get(0);
        var turn = lifecycle.begin(studentId, request(null, UUID.randomUUID()));
        knowledge.revoke("integration-fixture");
        var reference =
                new edu.hcmute.careersim.guidance.dto.SynMessageResponse.EvidenceReference(
                        "Fixture",
                        "KNOWLEDGE",
                        chunk.referenceId(),
                        Long.toString(chunk.versionId()),
                        "SQL",
                        null,
                        "SQL fixture");
        assertThatThrownBy(
                        () ->
                                lifecycle.finish(
                                        "guidance-test@example.test",
                                        turn,
                                        run(turn, List.of(reference)),
                                        5))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThat(turns.sessionVersion(studentId, turn.sessionId())).isZero();
        assertThat(turns.find(studentId, turn.id()).orElseThrow().response()).isNull();
    }

    @Test
    void cleanupExpiresPayloadAndRecoversInterruptedTurns() {
        var turn = lifecycle.begin(studentId, request(null, UUID.randomUUID()));
        jdbcTemplate.update(
                "UPDATE syn_turns SET created_at=CURRENT_TIMESTAMP-INTERVAL '2 minutes' WHERE id=?",
                turn.id());
        turns.cleanup();
        assertThat(turns.find(studentId, turn.id()).orElseThrow().state()).isEqualTo("FAILED");
        var next = lifecycle.begin(studentId, request(turn.sessionId(), UUID.randomUUID()));
        lifecycle.finish("guidance-test@example.test", next, run(next, List.of()), 3);
        jdbcTemplate.update(
                "UPDATE syn_turns SET response_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 minute' WHERE id=?",
                next.id());
        turns.cleanup();
        assertThat(turns.find(studentId, next.id()).orElseThrow().response()).isNull();
    }

    @Test
    void coreFlowWritesTurnAuditEvenWithoutSimulationEvidenceAndAfterSqlFailure() {
        assertThatThrownBy(
                        () -> jdbcTemplate.execute("SELECT missing_fixture_column FROM app_users"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        var response =
                guidance.sendMessage(
                        "guidance-test@example.test", request(null, UUID.randomUUID()));
        assertThat(response.reasonCode()).isEqualTo("NO_AI_CONSENT");
        assertThat(turns.find(studentId, UUID.fromString(response.id())).orElseThrow().state())
                .isEqualTo("COMPLETED");
    }

    @Test
    void consentAndStatusAreRecheckedBeforeExternalProcessingAndFinalization() {
        var turn = lifecycle.begin(studentId, request(null, UUID.randomUUID()));
        assertThat(
                        lifecycle.externalAllowed(
                                "guidance-test@example.test",
                                studentId,
                                turn.sessionId(),
                                turn.id()))
                .isFalse();
        jdbcTemplate.update(
                "INSERT INTO student_profiles(user_id,consented_to_ai_at) VALUES (?,CURRENT_TIMESTAMP) ON CONFLICT(user_id) DO UPDATE SET consented_to_ai_at=CURRENT_TIMESTAMP",
                studentId);
        assertThat(
                        lifecycle.externalAllowed(
                                "guidance-test@example.test",
                                studentId,
                                turn.sessionId(),
                                turn.id()))
                .isTrue();
        jdbcTemplate.update("UPDATE app_users SET status='LOCKED' WHERE id=?", studentId);
        assertThat(
                        lifecycle.externalAllowed(
                                "guidance-test@example.test",
                                studentId,
                                turn.sessionId(),
                                turn.id()))
                .isFalse();
        assertThatThrownBy(
                        () ->
                                lifecycle.finish(
                                        "guidance-test@example.test",
                                        turn,
                                        run(turn, List.of()),
                                        1))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    private KnowledgeDao.Document fixtureDocument() {
        return new KnowledgeDao.Document(
                "integration-fixture",
                "Synthetic SQL",
                null,
                "Test fixture only",
                "EN",
                "BACKEND",
                "AUTOMATED_TEST_FIXTURE",
                true,
                "SQL fixture");
    }

    private long publishFixture(String hash, String content) {
        return knowledge.publish(
                fixtureDocument(),
                hash,
                "synthetic:768:v1",
                List.of(new KnowledgeDao.EmbeddedChunk("SQL", content, vector(0))));
    }

    private double[] vector(int axis) {
        double[] vector = new double[768];
        vector[axis] = 1;
        return vector;
    }

    private edu.hcmute.careersim.guidance.dto.SynMessageRequest request(
            UUID session, UUID request) {
        return new edu.hcmute.careersim.guidance.dto.SynMessageRequest(
                "Start exploring", "GET_STARTED", session, "EN", null, request);
    }

    private edu.hcmute.careersim.guidance.service.SynAgentOrchestrator.AgentRun run(
            SynTurnDao.Turn turn,
            List<edu.hcmute.careersim.guidance.dto.SynMessageResponse.EvidenceReference> refs) {
        var reply =
                new edu.hcmute.careersim.guidance.dto.SynMessageResponse(
                        turn.id().toString(),
                        turn.sessionId(),
                        "GET_STARTED",
                        "EN",
                        "NEW",
                        "Synthetic test reply",
                        "STANDARD",
                        List.of(),
                        refs,
                        null,
                        List.of(),
                        List.of(),
                        null,
                        List.of(),
                        List.of(),
                        null);
        return new edu.hcmute.careersim.guidance.service.SynAgentOrchestrator.AgentRun(
                reply, "FALLBACK", "READY", null, "v=2|lang=EN", List.of(), "STANDARD_POLICY");
    }

    @Test
    void simultaneousRequestsAllowOnlyOnePendingTurnPerSession() throws Exception {
        var session = synAgentDao.loadOrCreateSession(studentId, null);
        var barrier = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Boolean> attempt =
                    () -> {
                        barrier.await();
                        try {
                            lifecycle.begin(studentId, request(session.id(), UUID.randomUUID()));
                            return true;
                        } catch (edu.hcmute.careersim.common.exception.ApiException expected) {
                            return false;
                        }
                    };
            var first = pool.submit(attempt);
            var second = pool.submit(attempt);
            barrier.countDown();
            assertThat(
                            List.of(
                                    first.get(5, java.util.concurrent.TimeUnit.SECONDS),
                                    second.get(5, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(
                            jdbcTemplate.queryForObject(
                                    "SELECT count(*) FROM syn_turns WHERE session_id=? AND state='PENDING'",
                                    Integer.class,
                                    session.id()))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void failedFinalWriteRollsBackSessionAndLeavesNoCompletedResponse() {
        var turn = lifecycle.begin(studentId, request(null, UUID.randomUUID()));
        var original = run(turn, List.of());
        var invalid =
                new edu.hcmute.careersim.guidance.service.SynAgentOrchestrator.AgentRun(
                        original.response(),
                        "FALLBACK",
                        "READY",
                        "x".repeat(121),
                        "changed",
                        List.of(),
                        "STANDARD_POLICY");
        assertThatThrownBy(() -> lifecycle.finish("guidance-test@example.test", turn, invalid, 5))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(turns.sessionVersion(studentId, turn.sessionId())).isZero();
        assertThat(synAgentDao.findSession(studentId, turn.sessionId()).orElseThrow().summary())
                .isEmpty();
        assertThat(turns.find(studentId, turn.id()).orElseThrow().state()).isEqualTo("PENDING");
    }
}
