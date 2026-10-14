package edu.hcmute.careersim.identity.access;

import java.time.Instant;
import java.util.Optional;

public interface StudentAccountAccess {

    Optional<StudentAccount> findByEmail(String email);

    record StudentAccount(
            long id,
            String displayName,
            String role,
            String status,
            Instant consentedToAiAt,
            long privacyVersion) {
        public StudentAccount(
                long id, String displayName, String role, String status, Instant consentedToAiAt) {
            this(id, displayName, role, status, consentedToAiAt, 0);
        }
    }
}
