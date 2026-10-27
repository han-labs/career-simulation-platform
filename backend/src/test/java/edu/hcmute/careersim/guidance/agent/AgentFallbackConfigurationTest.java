package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.AgentConfigurationPolicy;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.ConfigurationInput;
import java.util.*;
import org.junit.jupiter.api.Test;

class AgentFallbackConfigurationTest {
    final AgentConfigurationPolicy policy = new AgentConfigurationPolicy();

    ConfigurationInput input(Object fallbacks, boolean include) {
        var old = new AgentConfigurationPolicyTest().input();
        var experience = new AgentExperienceTest().valid();
        if (include) experience.set("fallbackModelKeys", CoreValues.tree(fallbacks));
        return new ConfigurationInput(
                "gemini/primary",
                old.promptTemplate(),
                null,
                old.toolPolicy(),
                old.memoryPolicy(),
                old.outputPolicy(),
                List.of(),
                experience);
    }

    @Test
    void fallbacksRoundTripAreOrderedHashedAndDoNotMutateOldVersion() {
        var old = input(null, false);
        String hash = policy.hash(old);
        var copy = old.experienceConfig().deepCopy();
        var next = input(List.of("gemini/secondary", "gemini/third"), true);
        policy.validate(old, Set.of("gemini/primary"));
        policy.validate(next, Set.of("gemini/primary", "gemini/secondary", "gemini/third"));
        assertThat(
                        CoreValues.json(CoreValues.write(next))
                                .path("experienceConfig")
                                .path("fallbackModelKeys")
                                .get(0)
                                .asText())
                .isEqualTo("gemini/secondary");
        assertThat(policy.hash(next)).isNotEqualTo(hash);
        assertThat(policy.hash(input(List.of("gemini/third", "gemini/secondary"), true)))
                .isNotEqualTo(policy.hash(next));
        assertThat(old.experienceConfig()).isEqualTo(copy);
        assertThat(policy.hash(old)).isEqualTo(hash);
        var legacy = new AgentConfigurationPolicyTest().input();
        assertThat(policy.hash(legacy)).isEqualTo(new AgentConfigurationPolicy().hash(legacy));
    }

    @Test
    void rejectsDuplicatesPrimaryUnsupportedCrossProviderTooManyAndWrongType() {
        for (Object value :
                List.of(
                        List.of("gemini/primary"),
                        List.of("gemini/secondary", "gemini/secondary"),
                        List.of("gemini/missing"),
                        List.of("openai/secondary"),
                        List.of("gemini/a", "gemini/b", "gemini/c", "gemini/d"),
                        "not-an-array"))
            assertThatThrownBy(
                            () ->
                                    policy.validate(
                                            input(value, true),
                                            Set.of(
                                                    "gemini/primary",
                                                    "gemini/secondary",
                                                    "openai/secondary",
                                                    "gemini/a",
                                                    "gemini/b",
                                                    "gemini/c",
                                                    "gemini/d")))
                    .isInstanceOf(RuntimeException.class);
    }
}
