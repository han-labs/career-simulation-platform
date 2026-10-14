package edu.hcmute.careersim.identity.access;

import java.io.Serializable;
import java.security.Principal;

public record AccountPrincipal(long id, String email) implements Principal, Serializable {
    @Override
    public String getName() {
        return email;
    }
}
