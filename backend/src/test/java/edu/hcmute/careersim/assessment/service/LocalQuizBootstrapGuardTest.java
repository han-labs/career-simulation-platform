package edu.hcmute.careersim.assessment.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LocalQuizBootstrapGuardTest {
    @Test
    void acceptsActualCanonicalDatabaseWithoutHardCodedName() {
        LocalQuizBootstrapGuard.require(
                "jdbc:postgresql://127.0.0.1:5433/career_simulation",
                "local-us03",
                "true",
                "career_simulation");
        LocalQuizBootstrapGuard.require(
                "jdbc:postgresql://127.0.0.1:5433/careersim_local",
                "local-us03",
                "true",
                "careersim_local");
    }

    @Test
    void rejectsRemoteAmbiguousOrMismatchedTargetsBeforeConnection() {
        for (String url :
                new String[] {
                    "jdbc:postgresql://production:5432/careersim_local",
                    "jdbc:postgresql://127.0.0.1:5433/other_database",
                    "jdbc:postgresql://127.0.0.1:5433/careersim_local?host=remote",
                    "jdbc:postgresql://127.0.0.1:5433/careersim_local/extra",
                    "jdbc:postgresql://127.0.0.1:0/careersim_local",
                    "jdbc:postgresql://127.0.0.1:65536/careersim_local",
                    "jdbc:postgresql://127.0.0.1:5433,remote:5432/careersim_local"
                }) {
            assertThatThrownBy(
                            () ->
                                    LocalQuizBootstrapGuard.require(
                                            url, "local-us03", "true", "careersim_local"))
                    .isInstanceOf(IllegalStateException.class);
        }
        assertThatThrownBy(
                        () ->
                                LocalQuizBootstrapGuard.require(
                                        null, "local-us03", "true", "careersim_local"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                LocalQuizBootstrapGuard.require(
                                        "jdbc:postgresql://127.0.0.1:5433/x",
                                        "local-us03",
                                        "true",
                                        "x?host=remote"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiresExplicitLocalProfileAndOptIn() {
        String url = "jdbc:postgresql://127.0.0.1:5433/careersim_local";
        for (String profile :
                new String[] {
                    "production", "prod", "default", "auth-test", "local", "local-us03,prod"
                })
            assertThatThrownBy(
                            () ->
                                    LocalQuizBootstrapGuard.require(
                                            url, profile, "true", "careersim_local"))
                    .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                LocalQuizBootstrapGuard.require(
                                        url, "local-us03", "false", "careersim_local"))
                .isInstanceOf(IllegalStateException.class);
    }
}
