package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.*;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.ConfigurationInput;
import java.util.*;
import org.junit.jupiter.api.Test;

class AgentConfigurationPolicyTest {
    final AgentConfigurationPolicy policy = new AgentConfigurationPolicy();

    ConfigurationInput input() {
        return new ConfigurationInput(
                "synthetic/model",
                "Use authorized evidence.",
                null,
                CoreValues.tree(
                        Map.of(
                                "enabled",
                                List.of(),
                                "maxRounds",
                                2,
                                "maxToolCalls",
                                0,
                                "topK",
                                5,
                                "timeoutSeconds",
                                30)),
                CoreValues.tree(Map.of("enabled", false)),
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

    @Test
    void noImplicitGrantsOrToolsAndHashChangesWithPayload() {
        var c = input();
        policy.validate(c, Set.of("synthetic/model"));
        assertThat(c.knowledgeBaseIds()).isEmpty();
        assertThat(c.toolPolicy().path("enabled")).isEmpty();
        assertThat(policy.hash(c)).isEqualTo(policy.hash(input()));
        var changed =
                new ConfigurationInput(
                        c.modelKey(),
                        "Different reviewed instructions.",
                        null,
                        c.toolPolicy(),
                        c.memoryPolicy(),
                        c.outputPolicy(),
                        c.knowledgeBaseIds());
        assertThat(policy.hash(changed)).isNotEqualTo(policy.hash(c));
        assertThatThrownBy(
                        () ->
                                policy.instructions(
                                        "unavailable-template@oldhash", c.promptTemplate()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsUnsupportedModelsPoliciesCredentialsAndDuplicateScope() {
        var c = input();
        assertThatThrownBy(() -> policy.validate(c, Set.of())).isInstanceOf(RuntimeException.class);
        for (String prompt :
                List.of("password=synthetic-only", "Bearer synthetic-invalid-token-123456")) {
            assertThatThrownBy(
                            () ->
                                    policy.validate(
                                            new ConfigurationInput(
                                                    c.modelKey(),
                                                    prompt,
                                                    null,
                                                    c.toolPolicy(),
                                                    c.memoryPolicy(),
                                                    c.outputPolicy(),
                                                    List.of()),
                                            Set.of(c.modelKey())))
                    .isInstanceOf(RuntimeException.class);
        }
        var bad = c.toolPolicy().deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) bad).put("extra", true);
        assertThatThrownBy(
                        () ->
                                policy.validate(
                                        new ConfigurationInput(
                                                c.modelKey(),
                                                c.promptTemplate(),
                                                null,
                                                bad,
                                                c.memoryPolicy(),
                                                c.outputPolicy(),
                                                List.of()),
                                        Set.of(c.modelKey())))
                .isInstanceOf(RuntimeException.class);
        UUID kb = UUID.randomUUID();
        assertThatThrownBy(
                        () ->
                                policy.validate(
                                        new ConfigurationInput(
                                                c.modelKey(),
                                                c.promptTemplate(),
                                                null,
                                                c.toolPolicy(),
                                                c.memoryPolicy(),
                                                c.outputPolicy(),
                                                List.of(kb, kb)),
                                        Set.of(c.modelKey())))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void strictJsonAndCitationsFailClosed() {
        var output =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "outcome",
                                "ANSWER",
                                "text",
                                "Practice safely.",
                                "citations",
                                List.of(Map.of("referenceId", "K999", "quote", "Invented quote")),
                                "suggestions",
                                List.of()));
        assertThatThrownBy(
                        () ->
                                AgentOutputValidator.validate(
                                        output, List.of(), input().outputPolicy()))
                .isInstanceOf(
                        edu.hcmute.careersim.guidance.service.SynGuidanceProvider.ProviderException
                                .class);
        ((com.fasterxml.jackson.databind.node.ObjectNode) output)
                .set("citations", CoreValues.tree(List.of()));
        assertThat(AgentOutputValidator.validate(output, List.of(), input().outputPolicy()))
                .isEqualTo(output);
        for (String unsafe :
                List.of(
                        "You must choose this career.",
                        "<script>unsafe</script>",
                        "Your score is 99")) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) output)
                    .set("suggestions", CoreValues.tree(List.of(unsafe)));
            assertThatThrownBy(
                            () ->
                                    AgentOutputValidator.validate(
                                            output, List.of(), input().outputPolicy()))
                    .isInstanceOf(
                            edu.hcmute.careersim.guidance.service.SynGuidanceProvider
                                    .ProviderException.class);
        }
        ((com.fasterxml.jackson.databind.node.ObjectNode) output)
                .set("suggestions", CoreValues.tree(List.of()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) output)
                .put("privateExtra", "must reject");
        assertThatThrownBy(
                        () ->
                                AgentOutputValidator.validate(
                                        output, List.of(), input().outputPolicy()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void unknownCitationReturnsOnlySafeValidationCategoryAndGreetingRespectsGrounding() {
        var answer =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "outcome",
                                "ANSWER",
                                "text",
                                "Hello. What career would you like to explore?",
                                "citations",
                                List.of(),
                                "suggestions",
                                List.of()));
        assertThat(AgentOutputValidator.validate(answer, List.of(), input().outputPolicy()))
                .isEqualTo(answer);
        var strict =
                CoreValues.tree(
                        Map.of("schemaVersion", 1, "grounding", "KB_ONLY", "maxCharacters", 4000));
        assertThatThrownBy(() -> AgentOutputValidator.validate(answer, List.of(), strict))
                .isInstanceOfSatisfying(
                        edu.hcmute.careersim.guidance.service.SynGuidanceProvider.ProviderException
                                .class,
                        e ->
                                assertThat(e.kind())
                                        .isEqualTo(
                                                edu.hcmute.careersim.guidance.service
                                                        .SynGuidanceProvider.FailureKind
                                                        .EMPTY_EVIDENCE));
        ((com.fasterxml.jackson.databind.node.ObjectNode) answer)
                .set(
                        "citations",
                        CoreValues.tree(
                                List.of(
                                        Map.of(
                                                "referenceId",
                                                "K999",
                                                "quote",
                                                "Synthetic quote"))));
        assertThatThrownBy(
                        () ->
                                AgentOutputValidator.validate(
                                        answer, List.of(), input().outputPolicy()))
                .isInstanceOfSatisfying(
                        edu.hcmute.careersim.guidance.service.SynGuidanceProvider.ProviderException
                                .class,
                        e ->
                                assertThat(e.diagnostics())
                                        .containsExactlyEntriesOf(
                                                Map.of(
                                                        "validationCheck",
                                                        "CITATION_NOT_DELIVERED")));
    }
}
