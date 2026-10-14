package edu.hcmute.careersim.identity.access;

/** In-transaction runtime adapter; identity never imports guidance/agent persistence. */
public interface ConsentRevocationListener {
    void revoked(long owner, String purpose, String recipient);
}
