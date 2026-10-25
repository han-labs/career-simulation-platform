package edu.hcmute.careersim.assessment.service;

/** Test-source only; validate before Spring opens a database connection. */
final class LocalQuizBootstrapGuard {
    static void require(String url, String profile, String enabled, String database) {
        if (!"local-us03".equals(profile)
                || !"true".equals(enabled)
                || url == null
                || database == null
                || !database.matches("[A-Za-z_][A-Za-z0-9_]{0,62}")
                || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]{1,5}/" + database))
            throw new IllegalStateException(
                    "Assessment bootstrap requires explicit local-us03 opt-in and the canonical loopback database.");
        int port = Integer.parseInt(url.substring(url.lastIndexOf(':') + 1, url.lastIndexOf('/')));
        if (port < 1 || port > 65535)
            throw new IllegalStateException("Invalid local database port.");
    }
}
