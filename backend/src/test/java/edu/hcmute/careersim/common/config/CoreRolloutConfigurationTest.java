package edu.hcmute.careersim.common.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CoreRolloutConfigurationTest {
    @Test
    void allFourFlagCombinationsFailClosedOnlyOnConflictingWriters() {
        for (boolean core : new boolean[] {false, true})
            for (boolean legacy : new boolean[] {false, true}) {
                new ApplicationContextRunner()
                        .withUserConfiguration(CoreRolloutConfiguration.class)
                        .withPropertyValues(
                                "app.core-v2-enabled=" + core, "app.legacy-write-enabled=" + legacy)
                        .run(
                                context -> {
                                    if (core && legacy) assertThat(context).hasFailed();
                                    else assertThat(context).hasNotFailed();
                                });
            }
    }
}
