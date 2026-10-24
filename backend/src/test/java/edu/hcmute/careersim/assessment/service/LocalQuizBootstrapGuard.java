package edu.hcmute.careersim.assessment.service;

/** Test-source only; validate before Spring opens a database connection. */
final class LocalQuizBootstrapGuard {
    static void require(String url, String profile, String enabled) {
        if (!"local-us03".equals(profile)
                || !"true".equals(enabled)
                || url == null
                || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]{1,5}/career_simulation"))
            throw new IllegalStateException(
                    "US-03 bootstrap requires explicit local-us03 and loopback career_simulation database.");
    }
}
