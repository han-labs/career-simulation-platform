package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.*;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Passage;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Executable engineering cases, not a claim of human-approved or live-model quality. */
class AgentEvaluationTest {
    static final ObjectMapper JSON = new ObjectMapper();
    static final List<Map<String, Object>> RESULTS = new ArrayList<>();

    static Stream<JsonNode> cases() throws Exception {
        try (var in =
                AgentEvaluationTest.class.getResourceAsStream(
                        "/guidance/agent-evaluation-cases.json")) {
            return java.util.stream.StreamSupport.stream(
                    JSON.readTree(in).path("cases").spliterator(), false);
        }
    }

    ObjectNode report() {
        return (ObjectNode)
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                "careersim-insight-v1",
                                "summary",
                                "A cautious exploration hypothesis.",
                                "hypotheses",
                                List.of(
                                        Map.of(
                                                "direction",
                                                "Explore a practical task",
                                                "reason",
                                                "Practice can help exploration.",
                                                "evidenceRefs",
                                                List.of("K1"))),
                                "strengthsPracticed",
                                List.of("API_TRIAGE"),
                                "difficulties",
                                List.of(),
                                "limitations",
                                List.of("Synthetic evidence only; no proficiency certification."),
                                "nextSteps",
                                List.of("Try a focused practice task."),
                                "disclaimer",
                                "Exploration support only; this report does not certify professional competence or determine a career.",
                                "evidenceRefs",
                                List.of("K1", "SE:1")));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("cases")
    void reviewedEngineeringCase(JsonNode c) {
        var output =
                (ObjectNode)
                        CoreValues.tree(
                                Map.of(
                                        "schemaVersion",
                                        1,
                                        "outcome",
                                        "ANSWER",
                                        "text",
                                        "Explore this documented practice.",
                                        "citations",
                                        List.of(
                                                Map.of(
                                                        "referenceId",
                                                        "K1",
                                                        "quote",
                                                        "Original reviewed practice.")),
                                        "suggestions",
                                        List.of()));
        var policy =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "grounding",
                                "GENERAL_ALLOWED",
                                "maxCharacters",
                                4000));
        String task = c.path("task").asText();
        if (task.equals("INSIGHT_REPORT")) output.set("report", report());
        if (task.equals("PLAN_SUGGESTION"))
            output.set(
                    "planSuggestion",
                    CoreValues.tree(
                            Map.of(
                                    "schemaVersion",
                                    "careersim-plan-v1",
                                    "title",
                                    "Explore practice",
                                    "steps",
                                    List.of("Try one task."),
                                    "limitations",
                                    List.of("Draft only."),
                                    "evidenceRefs",
                                    List.of("K1"))));
        switch (c.path("mutation").asText()) {
            case "refusal" -> {
                output.put("outcome", "REFUSED");
                output.set("citations", JSON.createArrayNode());
            }
            case "empty_grounding" -> {
                ((ObjectNode) policy).put("grounding", "KB_ONLY");
                output.set("citations", JSON.createArrayNode());
            }
            case "bad_quote" ->
                    ((ObjectNode) output.path("citations").path(0))
                            .put("quote", "Fabricated quotation");
            case "bad_reference" ->
                    ((ObjectNode) output.path("citations").path(0)).put("referenceId", "K999");
            case "extra" -> output.put("secret", true);
            case "schema" -> output.put("schemaVersion", 2);
            case "html" -> output.put("text", "<script>bad()</script>");
            case "score" -> output.put("text", "Your score is 99");
            case "certainty" -> output.put("text", "You must choose this career");
            case "bad_hypothesis" ->
                    ((ObjectNode) output.path("report").path("hypotheses").path(0))
                            .set("evidenceRefs", CoreValues.tree(List.of("FOREIGN")));
            case "bad_skill" ->
                    ((ObjectNode) output.path("report"))
                            .set("strengthsPracticed", CoreValues.tree(List.of("EXPERT_ENGINEER")));
            case "missing_disclaimer" -> ((ObjectNode) output.path("report")).remove("disclaimer");
            case "bad_disclaimer" ->
                    ((ObjectNode) output.path("report"))
                            .put("disclaimer", "Professional competence is certified.");
            case "unsolicited" -> output.set("report", report());
            case "too_many_steps" ->
                    ((ObjectNode) output.path("planSuggestion"))
                            .set("steps", CoreValues.tree(Collections.nCopies(6, "Extra step")));
            case "bad_plan_reference" ->
                    ((ObjectNode) output.path("planSuggestion"))
                            .set("evidenceRefs", CoreValues.tree(List.of("FOREIGN")));
            case "bidi" -> output.put("text", "Hidden \u202e directive");
            default -> {}
        }
        var kb =
                List.of(
                        new Passage(
                                "K1",
                                "1",
                                "1",
                                "synthetic-kb",
                                "guide",
                                "Synthetic evidence",
                                "Original reviewed practice.",
                                "Test",
                                CoreValues.tree(Map.of("fixture", true)),
                                null,
                                null,
                                1,
                                "SYNTHETIC"));
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
        boolean before, after;
        try {
            AgentOutputValidator.validate(output, kb, policy);
            before = true;
        } catch (RuntimeException invalid) {
            before = false;
        }
        JsonNode validated = null;
        try {
            validated = StudentOutputValidator.validate(output, task, kb, domain, policy);
            after = true;
        } catch (RuntimeException invalid) {
            after = false;
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("id", c.path("id").asText());
        result.put("case", c);
        result.put("provider", "NONE_SYNTHETIC_VALIDATOR_ONLY");
        result.put("configurationHash", CoreValues.hash(policy));
        result.put("sourceRefs", List.of("K1", "SE:1"));
        result.put("input", output);
        result.put("validatedOutput", validated);
        result.put("baselineValidatorAccepted", before);
        result.put("currentValidatorAccepted", after);
        result.put("toolCalls", List.of());
        result.put("tokens", null);
        result.put("latencyMs", null);
        result.put("estimatedCost", null);
        RESULTS.add(result);
        assertThat(after)
                .as(c.path("id").asText() + " " + c.path("name").asText())
                .isEqualTo(c.path("expectedValid").asBoolean());
    }

    @AfterAll
    static void evidence() throws Exception {
        var path = Path.of("target/agent-evaluation-results.json");
        Files.createDirectories(path.getParent());
        Files.writeString(
                path,
                JSON.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(
                                        Map.of(
                                                "execution",
                                                "ACTUAL_DETERMINISTIC_VALIDATOR_COMPARISON",
                                                "liveModelResults",
                                                "NOT_EXECUTED",
                                                "humanReview",
                                                "PENDING",
                                                "results",
                                                RESULTS))
                        + "\n");
    }
}
