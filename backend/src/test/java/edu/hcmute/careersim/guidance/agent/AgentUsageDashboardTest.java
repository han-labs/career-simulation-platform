package edu.hcmute.careersim.guidance.agent;

import static edu.hcmute.careersim.guidance.agent.dto.AgentUsageContracts.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.service.AgentUsageService;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
        properties = {
            "app.agent-platform-enabled=true",
            "app.core-v2-enabled=true",
            "app.ai.provider=disabled",
            "app.syn.cleanup-enabled=false",
            "server.servlet.session.cookie.secure=false"
        })
@AutoConfigureMockMvc
@Testcontainers
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class AgentUsageDashboardTest {
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
    @Autowired AgentUsageService service;
    @Autowired MockMvc mvc;
    @Autowired BrowserAuthentication auth;
    long admin, student;
    UUID kb, agent, config;
    static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z"),
            TO = FROM.plus(Duration.ofDays(7));

    long user(String role) {
        return jdbc.queryForObject(
                "INSERT INTO app_users(email,password_hash,display_name,role) VALUES (?,'synthetic-only','Synthetic usage fixture',?) RETURNING id",
                Long.class,
                UUID.randomUUID() + "@example.test",
                role);
    }

    @BeforeEach
    void fixture() {
        admin = user("ADMINISTRATOR");
        student = user("STUDENT");
        jdbc.update("INSERT INTO student_profiles(user_id) VALUES (?)", student);
        kb = UUID.randomUUID();
        agent = UUID.randomUUID();
        config = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO knowledge_base(id,name,scope_type,created_by_admin_id) VALUES (?,'Usage fixture','PLATFORM',?)",
                kb,
                admin);
        jdbc.update(
                "INSERT INTO agent(id,name,purpose,created_by_admin_id) VALUES (?,'Historical guide','Private purpose not projected',?)",
                agent,
                admin);
        jdbc.update(
                "INSERT INTO agent_configuration(id,agent_id,version_number,model_key,prompt_template,prompt_template_ref,prompt_hash,tool_policy,memory_policy,output_policy,created_by_admin_id,content_hash,sealed) VALUES (?,?,3,'gemini/fixture','PRIVATE_PROMPT','PRIVATE_INTERNAL_REFERENCE',?,'{}','{}','{}',?,?,true)",
                config,
                agent,
                "c".repeat(64),
                admin,
                "d".repeat(64));
    }

    Filter filter(UUID a, String provider, String model, String outcome, int limit, String cursor) {
        return new Filter(FROM, TO, a, provider, model, outcome, null, limit, cursor);
    }

    Dashboard read() {
        return service.dashboard(admin, filter(null, null, null, null, 20, null));
    }

    UUID row(
            String status,
            String costStatus,
            BigDecimal cost,
            Long latency,
            Instant at,
            boolean linked,
            String model) {
        UUID id = UUID.randomUUID();
        boolean pending = Set.of("ACCEPTED", "RUNNING").contains(status);
        jdbc.update(
                """
            INSERT INTO agent_execution(id,execution_mode,task_type,initiated_by_user_id,configuration_id,knowledge_base_id,
              input_snapshot,request_id,request_hash,status,model_key,prompt_hash,configuration_hash,privacy_version,
              started_at,finished_at,latency_ms,cost_status,estimated_cost,input_tokens,output_tokens,output_snapshot,
              refusal_or_error_code,tool_steps_and_sources,pricing_snapshot)
            VALUES (?,?,?,?,?,?,?::jsonb,?,?,?,?,?,?,0,?,?,?,?,?,?,?,?::jsonb,?,?::jsonb,?::jsonb)
            """,
                id,
                linked ? "ADMIN_PREVIEW" : "KB_TEST",
                linked ? "CHAT" : "KB_QUERY",
                admin,
                linked ? config : null,
                linked ? null : kb,
                "{\"query\":\"PRIVATE_STUDENT_MESSAGE\"}",
                UUID.randomUUID(),
                "e".repeat(64),
                status,
                model,
                "c".repeat(64),
                linked ? "d".repeat(64) : null,
                Timestamp.from(at),
                pending ? null : Timestamp.from(at.plusSeconds(1)),
                latency,
                costStatus,
                cost,
                "ESTIMATED".equals(costStatus) ? 10L : null,
                "ESTIMATED".equals(costStatus) ? 2L : null,
                "{\"text\":\"PRIVATE_PROVIDER_RESPONSE\"}",
                "FAILED".equals(status)
                        ? "TIMEOUT_OR_TRANSPORT"
                        : "REFUSED".equals(status) ? "EMPTY_EVIDENCE" : null,
                "[{\"type\":\"ACTUAL_NATIVE_CALL\",\"name\":\"assessment.lookup\",\"status\":\"SUCCEEDED\",\"arguments\":\"PRIVATE_ARGUMENTS\"},{\"type\":\"ACTUAL_NATIVE_CALL\",\"name\":\"PRIVATE_UNKNOWN_TOOL\",\"status\":\"DENIED\"},{\"type\":\"SERVER_RETRIEVAL\",\"sources\":\"PRIVATE_PASSAGES\"}]",
                "{\"generation\":{\"calls\":[{\"secret\":\"PRIVATE_KEY\"}]},\"embeddingResources\":{\"private\":\"PRIVATE_RESOURCE\"}}");
        return id;
    }

    @Test
    void aggregatesRealCountsCostCoverageWeightedLatencyAndDistinctOutcomes() {
        row("SUCCEEDED", "ESTIMATED", new BigDecimal("1.25"), 100L, FROM, true, "gemini/fixture");
        row(
                "SUCCEEDED",
                "PARTIAL",
                new BigDecimal("0.02"),
                300L,
                FROM.plusSeconds(10),
                false,
                "openai/fixture");
        row("FAILED", "UNKNOWN", null, null, FROM.plusSeconds(20), false, "openai/fixture");
        row("REFUSED", "NOT_APPLICABLE", null, 500L, FROM.plusSeconds(30), false, null);
        row("CANCELLED", "UNKNOWN", null, 700L, FROM.plusSeconds(40), false, "gemini/fixture");
        row("RUNNING", "UNKNOWN", null, 99999L, FROM.plusSeconds(50), false, "gemini/fixture");
        var s = read().summary();
        assertThat(s.requests()).isEqualTo(6);
        assertThat(s.succeeded()).isEqualTo(2);
        assertThat(s.failed()).isEqualTo(1);
        assertThat(s.refused()).isEqualTo(1);
        assertThat(s.cancelled()).isEqualTo(1);
        assertThat(s.pending()).isEqualTo(1);
        assertThat(s.recordedEstimatedCostUsd()).isEqualByComparingTo("1.27");
        assertThat(s.estimatedCostExecutions()).isEqualTo(1);
        assertThat(s.partialCostExecutions()).isEqualTo(1);
        assertThat(s.unknownCostExecutions()).isEqualTo(3);
        assertThat(s.notApplicableCostExecutions()).isEqualTo(1);
        assertThat(s.averageLatencyMs()).isEqualByComparingTo("400");
        assertThat(s.latencySamples()).isEqualTo(4);
        assertThat(s.missingLatencyExecutions()).isEqualTo(1);
    }

    @Test
    void unknownAndNoModelAreNotZeroAndEmptyDoesNotFabricateMetrics() {
        var empty = read();
        assertThat(empty.summary().requests()).isZero();
        assertThat(empty.summary().averageLatencyMs()).isNull();
        assertThat(empty.summary().recordedEstimatedCostUsd()).isNull();
        assertThat(empty.trend()).isEmpty();
        assertThat(empty.executions()).isEmpty();
        assertThat(empty.nextCursor()).isNull();
        row("REFUSED", "NOT_APPLICABLE", null, null, FROM, false, null);
        row("FAILED", "UNKNOWN", null, null, FROM.plusSeconds(1), false, "gemini/fixture");
        assertThat(read().summary().recordedEstimatedCostUsd()).isNull();
        assertThat(read().summary().averageLatencyMs()).isNull();
        assertThat(read().executions()).allSatisfy(x -> assertThat(x.estimatedCostUsd()).isNull());
    }

    @Test
    void filtersAgentModelProviderOutcomeModeAndHalfOpenRangeOnServer() {
        row("FAILED", "UNKNOWN", null, 10L, FROM, true, "gemini/fixture");
        row("REFUSED", "UNKNOWN", null, 20L, FROM.plusSeconds(1), false, "openai/fixture");
        row("FAILED", "UNKNOWN", null, 30L, TO, false, "gemini/fixture");
        assertThat(read().summary().requests()).isEqualTo(2);
        for (var f :
                List.of(
                        filter(agent, null, null, null, 20, null),
                        filter(null, "gemini", null, null, 20, null),
                        filter(null, null, "gemini/fixture", null, 20, null),
                        filter(null, null, null, "FAILED", 20, null),
                        new Filter(FROM, TO, null, null, null, null, "ADMIN_PREVIEW", 20, null))) {
            var d = service.dashboard(admin, f);
            assertThat(d.summary().requests()).isEqualTo(1);
            assertThat(d.executions()).hasSize(1);
            assertThat(d.trend().get(0).requests()).isEqualTo(1);
        }
        assertThat(
                        service.dashboard(
                                        admin,
                                        filter(UUID.randomUUID(), null, null, null, 20, null))
                                .summary()
                                .requests())
                .isZero();
    }

    @Test
    void stableKeysetPaginationDoesNotDuplicateTiedTimestampsAndKeepsSummaryGlobal() {
        var ids = new ArrayList<UUID>();
        for (int i = 0; i < 5; i++)
            ids.add(row("FAILED", "UNKNOWN", null, 1L, FROM, false, "gemini/fixture"));
        ids.sort(Comparator.comparing(UUID::toString).reversed());
        var all = new ArrayList<String>();
        String cursor = null;
        do {
            var d = service.dashboard(admin, filter(null, null, null, null, 2, cursor));
            assertThat(d.summary().requests()).isEqualTo(5);
            all.addAll(d.executions().stream().map(Execution::id).toList());
            cursor = d.nextCursor();
        } while (cursor != null);
        assertThat(all).containsExactlyElementsOf(ids.stream().map(UUID::toString).toList());
    }

    @Test
    void utcBucketsAreExplicitAndPromptToolProjectionIsMinimalEvenForArchivedAgent() {
        row(
                "FAILED",
                "UNKNOWN",
                null,
                9L,
                Instant.parse("2026-01-01T23:30:00Z"),
                true,
                "gemini/fixture");
        row("REFUSED", "UNKNOWN", null, 9L, Instant.parse("2026-01-02T00:30:00Z"), false, null);
        jdbc.update("UPDATE agent SET status='ARCHIVED' WHERE id=?", agent);
        jdbc.execute("SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh'");
        var d = read();
        assertThat(d.trend())
                .extracting(Bucket::startedAt)
                .containsExactly(FROM, FROM.plus(Duration.ofDays(1)));
        var linked =
                d.executions().stream().filter(x -> x.agentId() != null).findFirst().orElseThrow();
        assertThat(linked.agentName()).isEqualTo("Historical guide");
        assertThat(linked.configurationVersion()).isEqualTo(3);
        assertThat(linked.promptHash()).isEqualTo("c".repeat(64));
        assertThat(linked.nativeToolCount()).isEqualTo(2);
        assertThat(linked.serverRetrievalCount()).isEqualTo(1);
        assertThat(linked.tools()).containsExactly("UNKNOWN", "assessment.lookup");
        assertThat(linked.modelAttempts()).isEqualTo(1);
        assertThat(linked.failureCategory()).isEqualTo("TIMEOUT_OR_TRANSPORT");
        assertThat(d.executions()).anySatisfy(x -> assertThat(x.agentName()).isNull());
        assertThat(CoreValues.write(d))
                .doesNotContain(
                        "PRIVATE_",
                        "promptTemplate",
                        "inputSnapshot",
                        "outputSnapshot",
                        "pricingSnapshot",
                        "initiatedByUserId",
                        "studentUserId",
                        "arguments",
                        "@example.test");
    }

    @Test
    void unknownFailureCodesAreRedactedRatherThanReturnedAsProviderMessages() {
        UUID id = row("RUNNING", "UNKNOWN", null, null, FROM, false, "gemini/fixture");
        jdbc.update(
                "UPDATE agent_execution SET status='FAILED',finished_at=?,refusal_or_error_code='PRIVATE_RAW_PROVIDER_SECRET' WHERE id=?",
                Timestamp.from(FROM.plusSeconds(1)),
                id);
        assertThat(read().executions().get(0).failureCategory()).isEqualTo("OTHER");
    }

    @Test
    void modelFailoverProjectionIncludesOnlyAllowlistedOperationalMetadata() {
        UUID id = row("RUNNING", "UNKNOWN", null, 100L, FROM, true, "gemini/fixture");
        jdbc.update(
                "UPDATE agent_execution SET tool_steps_and_sources=?::jsonb WHERE id=?",
                """
                [{"type":"MODEL_ATTEMPT","requestedModel":"gemini/fixture","effectiveModel":"gemini/fixture","reason":"PRIMARY","privatePrompt":"PRIVATE_PROMPT"},
                 {"type":"MODEL_ATTEMPT","requestedModel":"gemini/fixture","effectiveModel":"gemini/fallback","reason":"PROVIDER_HTTP_UNAVAILABLE","rawResponse":"PRIVATE_RESPONSE"},
                 {"type":"MODEL_ATTEMPT","requestedModel":"gemini/fixture","effectiveModel":"PRIVATE_RAW_MODEL","reason":"PRIVATE_RAW_REASON","credential":"PRIVATE_KEY"}]
                """,
                id);
        jdbc.update(
                "UPDATE agent_execution SET status='SUCCEEDED',finished_at=? WHERE id=?",
                Timestamp.from(FROM.plusSeconds(1)),
                id);
        var dashboard = read();
        var execution = dashboard.executions().get(0);
        assertThat(execution.model()).isEqualTo("gemini/fixture");
        assertThat(execution.effectiveModel()).isEqualTo("gemini/fallback");
        assertThat(execution.fallbackReasons()).containsExactly("PROVIDER_HTTP_UNAVAILABLE");
        assertThat(CoreValues.write(dashboard))
                .doesNotContain("PRIVATE_", "privatePrompt", "rawResponse", "credential");
    }

    @Test
    void rejectsInvalidBoundsAndInputsIncludingOversizedWindowByOneNanosecond() {
        for (var f :
                List.of(
                        new Filter(FROM, FROM, null, null, null, null, null, 20, null),
                        new Filter(TO, FROM, null, null, null, null, null, 20, null),
                        new Filter(
                                FROM,
                                FROM.plus(Duration.ofDays(366)).plusNanos(1),
                                null,
                                null,
                                null,
                                null,
                                null,
                                20,
                                null),
                        filter(null, null, null, null, 0, null),
                        filter(null, null, null, null, 51, null),
                        filter(null, null, null, "TIMEOUT", 20, null),
                        filter(null, "bad provider", null, null, 20, null),
                        filter(null, null, "' OR 1=1", null, 20, null),
                        filter(null, null, null, null, 20, "not-a-cursor"),
                        filter(null, null, null, null, 20, "x".repeat(257)))) {
            assertThatThrownBy(() -> service.dashboard(admin, f))
                    .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        }
        assertThat(
                        service.dashboard(
                                        admin,
                                        new Filter(
                                                FROM,
                                                FROM.plus(Duration.ofDays(366)),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                50,
                                                null))
                                .bucketUnit())
                .isEqualTo("day");
        assertThat(
                        service.dashboard(
                                        admin,
                                        new Filter(
                                                FROM,
                                                FROM.plus(Duration.ofHours(24)),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                20,
                                                null))
                                .bucketUnit())
                .isEqualTo("hour");
    }

    String url() {
        return "/v2/admin/agent-usage?view=dashboard&from=" + FROM + "&to=" + TO;
    }

    jakarta.servlet.http.Cookie cookie(long uid) {
        return new jakarta.servlet.http.Cookie("CAREERSIM_AUTH", auth.issue(uid).value());
    }

    @Test
    void adminOnlyThroughRealSecurityAndLegacyAggregateRemainsCompatible() throws Exception {
        mvc.perform(get(url())).andExpect(status().isUnauthorized());
        mvc.perform(get(url()).cookie(cookie(student))).andExpect(status().isForbidden());
        mvc.perform(get(url()).cookie(cookie(admin))).andExpect(status().isOk());
        mvc.perform(get(url() + "&agent=bad").cookie(cookie(admin)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get(url() + "&limit=51").cookie(cookie(admin)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/v2/admin/agent-usage?from=" + FROM + "&to=" + TO).cookie(cookie(admin)))
                .andExpect(status().isOk());
        assertThatThrownBy(
                        () -> service.dashboard(student, filter(null, null, null, null, 20, null)))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }
}
