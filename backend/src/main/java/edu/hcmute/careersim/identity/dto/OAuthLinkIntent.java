package edu.hcmute.careersim.identity.dto;

import java.io.Serializable;
import java.time.Instant;

/** One-shot intent bound to the authenticated browser credential, never to an email. */
public record OAuthLinkIntent(long user, long version, String credentialHash, Instant expiresAt)
        implements Serializable {
    public static final String KEY = "careersim.oauth.link";
}
