package edu.hcmute.careersim.identity.service;

import edu.hcmute.careersim.identity.access.AccountPrincipal;
import java.time.Instant;

/** Browser credential facade. Authority is always re-read from the current account. */
public interface BrowserAuthentication {
    record Credential(String value, Instant expiresAt) {}

    Credential issue(long user);

    AccountPrincipal verify(String value);

    void revoke(String value);

    long version(long user);

    long externalIdentity(
            String issuer,
            String subject,
            String email,
            String name,
            Long linkUser,
            Long linkVersion);
}
