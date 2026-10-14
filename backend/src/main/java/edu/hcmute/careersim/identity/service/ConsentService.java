package edu.hcmute.careersim.identity.service;

import edu.hcmute.careersim.identity.dto.IdentityContracts.Grant;
import java.util.*;

public interface ConsentService {
    List<Map<String, Object>> list(long owner);

    String grant(long owner, Grant request);

    void revoke(long owner, long id);
}
