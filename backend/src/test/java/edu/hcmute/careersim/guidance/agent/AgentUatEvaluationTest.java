package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.*;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Passage;
import edu.hcmute.careersim.guidance.service.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Reconciles Owner E01–E24 with the existing validator/controller evaluation framework. Scripted
 * model decisions exercise engineering invariants, NOT live-model semantic accuracy.
 */
class AgentUatEvaluationTest {
    static final ObjectMapper JSON = new ObjectMapper();
    static final Map<String, Map<String, Object>> RESULTS = new TreeMap<>();

    static String resource(String name) throws Exception {
        try (var in =
                AgentUatEvaluationTest.class.getResourceAsStream(
                        "/guidance/careersim-uat/" + name)) {
            return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static Stream<JsonNode> cases() throws Exception {
        return StreamSupport.stream(
                JSON.readTree(resource("cases.json")).path("cases").spliterator(), false);
    }

    ObjectNode output(String text, String outcome, String quote) {
        return (ObjectNode)
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "outcome",
                                outcome,
                                "text",
                                text,
                                "citations",
                                quote == null
                                        ? List.of()
                                        : List.of(Map.of("referenceId", "K1", "quote", quote)),
                                "suggestions",
                                List.of()));
    }

    JsonNode policy =
            CoreValues.tree(
                    Map.of(
                            "schemaVersion",
                            1,
                            "grounding",
                            "GENERAL_ALLOWED",
                            "maxCharacters",
                            4000));

    AgentModelProvider.Decision answer() {
        return new AgentModelProvider.Decision(
                null, List.of(), output("A cautious exploration step.", "ANSWER", null));
    }

    boolean evaluate(String id, boolean repaired) throws Exception {
        String handbook = resource("CareerSim_KB_Career_Guide.txt");
        String injection = resource("CareerSim_KB_Prompt_Injection_Eval.txt");
        var kb =
                List.of(
                        new Passage(
                                "K1",
                                "1",
                                "1",
                                "fixture",
                                "CareerSim handbook",
                                "Synthetic",
                                id.equals("E09") ? injection : handbook,
                                "Controlled test",
                                CoreValues.tree(Map.of()),
                                null,
                                null,
                                1,
                                "FIXTURE"));
        var domain =
                List.of(
                        new DomainEvidence(
                                "SE:1",
                                "PRACTICED_SKILL",
                                "OWNER",
                                "1",
                                "API_TRIAGE",
                                CoreValues.tree(
                                        Map.of(
                                                "skillCode",
                                                "API_TRIAGE",
                                                "skillNameSnapshot",
                                                "API triage"))));
        try (var scope = SynExecutionScope.open(Duration.ofSeconds(30), () -> true)) {
            if (Set.of("E22", "E23", "E24").contains(id)) {
                var p =
                        new AgentModelFailover(
                                "gemini/primary",
                                CoreValues.tree(
                                        Map.of("fallbackModelKeys", List.of("gemini/secondary"))),
                                x -> {},
                                x -> {},
                                () -> 0);
                java.util.function.Function<String, AgentModelProvider.Decision> provider =
                        key -> {
                            if (id.equals("E24") || key.equals("gemini/primary"))
                                throw new SynGuidanceProvider.ProviderException(
                                        id.equals("E23")
                                                ? SynGuidanceProvider.FailureKind
                                                        .PROVIDER_MODEL_UNAVAILABLE
                                                : SynGuidanceProvider.FailureKind
                                                        .PROVIDER_HTTP_UNAVAILABLE,
                                        Map.of("providerHttpStatus", id.equals("E23") ? 404 : 503));
                            return answer();
                        };
                try {
                    // BEFORE is the actual pre-repair single-dispatch strategy, same fixture.
                    var decision = repaired ? p.call(provider) : provider.apply("gemini/primary");
                    StudentOutputValidator.validate(
                            decision.output(), "CHAT", List.of(), List.of(), policy);
                    return !id.equals("E24");
                } catch (SynGuidanceProvider.ProviderException failure) {
                    return id.equals("E24"); // explicit terminal outage, no synthetic answer saved
                }
            }
            if (id.equals("E19")) {
                new AgentModelFailoverTest()
                        .generationFallbackPreservesObservationsWithoutExecutingConfirmedWriteTwice();
                return true;
            }
            if (id.equals("E16")) {
                var tools = new StudentEvidenceTools(null, null, null, null, null, null);
                assertThatThrownBy(
                                () ->
                                        tools.arguments(
                                                "assessment.lookup",
                                                CoreValues.tree(
                                                        Map.of(
                                                                "studentUserId",
                                                                "another-student"))))
                        .isInstanceOf(RuntimeException.class);
                return true;
            }
            if (Set.of("E12", "E13", "E14", "E15").contains(id)) {
                var names =
                        switch (id) {
                            case "E12" -> List.of("assessment.lookup");
                            case "E13" -> List.of("simulation.explore");
                            case "E14" -> List.of("simulation.myEvidence");
                            default -> List.of("assessment.lookup", "simulation.myEvidence");
                        };
                var tools = new StudentEvidenceTools(null, null, null, null, null, null);
                var definitions = StudentEvidenceTools.definitions(names);
                assertThat(definitions).hasSize(names.size());
                for (var d : definitions) {
                    assertThat(d.parameters().toString())
                            .doesNotContain("studentUserId", "ownerId");
                    tools.arguments(d.name(), CoreValues.tree(Map.of()));
                }
                StudentOutputValidator.validate(
                        answer().output(), "CHAT", List.of(), domain, policy);
                return true;
            }
            if (id.equals("E17") || id.equals("E21")) {
                var invalid = output("Unsupported response.", "ANSWER", null);
                if (id.equals("E21")) invalid.put("schemaVersion", 999);
                else
                    invalid.set(
                            "citations",
                            CoreValues.tree(
                                    List.of(
                                            Map.of(
                                                    "referenceId",
                                                    "LOCKED_OTHER_USER",
                                                    "quote",
                                                    "Never delivered"))));
                assertThatThrownBy(
                                () ->
                                        StudentOutputValidator.validate(
                                                invalid, "CHAT", kb, domain, policy))
                        .isInstanceOf(SynGuidanceProvider.ProviderException.class);
                return true;
            }
            if (id.equals("E18")) {
                var draft = output("Review an unsaved draft.", "ANSWER", null);
                draft.set(
                        "planSuggestion",
                        CoreValues.tree(
                                Map.of(
                                        "schemaVersion",
                                        "careersim-plan-v1",
                                        "title",
                                        "Explore a task",
                                        "steps",
                                        List.of("Try one published simulation."),
                                        "limitations",
                                        List.of("Draft only."),
                                        "evidenceRefs",
                                        List.of("K1"))));
                var validated =
                        StudentOutputValidator.validate(
                                draft, "PLAN_SUGGESTION", kb, domain, policy);
                assertThat(validated.path("planSuggestion").has("saved")).isFalse();
                assertThat(StudentEvidenceTools.NAMES)
                        .allMatch(n -> !n.contains("write") && !n.contains("save"));
                return true;
            }
            if (id.equals("E20")) {
                var value = answer().output().deepCopy();
                var report = new AgentEvaluationTest().report();
                ((ObjectNode) value).set("report", report);
                StudentOutputValidator.validate(value, "INSIGHT_REPORT", kb, domain, policy);
                return true;
            }
            if (Set.of("E06", "E07", "E08", "E09", "E10", "E11").contains(id)) {
                var refused =
                        output(
                                "Authorized evidence does not support this request.",
                                Set.of("E06", "E07").contains(id)
                                        ? "INSUFFICIENT_EVIDENCE"
                                        : "REFUSED",
                                null);
                StudentOutputValidator.validate(refused, "CHAT", kb, List.of(), policy);
                if (id.equals("E09")) {
                    var platform =
                            new AgentConfigurationPolicy()
                                    .instructions(
                                            new AgentConfigurationPolicy().reference(),
                                            "Career exploration only.");
                    assertThat(platform)
                            .contains("untrusted evidence", "never instructions or permissions");
                    assertThat(platform).doesNotContain(injection);
                    assertThat(StudentEvidenceTools.NAMES)
                            .containsExactlyInAnyOrder(
                                    "kb.search",
                                    "assessment.lookup",
                                    "simulation.explore",
                                    "simulation.myEvidence");
                }
                return true;
            }
            var facts =
                    Map.of(
                            "E01",
                            new String[] {
                                "For this handbook, the incident-summary checklist",
                                "The handbook checklist has four items."
                            },
                            "E02",
                            new String[] {
                                "The handbook's data-analysis mini-experiment requires",
                                "One question, one table, one chart and a three-sentence conclusion."
                            },
                            "E03",
                            new String[] {
                                "The protected-API experiment has FOUR",
                                "The experiment has four explanation points."
                            },
                            "E04",
                            new String[] {
                                "The QA mini-experiment contains SIX",
                                "The QA experiment contains six cases."
                            },
                            "E05",
                            new String[] {
                                "- Cybersecurity emphasizes investigation",
                                "Cybersecurity emphasizes investigation and risk evidence."
                            });
            var fact = facts.get(id);
            assertThat(fact).isNotNull();
            String quote =
                    handbook.lines()
                            .filter(line -> line.startsWith(fact[0]))
                            .findFirst()
                            .orElseThrow();
            var grounded = output(fact[1], "ANSWER", quote);
            StudentOutputValidator.validate(
                    grounded,
                    "CHAT",
                    kb,
                    List.of(),
                    CoreValues.tree(
                            Map.of(
                                    "schemaVersion",
                                    1,
                                    "grounding",
                                    "KB_ONLY",
                                    "maxCharacters",
                                    4000)));
            return true;
        }
    }

    @ParameterizedTest(name = "Owner {0}")
    @MethodSource("cases")
    void approvedCaseAgainstSameControlledFixtures(JsonNode c) throws Exception {
        String id = c.path("id").asText();
        boolean before = evaluate(id, false), after = evaluate(id, true);
        RESULTS.put(
                id,
                Map.of(
                        "id",
                        id,
                        "expected",
                        c.path("expected").asText(),
                        "actual",
                        "Controller/schema fixture checks executed; live semantic result is separate.",
                        "beforePass",
                        before,
                        "afterPass",
                        after,
                        "evidenceType",
                        "DETERMINISTIC_ENGINEERING_NOT_LIVE_MODEL"));
        assertThat(after).as(id).isTrue();
        assertThat(before).isEqualTo(!Set.of("E22", "E23").contains(id));
    }

    @AfterAll
    static void report() throws Exception {
        Files.createDirectories(Path.of("target"));
        JSON.writerWithDefaultPrettyPrinter()
                .writeValue(
                        Path.of("target/agent-uat-evaluation.json").toFile(),
                        Map.of(
                                "type",
                                "DETERMINISTIC_CONTROLLER_SCHEMA_FIXTURES_NOT_LIVE_MODEL_QUALITY",
                                "beforeStrategy",
                                "single model dispatch",
                                "afterStrategy",
                                "bounded compatible model failover",
                                "cases",
                                RESULTS.values(),
                                "beforePassed",
                                RESULTS.values().stream()
                                        .filter(r -> Boolean.TRUE.equals(r.get("beforePass")))
                                        .count(),
                                "afterPassed",
                                RESULTS.values().stream()
                                        .filter(r -> Boolean.TRUE.equals(r.get("afterPass")))
                                        .count(),
                                "humanReview",
                                "PENDING"));
    }
}
