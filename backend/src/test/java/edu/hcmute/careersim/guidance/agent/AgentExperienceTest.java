package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.AgentExperience;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AgentExperienceTest {
    @Test
    void dynamicVariablesAreLiteralBoundedUniqueAndCompatibleWithOldV2() {
        var n = valid();
        var oldGreeting = AgentExperience.greeting(n);
        n.set(
                "variables",
                CoreValues.tree(
                        java.util.List.of(
                                Map.of("key", "name", "value", "Lan Nguyễn 🧪"),
                                Map.of("key", "school", "value", "HCMUTE"),
                                Map.of("key", "tone", "value", "$friendly"))));
        AgentExperience.validate(n, "Hello {{name}} from {{school}}; {{tone}}.");
        assertThat(AgentExperience.greeting(n)).isEqualTo(oldGreeting);
        assertThat(AgentExperience.render("{{school}} {{tone}}", n)).isEqualTo("HCMUTE $friendly");
        n.set(
                "variables",
                CoreValues.tree(java.util.List.of(Map.of("key", "name", "value", "{{school}}"))));
        assertThatThrownBy(() -> AgentExperience.validate(n, "{{name}}"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1bad", "has space", "__proto__", "api_key", "name-name"})
    void invalidVariableKeysAreRejected(String key) {
        var n = valid();
        n.set(
                "variables",
                CoreValues.tree(java.util.List.of(Map.of("key", key, "value", "plain"))));
        assertThatThrownBy(() -> AgentExperience.validate(n, "Text"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @Test
    void duplicateAndOversizedCollectionsFailBeforeSaving() {
        var n = valid();
        n.set(
                "variables",
                CoreValues.tree(
                        java.util.List.of(
                                Map.of("key", "name", "value", "one"),
                                Map.of("key", "name", "value", "two"))));
        assertThatThrownBy(() -> AgentExperience.validate(n, "Text"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        n.set(
                "variables",
                CoreValues.tree(
                        java.util.stream.IntStream.range(0, 21)
                                .mapToObj(i -> Map.of("key", "v" + i, "value", "value"))
                                .toList()));
        assertThatThrownBy(() -> AgentExperience.validate(n, "Text"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        n.set("variables", CoreValues.tree(Map.of("name", "a".repeat(501))));
        assertThatThrownBy(() -> AgentExperience.validate(n, "Text"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        n.set("variables", CoreValues.tree(Map.of("a".repeat(41), "plain")));
        assertThatThrownBy(() -> AgentExperience.validate(n, "Text"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    ObjectNode valid() {
        return (ObjectNode)
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                2,
                                "name",
                                "Guide",
                                "description",
                                "Purpose",
                                "audience",
                                "personal",
                                "language",
                                "vi",
                                "greeting",
                                "Xin chào {{name}}",
                                "timezone",
                                "Asia/Ho_Chi_Minh",
                                "variables",
                                Map.of("name", "Lan Nguyễn 🧪"),
                                "chat",
                                Map.of(
                                        "includeHistory",
                                        true,
                                        "maxTurns",
                                        6,
                                        "showSourceExcerpts",
                                        false,
                                        "responseStyle",
                                        "balanced")));
    }

    @Test
    void unicodeLiteralSubstitutionDoesNotInterpretLegacyOrExpandOtherContent() {
        var n = valid();
        AgentExperience.validate(n, "Help {{name}}.");
        assertThat(AgentExperience.render("Help {{name}}.", n)).isEqualTo("Help Lan Nguyễn 🧪.");
        assertThat(AgentExperience.render("Help {{name}}.", null)).isEqualTo("Help {{name}}.");
        ((ObjectNode) n.path("variables")).put("name", "");
        assertThat(AgentExperience.greeting(n)).isEqualTo("Xin chào ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"audience", "language", "timezone"})
    void rejectsInvalidEnumOrTimezone(String field) {
        var n = valid();
        n.put(field, "unsupported");
        assertThatThrownBy(() -> AgentExperience.validate(n, "Instructions"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{{unknown}}",
                "{{ name }}",
                "{{name}} {{system}}",
                "{{name",
                "name}}",
                "{{{{name}}}}"
            })
    void rejectsUnknownOrMalformedTemplates(String prompt) {
        assertThatThrownBy(() -> AgentExperience.validate(valid(), prompt))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @Test
    void rejectsNestedVariablesAndBoundsChatPairs() {
        var n = valid();
        ((ObjectNode) n.path("variables")).put("name", "{{name}}");
        assertThatThrownBy(() -> AgentExperience.validate(n, "Hi"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        var invalid = valid();
        ((ObjectNode) invalid.path("chat")).put("maxTurns", 7);
        assertThatThrownBy(() -> AgentExperience.validate(invalid, "Hi"))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
    }

    @Test
    void blankGreetingUsesSavedLanguageAndPolicyNeverGrantsAuthority() {
        var n = valid();
        n.put("greeting", "");
        assertThat(AgentExperience.greeting(n)).startsWith("Xin chào");
        n.put("language", "bilingual");
        assertThat(AgentExperience.greeting(n)).contains("Hello", "Xin chào");
        assertThat(AgentExperience.policy(n))
                .contains("never grants data access", "preserve the output character limit");
    }
}
