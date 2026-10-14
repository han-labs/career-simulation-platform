package edu.hcmute.careersim.enterprise.service;

import edu.hcmute.careersim.enterprise.access.EnterpriseAccess.Membership;

public interface EnterpriseService {
    Membership mine(long actor);

    void verification(long actor, long id, long revision, String status);

    void publishing(long actor, long id, long revision, String status);

    void member(long actor, long id, long user, long revision, String status, boolean permission);
}
