package edu.hcmute.careersim.enterprise.adapter;

import edu.hcmute.careersim.enterprise.access.EnterpriseAccess;
import edu.hcmute.careersim.identity.access.SharingRecipientAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnterpriseSharingRecipient implements SharingRecipientAccess {
    private final EnterpriseAccess enterprise;

    public long requireEnterprise(long actor) {
        long id = Long.parseLong(enterprise.mine(actor).enterpriseId());
        enterprise.requireMember(actor, id);
        return id;
    }
}
