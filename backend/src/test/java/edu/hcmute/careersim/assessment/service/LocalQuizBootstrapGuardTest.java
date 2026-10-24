package edu.hcmute.careersim.assessment.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LocalQuizBootstrapGuardTest {
    @Test
    void onlyExplicitLocalDatabaseAndProfileAreAllowed() {
        LocalQuizBootstrapGuard.require(
                "jdbc:postgresql://127.0.0.1:5433/career_simulation", "local-us03", "true");
        for (String url :
                new String[] {
                    "jdbc:postgresql://production:5432/career_simulation",
                    "jdbc:postgresql://127.0.0.1:5433/production",
                    "jdbc:postgresql://127.0.0.1:5433/career_simulation?host=remote"
                })
            assertThatThrownBy(() -> LocalQuizBootstrapGuard.require(url, "local-us03", "true"))
                    .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                LocalQuizBootstrapGuard.require(
                                        "jdbc:postgresql://127.0.0.1:5433/career_simulation",
                                        "production",
                                        "true"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                LocalQuizBootstrapGuard.require(
                                        "jdbc:postgresql://127.0.0.1:5433/career_simulation",
                                        "local-us03",
                                        "false"))
                .isInstanceOf(IllegalStateException.class);
    }
}
