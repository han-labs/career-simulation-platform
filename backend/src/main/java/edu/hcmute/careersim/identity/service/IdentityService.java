package edu.hcmute.careersim.identity.service;

import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.dto.IdentityContracts.*;

public interface IdentityService {
    Account register(Registration request);

    AccountPrincipal authenticate(Login request);

    Account me(long actor);

    Account profile(long actor, long revision, Profile request);

    void status(long actor, long target, long revision, String status);
}
