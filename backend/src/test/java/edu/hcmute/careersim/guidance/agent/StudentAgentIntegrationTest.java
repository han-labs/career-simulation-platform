package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;
import edu.hcmute.careersim.guidance.agent.dto.StudentAgentContracts.*;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.guidance.service.*;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
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
class StudentAgentIntegrationTest {
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

    @Autowired edu.hcmute.careersim.assessment.service.AssessmentAuthoringService assessmentAuthor;
    @Autowired edu.hcmute.careersim.guidance.knowledge.service.KnowledgeStudioService knowledge;
    @Autowired JdbcTemplate jdbc;
    @Autowired StudentAgentService students;

    @Autowired
    edu.hcmute.careersim.assessment.service.VersionedAssessmentService assessmentCatalogue;

    @Autowired StudentEvidenceTools publicTools;
    @Autowired AgentRegistryService registry;
    @Autowired ExecutionService executions;
    @Autowired AgentUsageQuery usage;
    @Autowired ObjectMapper json;
    @Autowired BrowserAuthentication auth;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @MockitoBean AgentModelProvider model;
    @MockitoBean EmbeddingProvider embedding;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    static final String MODEL = "gemini/synthetic-student",
            FACT = "Consider one small practice task.";
    long admin, owner, other;
    UUID agent, config;
    AtomicInteger providerCalls;
    String naturalAction;
    CountDownLatch entered, release;

    long user(String role) {
        String id = UUID.randomUUID().toString();
        long uid =
                jdbc.queryForObject(
                        "INSERT INTO app_users(email,password_hash,display_name,role) VALUES (?,'synthetic-only','Synthetic account',?) RETURNING id",
                        Long.class,
                        id + "@example.test",
                        role);
        if (role.equals("STUDENT"))
            jdbc.update("INSERT INTO student_profiles(user_id) VALUES (?)", uid);
        return uid;
    }

    void consent(long user) {
        jdbc.update(
                "INSERT INTO consent_grant(user_id,purpose,recipient_key,policy_version,confirmed_at,confirmation_method) VALUES (?,'EXTERNAL_AI','provider:gemini','careersim-purpose-v1',CURRENT_TIMESTAMP,'EXPLICIT_UI')",
                user);
    }

    ConfigurationInput configuration() {
        return new ConfigurationInput(
                MODEL,
                "Support exploration with authorized evidence only.",
                null,
                CoreValues.tree(
                        Map.of(
                                "enabled",
                                List.of(
                                        "assessment.lookup",
                                        "simulation.explore",
                                        "simulation.myEvidence"),
                                "maxRounds",
                                3,
                                "maxToolCalls",
                                8,
                                "topK",
                                5,
                                "timeoutSeconds",
                                30)),
                CoreValues.tree(Map.of("enabled", true)),
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "grounding",
                                "GENERAL_ALLOWED",
                                "maxCharacters",
                                4000)),
                List.of());
    }

    AgentModelProvider.Decision plain() {
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
                                FACT,
                                "citations",
                                List.of(),
                                "suggestions",
                                List.of())));
    }

    @BeforeEach
    void fixture() {
        naturalAction = null;
        admin = user("ADMINISTRATOR");
        owner = user("STUDENT");
        other = user("STUDENT");
        assessment();
        consent(owner);
        consent(other);
        providerCalls = new AtomicInteger();
        entered = new CountDownLatch(1);
        release = new CountDownLatch(1);
        when(model.models())
                .thenReturn(
                        List.of(
                                new AgentModelProvider.Model(
                                        MODEL, "gemini", "synthetic-student", true)));
        when(model.modelKeys()).thenReturn(Set.of(MODEL));
        when(model.streamStructured(
                        anyString(), anyString(), anyString(), anyList(), anyList(), any(), any()))
                .thenCallRealMethod();
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(plain());
        when(model.stream(anyString(), anyString(), anyString(), anyList(), anyList(), any()))
                .thenAnswer(
                        call -> {
                            String configuredPrompt = call.getArgument(1);
                            if (configuredPrompt.contains("ADMIN_PREVIEW")) return plain();
                            providerCalls.incrementAndGet();
                            SynExecutionScope.recordUsage("gemini", "synthetic-student", 40L, 12L);
                            String query = call.getArgument(2);
                            List<AgentModelProvider.Exchange> exchanges = call.getArgument(3);
                            String prompt = call.getArgument(1);
                            if (json.readTree(query).path("request").asText().contains("hold")
                                    && !exchanges.isEmpty()) {
                                var sink = call.getArgument(5, java.util.function.Consumer.class);
                                sink.accept(
                                        "{\"schemaVersion\":1,\"outcome\":\"ANSWER\",\"text\":\"First streamed sentence. ");
                                entered.countDown();
                                if (!release.await(8, TimeUnit.SECONDS))
                                    throw new IllegalStateException("Fixture wait expired");
                            }
                            if (exchanges.isEmpty())
                                return new AgentModelProvider.Decision(
                                        CoreValues.tree(Map.of("role", "model")),
                                        List.of(
                                                new AgentModelProvider.Call(
                                                        "a",
                                                        "assessment.lookup",
                                                        CoreValues.tree(Map.of())),
                                                new AgentModelProvider.Call(
                                                        "b",
                                                        "simulation.explore",
                                                        CoreValues.tree(Map.of())),
                                                new AgentModelProvider.Call(
                                                        "c",
                                                        "simulation.myEvidence",
                                                        CoreValues.tree(Map.of()))),
                                        null);
                            var evidence =
                                    exchanges.get(0).observations().get(0).path("evidence").path(0);
                            String ref = evidence.path("referenceId").asText();
                            String quote = evidence.path("text").asText();
                            var out =
                                    (com.fasterxml.jackson.databind.node.ObjectNode)
                                            plain().output();
                            out.set(
                                    "citations",
                                    CoreValues.tree(
                                            List.of(
                                                    Map.of(
                                                            "referenceId",
                                                            ref,
                                                            "quote",
                                                            quote.substring(
                                                                    0,
                                                                    Math.min(
                                                                            quote.length(),
                                                                            200))))));
                            if (json.readTree(query).path("request").asText().contains("hold"))
                                out.put("text", "First streamed sentence. " + FACT);
                            if (prompt.contains("Explicit report request")
                                    || "INSIGHT_REPORT".equals(naturalAction))
                                out.set(
                                        "report",
                                        CoreValues.tree(
                                                Map.of(
                                                        "schemaVersion",
                                                        "careersim-insight-v1",
                                                        "summary",
                                                        "An exploration hypothesis based on authorized evidence.",
                                                        "hypotheses",
                                                        List.of(
                                                                Map.of(
                                                                        "direction",
                                                                        "Explore practical tasks",
                                                                        "reason",
                                                                        FACT,
                                                                        "evidenceRefs",
                                                                        List.of(ref))),
                                                        "strengthsPracticed",
                                                        List.of(),
                                                        "difficulties",
                                                        List.of(),
                                                        "limitations",
                                                        List.of(
                                                                "Synthetic fixture; no professional certification."),
                                                        "nextSteps",
                                                        List.of(FACT),
                                                        "disclaimer",
                                                        "Exploration support only; this report does not certify professional competence or determine a career.",
                                                        "evidenceRefs",
                                                        List.of(ref))));
                            if (prompt.contains("Explicit draft suggestion")
                                    || "PLAN_SUGGESTION".equals(naturalAction))
                                out.set(
                                        "planSuggestion",
                                        CoreValues.tree(
                                                Map.of(
                                                        "schemaVersion",
                                                        "careersim-plan-v1",
                                                        "title",
                                                        "Practice exploration",
                                                        "steps",
                                                        List.of(FACT),
                                                        "limitations",
                                                        List.of("Draft for Student review."),
                                                        "evidenceRefs",
                                                        List.of(ref))));
                            if (naturalAction != null) out.put("requestedAction", naturalAction);
                            if (json.readTree(query)
                                    .path("request")
                                    .asText()
                                    .contains("invalid-schema")) out.put("unapproved", true);
                            return new AgentModelProvider.Decision(
                                    CoreValues.tree(Map.of()), List.of(), out);
                        });
        var a =
                registry.create(
                        admin, new AgentInput("Synthetic Student Agent", "Exploration", null));
        agent = UUID.fromString(a.id());
        config = savePublish();
        activateRole(true);
    }

    void activateRole(boolean active) {
        var revisions =
                registry.roleBindings(admin).stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        RoleBinding::role, RoleBinding::revision));
        registry.activateRoles(
                admin,
                agent,
                registry.get(admin, agent).revision(),
                new RoleActivation(active ? List.of("STUDENT") : List.of(), revisions, true));
    }

    void assessment() {
        var dimensions = edu.hcmute.careersim.assessment.domain.RiasecV1Evaluator.DIMENSIONS;
        var policy =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "dimensions",
                                dimensions,
                                "aggregation",
                                "SUM",
                                "minAnswer",
                                1,
                                "maxAnswer",
                                5,
                                "tieOrder",
                                dimensions));
        var questions =
                new ArrayList<
                        edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                .QuestionDraft>();
        int position = 0;
        for (String d : dimensions)
            questions.add(
                    new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                            .QuestionDraft(
                            ++position,
                            "Synthetic interest " + d,
                            "LIKERT",
                            CoreValues.tree(
                                    Map.of("schemaVersion", 1, "min", 1, "max", 5, "dimension", d)),
                            CoreValues.tree(Map.of()),
                            true));
        String type =
                assessmentAuthor.createType(
                        admin,
                        new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                .TypeRequest("SYN_" + UUID.randomUUID(), "Synthetic assessment"));
        var v =
                assessmentAuthor.createVersion(
                        admin,
                        Long.parseLong(type),
                        new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                .VersionDraft("riasec-sum", "1", policy, questions));
        assessmentAuthor.publish(admin, Long.parseLong(v.id()), v.revision());
    }

    UUID savePublish() {
        return savePublish(configuration());
    }

    UUID savePublish(ConfigurationInput input) {
        var a = registry.get(admin, agent);
        var c = registry.save(admin, agent, a.revision(), input);
        var e =
                executions.preview(
                        admin,
                        agent,
                        UUID.fromString(c.id()),
                        new Preview(UUID.randomUUID(), "Synthetic public preview"));
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> executions.read(admin, UUID.fromString(e.id())).finishedAt() != null);
        registry.publish(
                admin, agent, UUID.fromString(c.id()), registry.get(admin, agent).revision(), true);
        return UUID.fromString(c.id());
    }

    Session session(long user) {
        return students.create(
                user,
                agent,
                new CreateSession(UUID.randomUUID(), "Synthetic conversation", List.of()));
    }

    Run done(long user, Run run) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(
                        () ->
                                !Set.of("ACCEPTED", "RUNNING")
                                        .contains(
                                                students.read(user, UUID.fromString(run.id()))
                                                        .status()));
        return students.read(user, UUID.fromString(run.id()));
    }

    Run message(Session s, String text) {
        return students.submit(
                owner, UUID.fromString(s.id()), "CHAT", new Message(UUID.randomUUID(), text));
    }

    @AfterEach
    void unblock() {
        release.countDown();
    }

    @Test
    void newSessionStoresPinnedConfiguredGreetingOnceWithoutProviderCall() {
        var base = configuration();
        var experience = new AgentExperienceTest().valid();
        var selected =
                savePublish(
                        new ConfigurationInput(
                                base.modelKey(),
                                base.promptTemplate(),
                                null,
                                base.toolPolicy(),
                                base.memoryPolicy(),
                                base.outputPolicy(),
                                base.knowledgeBaseIds(),
                                experience));
        activateRole(true);
        int before = providerCalls.get();
        var request = new CreateSession(UUID.randomUUID(), "Greeting test", List.of());
        var created = students.create(owner, agent, request);
        assertThat(created.configurationId()).isEqualTo(selected.toString());
        assertThat(students.create(owner, agent, request).id()).isEqualTo(created.id());
        var history = students.history(owner, UUID.fromString(created.id()), 0, 100);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).actorType()).isEqualTo("ASSISTANT");
        assertThat(history.get(0).content().path("text").asText())
                .isEqualTo("Xin chào Lan Nguyễn 🧪");
        assertThat(history.get(0).executionId()).isNull();
        assertThat(providerCalls.get()).isEqualTo(before);
        assertThatThrownBy(() -> students.history(other, UUID.fromString(created.id()), 0, 100))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
    }

    @Test
    void insufficientBeforeLookupIsReconsideredOnceWithoutStreamingThePrematureAbstention() {
        var base = configuration();
        var policy =
                ((com.fasterxml.jackson.databind.node.ObjectNode) base.toolPolicy())
                        .deepCopy()
                        .put("maxRounds", 4);
        savePublish(
                new ConfigurationInput(
                        base.modelKey(),
                        base.promptTemplate(),
                        null,
                        policy,
                        base.memoryPolicy(),
                        base.outputPolicy(),
                        base.knowledgeBaseIds()));
        activateRole(true);
        var attempts = new AtomicInteger();
        when(model.stream(anyString(), anyString(), anyString(), anyList(), anyList(), any()))
                .thenAnswer(
                        call -> {
                            int attempt = attempts.incrementAndGet();
                            if (attempt == 1) {
                                var output =
                                        ((com.fasterxml.jackson.databind.node.ObjectNode)
                                                        plain().output())
                                                .deepCopy()
                                                .put("outcome", "INSUFFICIENT_EVIDENCE")
                                                .put("text", "Premature missing evidence");
                                call.getArgument(5, java.util.function.Consumer.class)
                                        .accept(CoreValues.write(output));
                                return new AgentModelProvider.Decision(null, List.of(), output);
                            }
                            if (attempt == 2) {
                                assertThat(call.getArgument(1, String.class))
                                        .contains(
                                                "No evidence lookup has been attempted",
                                                "Public catalogue discovery");
                                assertThat(call.getArgument(4, List.class)).isNotEmpty();
                                return new AgentModelProvider.Decision(
                                        CoreValues.tree(Map.of("role", "model")),
                                        List.of(
                                                new AgentModelProvider.Call(
                                                        "public",
                                                        "assessment.lookup",
                                                        CoreValues.tree(Map.of()))),
                                        null);
                            }
                            assertThat(call.getArgument(1, String.class))
                                    .doesNotContain("No evidence lookup has been attempted");
                            return plain();
                        });
        var s = session(owner);
        var result = done(owner, message(s, "Explain available career quizzes"));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(attempts).hasValue(3);
        assertThat(result.toolStepsAndSources().findValuesAsText("type"))
                .contains("EVIDENCE_RECONSIDERATION", "ACTUAL_NATIVE_CALL");
        assertThat(CoreValues.write(students.events(owner, UUID.fromString(result.id()), 0)))
                .doesNotContain("Premature missing evidence");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ANSWER", "REFUSED"})
    void greetingsAndSecurityRefusalsDoNotTriggerEvidenceReconsideration(String outcome) {
        var count = new AtomicInteger();
        when(model.stream(anyString(), anyString(), anyString(), anyList(), anyList(), any()))
                .thenAnswer(
                        call -> {
                            count.incrementAndGet();
                            var answer =
                                    ((com.fasterxml.jackson.databind.node.ObjectNode)
                                                    plain().output())
                                            .deepCopy()
                                            .put("outcome", outcome);
                            return new AgentModelProvider.Decision(null, List.of(), answer);
                        });
        var result = done(owner, message(session(owner), "Direct response fixture"));
        assertThat(result.status()).isEqualTo(outcome.equals("ANSWER") ? "SUCCEEDED" : "REFUSED");
        assertThat(count).hasValue(1);
        assertThat(result.toolStepsAndSources().findValuesAsText("type"))
                .doesNotContain("EVIDENCE_RECONSIDERATION", "ACTUAL_NATIVE_CALL");
    }

    @Test
    void publicAssessmentEvidenceCountsThePublishedVersionAndExcludesDraftOrArchivedTypes() {
        var listed = assessmentCatalogue.publicCatalog();
        var sample = listed.get(listed.size() - 1);
        int count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM assessment_question WHERE assessment_version_id=?",
                        Integer.class,
                        Long.parseLong(sample.activeVersionId()));
        assertThat(sample.questionCount()).isEqualTo(count).isPositive();
        var publicEvidence =
                publicTools.preview(
                        "assessment.lookup",
                        CoreValues.tree(Map.of("assessmentType", sample.code())));
        assertThat(publicEvidence).hasSize(1);
        assertThat(publicEvidence.get(0).data().path("questionCount").asInt()).isEqualTo(count);
        assertThat(CoreValues.write(publicEvidence))
                .doesNotContain(
                        "scoringConfig", "responseConfig", "answerData", "ASSESSMENT_RESULT");
        var draft =
                assessmentAuthor.createType(
                        admin,
                        new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                .TypeRequest("DRAFT_" + UUID.randomUUID(), "Unpublished test"));
        assertThat(assessmentCatalogue.publicCatalog())
                .extracting(
                        edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.PublicType
                                ::id)
                .doesNotContain(draft);
        jdbc.update(
                "UPDATE assessment_type SET status='ARCHIVED' WHERE id=?",
                Long.parseLong(sample.id()));
        assertThat(assessmentCatalogue.publicCatalog())
                .extracting(
                        edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.PublicType
                                ::id)
                .doesNotContain(sample.id());
        assertThat(
                        publicTools.preview(
                                "assessment.lookup",
                                CoreValues.tree(Map.of("assessmentType", sample.code()))))
                .isEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {
                "tôi hình như có làm 1 bài rồi, bạn tìm trong cơ sở dữ liệu và phân tích kết quả của bài quizz tôi đã làm với",
                "I think I completed a quiz; look up my saved result and explain it."
            })
    void naturalSelfResultRequestRepairsDuplicateCitationWithoutReplayingTools(String message) {
        var run = duplicateCitationRun(message, false);
        assertThat(run.status()).isEqualTo("SUCCEEDED");
        assertThat(run.output().path("citations")).hasSize(1);
        assertThat(run.toolStepsAndSources().findValuesAsText("validationCheck"))
                .contains("DUPLICATE_CITATION");
        assertThat(
                        run.toolStepsAndSources().findValuesAsText("type").stream()
                                .filter("ACTUAL_NATIVE_CALL"::equals)
                                .count())
                .isEqualTo(1);
    }

    @Test
    void providerOutageDuringRepairRemainsFailedWithSafeStatusAndNoFakeAnswer() {
        var run = duplicateCitationRun("Explain my saved quiz result", true);
        assertThat(run.status()).isEqualTo("FAILED");
        assertThat(run.errorCode()).isEqualTo("PROVIDER_HTTP_UNAVAILABLE");
        assertThat(run.toolStepsAndSources().findValuesAsText("httpStatusClass")).contains("5xx");
        assertThat(run.toolStepsAndSources().findValues("providerHttpStatus"))
                .allSatisfy(n -> assertThat(n.asInt()).isEqualTo(503));
        assertThat(CoreValues.write(run.output())).doesNotContain("Rejected candidate draft");
        assertThat(
                        run.toolStepsAndSources().findValuesAsText("type").stream()
                                .filter("ACTUAL_NATIVE_CALL"::equals)
                                .count())
                .isEqualTo(1);
    }

    Run duplicateCitationRun(String request, boolean unavailable) {
        var v = assessmentCatalogue.publicCatalog().get(0);
        var attempt =
                assessmentCatalogue.start(
                        owner, Long.parseLong(v.activeVersionId()), UUID.randomUUID());
        assessmentCatalogue.submit(
                owner,
                Long.parseLong(attempt.id()),
                attempt.revision(),
                new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.Responses(
                        attempt.questions().stream()
                                .map(
                                        q ->
                                                new edu.hcmute.careersim.assessment.dto
                                                        .VersionedAssessmentContracts.Answer(
                                                        q.id(),
                                                        CoreValues.tree(
                                                                Map.of(
                                                                        "schemaVersion",
                                                                        1,
                                                                        "value",
                                                                        3))))
                                .toList()));
        var calls = new AtomicInteger();
        when(model.stream(anyString(), anyString(), anyString(), anyList(), anyList(), any()))
                .thenAnswer(
                        call -> {
                            int round = calls.incrementAndGet();
                            if (round == 1)
                                return new AgentModelProvider.Decision(
                                        CoreValues.tree(Map.of("role", "model")),
                                        List.of(
                                                new AgentModelProvider.Call(
                                                        "self",
                                                        "assessment.lookup",
                                                        CoreValues.tree(
                                                                Map.of("scope", "PERSONAL")))),
                                        null);
                            List<AgentModelProvider.Exchange> history = call.getArgument(3);
                            assertThat(history).hasSize(1);
                            assertThat(call.getArgument(4, List.class)).isEmpty();
                            var evidence = history.get(0).observations().get(0).path("evidence");
                            var result =
                                    java.util.stream.StreamSupport.stream(
                                                    evidence.spliterator(), false)
                                            .filter(
                                                    d ->
                                                            d.path("kind")
                                                                    .asText()
                                                                    .equals("ASSESSMENT_RESULT"))
                                            .findFirst()
                                            .orElseThrow();
                            String text = result.path("text").asText();
                            var citation =
                                    Map.of(
                                            "referenceId",
                                            result.path("referenceId").asText(),
                                            "quote",
                                            text.substring(0, Math.min(180, text.length())));
                            if (round >= 3) {
                                assertThat(call.getArgument(1, String.class))
                                        .contains("DUPLICATE_CITATION", "ONE citation")
                                        .doesNotContain("Rejected candidate draft");
                                assertThat(
                                                json.readTree(call.getArgument(2, String.class))
                                                        .path(
                                                                "currentRunCitationOptionsUntrustedData")
                                                        .get(0)
                                                        .has("quoteOptions"))
                                        .isTrue();
                                if (unavailable)
                                    throw new edu.hcmute.careersim.guidance.service
                                            .SynGuidanceProvider.ProviderException(
                                            edu.hcmute.careersim.guidance.service
                                                    .SynGuidanceProvider.FailureKind
                                                    .PROVIDER_HTTP_UNAVAILABLE,
                                            Map.of("providerHttpStatus", 503));
                            }
                            var output =
                                    ((com.fasterxml.jackson.databind.node.ObjectNode)
                                                    plain().output())
                                            .deepCopy()
                                            .put("requestedAction", "NONE")
                                            .put(
                                                    "text",
                                                    round == 2
                                                            ? "Rejected candidate draft"
                                                            : "Your saved quiz result is available for exploration.");
                            output.set(
                                    "citations",
                                    CoreValues.tree(
                                            round == 2
                                                    ? List.of(citation, citation)
                                                    : List.of(citation)));
                            return new AgentModelProvider.Decision(null, List.of(), output);
                        });
        var session = session(owner);
        var result = done(owner, message(session, request));
        assertThat(calls.get()).isEqualTo(unavailable ? 4 : 3);
        assertThat(students.history(owner, UUID.fromString(session.id()), 0, 100))
                .noneMatch(
                        i -> i.content().path("text").asText().equals("Rejected candidate draft"));
        return result;
    }

    @Test
    void pinnedConfigurationSurvivesRepublishAndOwnerOnlyHistory() {
        var s = session(owner);
        assertThat(s.provider()).isEqualTo("gemini");
        UUID next = savePublish();
        assertThat(next).isNotEqualTo(config);
        assertThat(session(owner).configurationId()).isEqualTo(next.toString());
        assertThat(done(owner, message(s, "Explore evidence")).status()).isEqualTo("SUCCEEDED");
        assertThat(students.session(owner, UUID.fromString(s.id())).configurationId())
                .isEqualTo(config.toString());
        assertThatThrownBy(() -> students.history(other, UUID.fromString(s.id()), 0, 100))
                .isInstanceOf(edu.hcmute.careersim.common.exception.NotFoundException.class);
        assertThat(students.history(owner, UUID.fromString(s.id()), 0, 100))
                .extracting(Interaction::sequenceNumber)
                .containsExactly(1L, 2L, 3L);
        assertThat(providerCalls.get()).isEqualTo(2);
    }

    @Test
    void nativeToolRegistryUsesHostActorAndPersistsRealMockProviderRequests() {
        var s = session(owner);
        var r = done(owner, message(s, "Explore evidence"));
        assertThat(r.status())
                .as("Student execution error=%s trace=%s", r.errorCode(), r.toolStepsAndSources())
                .isEqualTo("SUCCEEDED");
        assertThat(r.toolStepsAndSources()).hasSize(5);
        assertThat(r.toolStepsAndSources().findValuesAsText("type"))
                .containsExactly(
                        "MODEL_ATTEMPT",
                        "ACTUAL_NATIVE_CALL",
                        "ACTUAL_NATIVE_CALL",
                        "ACTUAL_NATIVE_CALL",
                        "MODEL_ATTEMPT");
        assertThat(r.toolStepsAndSources().toString())
                .contains(
                        "ACTUAL_NATIVE_CALL",
                        "assessment.lookup",
                        "simulation.explore",
                        "simulation.myEvidence")
                .doesNotContain("password_hash", "scoringConfig", "modelAnswer");
        assertThat(r.inputTokens()).isEqualTo(80);
        assertThat(r.outputTokens()).isEqualTo(24);
    }

    @Test
    void idempotencySerializesConcurrentMessagesAndCancellation() throws Exception {
        var s = session(owner);
        UUID sid = UUID.fromString(s.id()), request = UUID.randomUUID();
        var input = new Message(request, "hold response");
        var run = students.submit(owner, sid, "CHAT", input);
        assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(students.submit(owner, sid, "CHAT", input).id()).isEqualTo(run.id());
        assertThatThrownBy(
                        () -> students.submit(owner, sid, "CHAT", new Message(request, "changed")))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThatThrownBy(() -> message(s, "another request"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        students.cancel(owner, UUID.fromString(run.id()));
        release.countDown();
        assertThat(done(owner, run).status()).isEqualTo("CANCELLED");
        assertThat(students.history(owner, sid, 0, 100)).hasSize(2);
    }

    @Test
    void actualSegmentsExistBeforeTerminalAndReplayDoesNotRegenerate() throws Exception {
        var s = session(owner);
        var run = message(s, "hold stream");
        assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
        var events = students.events(owner, UUID.fromString(run.id()), 0);
        assertThat(events)
                .anyMatch(
                        e ->
                                e.type().equals("answer.segment")
                                        && e.payload()
                                                .path("text")
                                                .asText()
                                                .contains("First streamed sentence."));
        assertThat(students.read(owner, UUID.fromString(run.id())).status()).isEqualTo("RUNNING");
        int calls = providerCalls.get();
        students.events(owner, UUID.fromString(run.id()), 1);
        assertThat(providerCalls.get()).isEqualTo(calls);
        release.countDown();
        assertThat(done(owner, run).status()).isEqualTo("SUCCEEDED");
    }

    @Test
    void explicitStudentRequestNeedsNoSeparateConsentGrantAndHistoryRemainsOwnerScoped() {
        jdbc.update("UPDATE consent_grant SET revoked_at=CURRENT_TIMESTAMP WHERE user_id=?", owner);
        var s = session(owner);
        var run = done(owner, message(s, "No consent"));
        assertThat(run.status()).isEqualTo("SUCCEEDED");
        assertThat(run.errorCode()).isNull();
        assertThat(providerCalls.get()).isPositive();
        assertThat(students.history(owner, UUID.fromString(s.id()), 0, 100)).hasSize(3);
    }

    @Test
    void privacyRevisionChangeStopsLateOutputAndReplay() throws Exception {
        var s = session(owner);
        var run = message(s, "hold revoke");
        assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
        jdbc.update("UPDATE consent_grant SET revoked_at=CURRENT_TIMESTAMP WHERE user_id=?", owner);
        jdbc.update("UPDATE app_users SET privacy_version=privacy_version+1 WHERE id=?", owner);
        release.countDown();
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(
                        () ->
                                jdbc.queryForObject(
                                                "SELECT status FROM agent_execution WHERE id=?",
                                                String.class,
                                                UUID.fromString(run.id()))
                                        .equals("CANCELLED"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT output_snapshot IS NULL FROM agent_execution WHERE id=?",
                                Boolean.class,
                                UUID.fromString(run.id())))
                .isTrue();
        assertThat(students.history(owner, UUID.fromString(s.id()), 0, 100)).hasSize(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT tool_steps_and_sources::text FROM agent_execution WHERE id=?",
                                String.class,
                                UUID.fromString(run.id())))
                .contains("MODEL_ATTEMPT", "contentRedacted")
                .doesNotContain("arguments", "evidence", "sources");
        assertThatThrownBy(() -> students.events(owner, UUID.fromString(run.id()), 0))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @Test
    void deletionTransitivelyInvalidatesThreeSessionsAndBlocksCrossOwnerLineage() {
        var a = session(owner);
        done(owner, message(a, "Original narrative"));
        var b =
                students.create(
                        owner,
                        agent,
                        new CreateSession(
                                UUID.randomUUID(), "Derived B", List.of(UUID.fromString(a.id()))));
        var c =
                students.create(
                        owner,
                        agent,
                        new CreateSession(
                                UUID.randomUUID(), "Derived C", List.of(UUID.fromString(b.id()))));
        assertThatThrownBy(
                        () ->
                                students.create(
                                        other,
                                        agent,
                                        new CreateSession(
                                                UUID.randomUUID(),
                                                "Foreign source",
                                                List.of(UUID.fromString(a.id())))))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        students.delete(owner, UUID.fromString(a.id()));
        assertThat(students.session(owner, UUID.fromString(b.id())).summaryStatus())
                .isEqualTo("INVALIDATED");
        assertThat(students.session(owner, UUID.fromString(c.id())).summaryStatus())
                .isEqualTo("INVALIDATED");
        assertThatThrownBy(() -> students.history(owner, UUID.fromString(a.id()), 0, 100))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThat(done(owner, message(b, "Fresh context after deletion")).status())
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void deletedSourceDuringCallCannotResurrectDerivedContent() throws Exception {
        var a = session(owner);
        done(owner, message(a, "Source"));
        var b =
                students.create(
                        owner,
                        agent,
                        new CreateSession(
                                UUID.randomUUID(), "Derived", List.of(UUID.fromString(a.id()))));
        var run = message(b, "hold derived");
        assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
        students.delete(owner, UUID.fromString(a.id()));
        release.countDown();
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(
                        () ->
                                jdbc.queryForObject(
                                                "SELECT status FROM agent_execution WHERE id=?",
                                                String.class,
                                                UUID.fromString(run.id()))
                                        .equals("CANCELLED"));
        assertThatThrownBy(() -> students.read(owner, UUID.fromString(run.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @Test
    void reportIsExplicitValidatedPersistedAndNeverVisibleToAdminOrOtherStudent() {
        var s = session(owner);
        var run =
                done(
                        owner,
                        students.submit(
                                owner,
                                UUID.fromString(s.id()),
                                "INSIGHT_REPORT",
                                new Message(UUID.randomUUID(), "Create report")));
        assertThat(run.status()).isEqualTo("SUCCEEDED");
        assertThat(run.reportId()).isNotNull();
        int calls = providerCalls.get();
        assertThat(students.report(owner, UUID.fromString(run.reportId())).sourceExecutionId())
                .isEqualTo(run.id());
        assertThat(providerCalls.get()).isEqualTo(calls);
        assertThatThrownBy(() -> students.report(other, UUID.fromString(run.reportId())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThatThrownBy(() -> students.report(admin, UUID.fromString(run.reportId())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThat(students.reports(owner)).hasSize(1);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {
                "bạn tạo cho tôi insight report để tôi tải về",
                "Please prepare a downloadable career insight report",
                "Tổng hợp minh chứng nghề nghiệp thành một báo cáo giúp mình"
            })
    void naturalReportCommandUsesSameAuthorizedEvidenceAndPersistsOnce(String request) {
        naturalAction =
                "INSIGHT_REPORT"; // Controlled model decision, not a production phrase matcher.
        var s = session(owner);
        var input = new Message(UUID.randomUUID(), request);
        var run = done(owner, students.submit(owner, UUID.fromString(s.id()), "CHAT", input));
        assertThat(run.status()).isEqualTo("SUCCEEDED");
        assertThat(run.taskType()).isEqualTo("CHAT");
        assertThat(run.output().path("requestedAction").asText()).isEqualTo("INSIGHT_REPORT");
        assertThat(run.reportId()).isNotNull();
        assertThat(students.report(owner, UUID.fromString(run.reportId())).sourceExecutionId())
                .isEqualTo(run.id());
        assertThat(students.submit(owner, UUID.fromString(s.id()), "CHAT", input).id())
                .isEqualTo(run.id());
        assertThat(students.reports(owner)).hasSize(1);
        assertThatThrownBy(() -> students.report(other, UUID.fromString(run.reportId())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThat(
                        students.history(owner, UUID.fromString(s.id()), 0, 100)
                                .get(1)
                                .content()
                                .path("text")
                                .asText())
                .isEqualTo(request);
        verify(model, atLeastOnce())
                .streamStructured(
                        anyString(),
                        contains("Student application actions contract v4"),
                        anyString(),
                        anyList(),
                        anyList(),
                        argThat(schema -> schema.path("properties").has("requestedAction")),
                        any());
    }

    @Test
    void ordinaryChatCannotSaveAnUnrequestedReportAndUnknownActionsFailClosed() {
        var s = session(owner);
        naturalAction = "NONE";
        var chat = done(owner, message(s, "Explain what an insight report is; do not create one"));
        assertThat(chat.status()).isEqualTo("SUCCEEDED");
        assertThat(chat.reportId()).isNull();
        assertThat(students.reports(owner)).isEmpty();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO insight_report(id,owner_user_id,requested_interaction_id,source_execution_id,schema_version,structured_insights,evidence_refs) SELECT ?,?,input_interaction_id,id,'careersim-insight-v1','{}'::jsonb,'[]'::jsonb FROM agent_execution WHERE id=?",
                                        UUID.randomUUID(),
                                        owner,
                                        UUID.fromString(chat.id())))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("EXPLICIT_VALID_OWNED_REPORT_EXECUTION_REQUIRED");
        naturalAction = "DELETE_ACCOUNT";
        assertThat(done(owner, message(s, "unsupported action fixture")).status())
                .isEqualTo("FAILED");
        assertThat(students.reports(owner)).isEmpty();
    }

    @Test
    void naturalPlanIsOnlyADraftUntilExactOwnerConfirmation() {
        naturalAction = "PLAN_SUGGESTION";
        var s = session(owner);
        var run = done(owner, message(s, "Gợi ý cho tôi một kế hoạch khám phá nghề nghiệp"));
        assertThat(run.status()).isEqualTo("SUCCEEDED");
        assertThat(run.planContentHash()).isNotBlank();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM exploration_plan WHERE student_id=?",
                                Integer.class,
                                owner))
                .isZero();
        var input =
                new ConfirmSuggestion(
                        UUID.fromString(run.id()),
                        UUID.randomUUID(),
                        run.planContentHash(),
                        null,
                        null);
        assertThatThrownBy(() -> students.confirm(other, input))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        var saved = students.confirm(owner, input);
        assertThat(students.confirm(owner, input).id()).isEqualTo(saved.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM exploration_plan WHERE student_id=?",
                                Integer.class,
                                owner))
                .isEqualTo(1);
    }

    @Test
    void invalidReportCannotPublishAndSuggestionRequiresExactExplicitConfirmation() {
        var s = session(owner);
        var bad =
                done(
                        owner,
                        students.submit(
                                owner,
                                UUID.fromString(s.id()),
                                "INSIGHT_REPORT",
                                new Message(UUID.randomUUID(), "invalid-schema report")));
        assertThat(bad.status()).isEqualTo("FAILED");
        assertThat(students.reports(owner)).isEmpty();
        var plan =
                done(
                        owner,
                        students.submit(
                                owner,
                                UUID.fromString(s.id()),
                                "PLAN_SUGGESTION",
                                new Message(UUID.randomUUID(), "Suggest draft")));
        assertThat(plan.status()).as("%s", plan).isEqualTo("SUCCEEDED");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM exploration_plan WHERE student_id=?",
                                Integer.class,
                                owner))
                .isZero();
        assertThatThrownBy(
                        () ->
                                students.confirm(
                                        owner,
                                        new ConfirmSuggestion(
                                                UUID.fromString(plan.id()),
                                                UUID.randomUUID(),
                                                "wrong",
                                                null,
                                                null)))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        var request =
                new ConfirmSuggestion(
                        UUID.fromString(plan.id()),
                        UUID.randomUUID(),
                        plan.planContentHash(),
                        null,
                        null);
        var confirmed = students.confirm(owner, request);
        assertThat(students.confirm(owner, request).id()).isEqualTo(confirmed.id());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM exploration_plan WHERE student_id=?",
                                Integer.class,
                                owner))
                .isEqualTo(1);
        students.delete(owner, UUID.fromString(s.id()));
        assertThatThrownBy(() -> students.confirm(owner, request))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @Test
    void adminUsageAggregatesModesAndNeverProjectsPrivateContent() {
        var s = session(owner);
        done(owner, message(s, "Private student narrative"));
        var result =
                CoreValues.write(
                        usage.query(
                                admin,
                                Instant.now().minusSeconds(120),
                                Instant.now().plusSeconds(10),
                                null));
        assertThat(result)
                .contains(
                        "STUDENT_CHAT",
                        "ADMIN_PREVIEW",
                        "assessment.lookup",
                        "unknown_cost_executions")
                .doesNotContain(
                        "Private student narrative",
                        "inputSnapshot",
                        "promptTemplate",
                        "report",
                        "@example.test");
        assertThatThrownBy(
                        () ->
                                usage.query(
                                        owner, Instant.now().minusSeconds(60), Instant.now(), null))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @Test
    void roleDeactivationAndAccountLockBlockAdmissionAndReplay() {
        var s = session(owner);
        var run = done(owner, message(s, "Explore"));
        activateRole(false);
        assertThatThrownBy(() -> students.read(owner, UUID.fromString(run.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThatThrownBy(() -> message(s, "Denied"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThat(students.history(owner, UUID.fromString(s.id()), 0, 100).get(2).unavailable())
                .isTrue();
        jdbc.update("UPDATE app_users SET status='LOCKED' WHERE id=?", owner);
        assertThatThrownBy(() -> students.history(owner, UUID.fromString(s.id()), 0, 100))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
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

    JsonNode gateway(
            java.net.http.HttpClient http,
            String origin,
            String method,
            String path,
            Object body,
            int expected)
            throws Exception {
        var request =
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(origin + "/api" + path))
                        .timeout(Duration.ofSeconds(15));
        if (!method.equals("GET")) {
            var csrf =
                    http.send(
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(origin + "/api/v2/auth/csrf"))
                                    .GET()
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            request.header(
                    "X-CSRF-TOKEN", json.readTree(csrf.body()).path("data").path("token").asText());
        }
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
                .as("%s %s: %s", method, path, response.body())
                .isEqualTo(expected);
        return json.readTree(response.body()).path("data");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"../frontend/nginx.conf", "../deployment/nginx/default.conf"})
    void productionGatewaysStreamBeforeTerminalReconnectCanonicalAndEnforceJwtCsrfTenant(
            String path) throws Exception {
        org.testcontainers.Testcontainers.exposeHostPorts(port);
        String configuration =
                java.nio.file.Files.readString(Path.of(path))
                        .replace(
                                "http://backend:8080/",
                                "http://host.testcontainers.internal:" + port + "/")
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
                                org.testcontainers.images.builder.Transferable.of(configuration),
                                "/etc/nginx/conf.d/default.conf")) {
            nginx.start();
            String origin = "http://localhost:" + nginx.getMappedPort(80);
            var browser = browser(owner, origin);
            gateway(
                    java.net.http.HttpClient.newHttpClient(),
                    origin,
                    "GET",
                    "/v2/agent-sessions",
                    null,
                    401);
            var session =
                    gateway(
                            browser,
                            origin,
                            "POST",
                            "/v2/agents/" + agent + "/sessions",
                            new CreateSession(UUID.randomUUID(), "Gateway conversation", List.of()),
                            200);
            String sid = session.path("id").asText();
            gateway(browser(other, origin), origin, "GET", "/v2/agent-sessions/" + sid, null, 404);
            var unsafe =
                    browser.send(
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(
                                                    origin
                                                            + "/api/v2/agent-sessions/"
                                                            + sid
                                                            + "/messages"))
                                    .header("Content-Type", "application/json")
                                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(unsafe.statusCode()).isEqualTo(403);
            var run =
                    gateway(
                            browser,
                            origin,
                            "POST",
                            "/v2/agent-sessions/" + sid + "/messages",
                            new Message(UUID.randomUUID(), "hold gateway stream"),
                            202);
            String rid = run.path("id").asText();
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            var stream =
                    browser.send(
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(
                                                    origin
                                                            + "/api/v2/agent-executions/"
                                                            + rid
                                                            + "/events"))
                                    .header("Accept", "text/event-stream")
                                    .timeout(Duration.ofSeconds(12))
                                    .GET()
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofInputStream());
            assertThat(stream.statusCode()).isEqualTo(200);
            assertThat(stream.headers().firstValue("content-type").orElse(""))
                    .contains("text/event-stream");
            long cursor = 0;
            boolean segment = false;
            try (var reader =
                    new java.io.BufferedReader(
                            new java.io.InputStreamReader(
                                    stream.body(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("id:")) cursor = Long.parseLong(line.substring(3).strip());
                    if (line.contains("First streamed sentence.")) {
                        segment = true;
                        break;
                    }
                }
            }
            assertThat(segment).isTrue();
            assertThat(students.read(owner, UUID.fromString(rid)).status()).isEqualTo("RUNNING");
            int calls = providerCalls.get();
            release.countDown();
            done(owner, students.read(owner, UUID.fromString(rid)));
            var resumed =
                    browser.send(
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(
                                                    origin
                                                            + "/api/v2/agent-executions/"
                                                            + rid
                                                            + "/events"))
                                    .header("Last-Event-ID", Long.toString(cursor))
                                    .timeout(Duration.ofSeconds(12))
                                    .GET()
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(resumed.statusCode()).isEqualTo(200);
            assertThat(resumed.body())
                    .contains("execution.completed")
                    .doesNotContain("First streamed sentence.", "promptTemplate", "functionCall");
            assertThat(providerCalls.get()).isEqualTo(calls);
            gateway(
                    browser(admin, origin),
                    origin,
                    "GET",
                    "/v2/agent-executions/" + rid,
                    null,
                    403);
            gateway(
                    browser(other, origin),
                    origin,
                    "GET",
                    "/v2/agent-executions/" + rid,
                    null,
                    404);
            gateway(browser, origin, "POST", "/v2/auth/logout", Map.of(), 200);
            gateway(browser, origin, "GET", "/v2/agent-sessions/" + sid, null, 401);
        }
    }

    UUID publishedStudentKb(String key, String text) {
        when(embedding.available()).thenReturn(true);
        when(embedding.profile()).thenReturn("synthetic:768:v1");
        when(embedding.embed(anyString(), anyBoolean()))
                .thenAnswer(
                        call -> {
                            double[] vector = new double[768];
                            vector[0] = 1;
                            return vector;
                        });
        var b =
                knowledge.create(
                        admin,
                        new edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts
                                .BaseInput("Synthetic " + key, null));
        UUID kb = UUID.fromString(b.id());
        var job =
                knowledge.upload(
                        admin,
                        kb,
                        new edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Upload(
                                key,
                                "Synthetic scoped source",
                                "Original synthetic fixture",
                                null,
                                "EN",
                                "BACKEND",
                                true,
                                UUID.randomUUID()),
                        "guide.txt",
                        "text/plain",
                        text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var ready = knowledge.retry(admin, kb, UUID.fromString(job.id()));
        assertThat(ready.state()).isEqualTo("READY");
        knowledge.publish(
                admin,
                kb,
                key,
                Long.parseLong(ready.producedVersionId()),
                knowledge.get(admin, kb).revision(),
                true);
        return kb;
    }

    @Test
    void studentKbDispatchRespectsPinnedGrantsAndSourceRevocation() {
        UUID granted = publishedStudentKb("grant", "Original authorized practice evidence."),
                foreign = publishedStudentKb("foreign", "Foreign hidden source must never appear.");
        var base = configuration();
        var c =
                registry.save(
                        admin,
                        agent,
                        registry.get(admin, agent).revision(),
                        new ConfigurationInput(
                                MODEL,
                                base.promptTemplate(),
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
                                base.memoryPolicy(),
                                CoreValues.tree(
                                        Map.of(
                                                "schemaVersion",
                                                1,
                                                "grounding",
                                                "KB_ONLY",
                                                "maxCharacters",
                                                4000)),
                                List.of(granted)));
        when(model.decide(anyString(), anyString(), anyString(), anyList(), anyBoolean()))
                .thenAnswer(
                        call -> {
                            if (call.getArgument(4, Boolean.class))
                                return new AgentModelProvider.Decision(
                                        CoreValues.tree(Map.of()),
                                        List.of(
                                                new AgentModelProvider.Call(
                                                        "kb-preview",
                                                        "kb.search",
                                                        CoreValues.tree(
                                                                Map.of(
                                                                        "query",
                                                                        "authorized practice")))),
                                        null);
                            var p =
                                    knowledge
                                            .search(
                                                    admin,
                                                    UUID.fromString(c.id()),
                                                    Set.of(granted),
                                                    "authorized practice",
                                                    3,
                                                    false)
                                            .get(0);
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
                                                    "Read the authorized practice source.",
                                                    "citations",
                                                    List.of(
                                                            Map.of(
                                                                    "referenceId",
                                                                    p.referenceId(),
                                                                    "quote",
                                                                    p.text())),
                                                    "suggestions",
                                                    List.of())));
                        });
        var preview =
                executions.preview(
                        admin,
                        agent,
                        UUID.fromString(c.id()),
                        new Preview(UUID.randomUUID(), "authorized practice"));
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(
                        () ->
                                executions
                                        .read(admin, UUID.fromString(preview.id()))
                                        .status()
                                        .equals("SUCCEEDED"));
        registry.publish(
                admin, agent, UUID.fromString(c.id()), registry.get(admin, agent).revision(), true);
        var s = session(owner);
        assertThatThrownBy(
                        () ->
                                knowledge.search(
                                        owner,
                                        UUID.fromString(c.id()),
                                        Set.of(foreign),
                                        "hidden",
                                        5,
                                        false))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        when(model.stream(anyString(), anyString(), anyString(), anyList(), anyList(), any()))
                .thenAnswer(
                        call -> {
                            List<AgentModelProvider.Exchange> history = call.getArgument(3);
                            if (history.isEmpty())
                                return new AgentModelProvider.Decision(
                                        CoreValues.tree(Map.of()),
                                        List.of(
                                                new AgentModelProvider.Call(
                                                        "kb-native",
                                                        "kb.search",
                                                        CoreValues.tree(
                                                                Map.of(
                                                                        "query",
                                                                        "authorized practice",
                                                                        "topK",
                                                                        2)))),
                                        null);
                            var p = history.get(0).observations().get(0).path("passages").path(0);
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
                                                    "Read this practice evidence.",
                                                    "citations",
                                                    List.of(
                                                            Map.of(
                                                                    "referenceId",
                                                                    p.path("referenceId").asText(),
                                                                    "quote",
                                                                    p.path("text").asText())),
                                                    "suggestions",
                                                    List.of())));
                        });
        var result = done(owner, message(s, "authorized practice"));
        assertThat(result.status()).as("%s", result).isEqualTo("SUCCEEDED");
        assertThat(result.toolStepsAndSources().toString())
                .contains(
                        "ACTUAL_NATIVE_CALL",
                        granted.toString(),
                        "Original authorized practice evidence.")
                .doesNotContain("Foreign hidden source");
        knowledge.archive(admin, granted, knowledge.get(admin, granted).revision());
        assertThatThrownBy(() -> students.read(owner, UUID.fromString(result.id())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    /**
     * Opt-in disposable UAT host. Bootstrap issues real test JWTs, never a production login route.
     */
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(
            named = "careersim.browser-fixture",
            matches = "true")
    void isolatedBrowserFixture() throws Exception {
        org.testcontainers.Testcontainers.exposeHostPorts(port);
        String configuration =
                java.nio.file.Files.readString(Path.of("../frontend/nginx.conf"))
                        .replace(
                                "http://backend:8080/",
                                "http://host.testcontainers.internal:" + port + "/");
        StringBuilder bootstrap = new StringBuilder();
        for (var pair :
                List.of(
                        Map.entry("student", owner),
                        Map.entry("other", other),
                        Map.entry("admin", admin))) {
            String token = auth.issue(pair.getValue()).value();
            bootstrap
                    .append("    location = /__fixture/")
                    .append(pair.getKey())
                    .append(" { add_header Set-Cookie \"CAREERSIM_AUTH=")
                    .append(token)
                    .append(
                            "; Path=/api; HttpOnly; SameSite=Lax\"; return 302 $scheme://$http_host/agents; }\n");
        }
        configuration = configuration.replace("    location / {", bootstrap + "    location / {");
        Path done = Path.of("target/p5-p6-browser.done");
        java.nio.file.Files.deleteIfExists(done);
        try (var nginx =
                new org.testcontainers.containers.GenericContainer<>("nginx:1.29-alpine")
                        .withExposedPorts(80)
                        .withCopyToContainer(
                                org.testcontainers.images.builder.Transferable.of(configuration),
                                "/etc/nginx/conf.d/default.conf")
                        .withCopyFileToContainer(
                                org.testcontainers.utility.MountableFile.forHostPath(
                                        Path.of("../frontend/dist").toAbsolutePath()),
                                "/usr/share/nginx/html")) {
            nginx.start();
            String origin = "http://localhost:" + nginx.getMappedPort(80);
            java.nio.file.Files.writeString(
                    Path.of("target/p5-p6-browser-fixture.json"),
                    json.writeValueAsString(
                            Map.of(
                                    "origin",
                                    origin,
                                    "agentId",
                                    agent.toString(),
                                    "studentBootstrap",
                                    "/__fixture/student",
                                    "otherBootstrap",
                                    "/__fixture/other",
                                    "adminBootstrap",
                                    "/__fixture/admin",
                                    "database",
                                    "ISOLATED_TESTCONTAINERS",
                                    "provider",
                                    "MOCKED",
                                    "googleOAuth",
                                    "NOT_EXECUTED")));
            long end = System.nanoTime() + Duration.ofMinutes(10).toNanos();
            while (!java.nio.file.Files.exists(done) && System.nanoTime() < end) Thread.sleep(500);
            assertThat(java.nio.file.Files.exists(done))
                    .as("Browser UAT explicitly signalled finished")
                    .isTrue();
        } finally {
            java.nio.file.Files.deleteIfExists(Path.of("target/p5-p6-browser-fixture.json"));
        }
    }
}
