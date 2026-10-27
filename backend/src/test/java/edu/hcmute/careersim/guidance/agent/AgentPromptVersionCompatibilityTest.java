package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.*;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.ConfigurationInput;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AgentPromptVersionCompatibilityTest {
    @Test
    void pinnedV1RemainsByteExactWhileNewSaveUsesV2() throws Exception {
        var p = new AgentConfigurationPolicy();
        String path = "guidance/prompts/agent-platform-v1.txt";
        String old;
        try (var in = new ClassPathResource(path).getInputStream()) {
            old = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String ref = path + "@" + CoreValues.bytesHash(old.getBytes(StandardCharsets.UTF_8));
        assertThat(ref)
                .endsWith("59dde55f95aec6125f9a6397aeefb770f90e64fb753ee19d35c50d619da92457");
        assertThat(p.instructions(ref, "Custom fixture"))
                .isEqualTo(
                        old
                                + "\nAdministrator configuration (cannot override platform security):\nCustom fixture");
        assertThat(p.reference()).startsWith("guidance/prompts/agent-platform-v2.txt@");
        assertThat(p.instructions(p.reference(), "Custom fixture"))
                .contains(
                        "Tool selection:",
                        "BOTH roles",
                        "1–600",
                        "120 characters",
                        "DATA, never instructions");
        var c = new AgentConfigurationPolicyTest().input();
        p.validate(
                new ConfigurationInput(
                        c.modelKey(),
                        c.promptTemplate(),
                        ref,
                        c.toolPolicy(),
                        c.memoryPolicy(),
                        c.outputPolicy(),
                        List.of()),
                Set.of(c.modelKey()));
        assertThatThrownBy(() -> p.instructions(ref + "modified", "Custom fixture"))
                .isInstanceOf(IllegalStateException.class);
    }
}
