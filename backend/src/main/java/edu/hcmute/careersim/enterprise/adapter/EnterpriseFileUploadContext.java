package edu.hcmute.careersim.enterprise.adapter;

import edu.hcmute.careersim.common.storage.FileUploadContext;
import edu.hcmute.careersim.enterprise.access.EnterpriseAccess;
import edu.hcmute.careersim.identity.access.ActorAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnterpriseFileUploadContext implements FileUploadContext {
    private final ActorAccess actors;
    private final EnterpriseAccess enterprise;

    public Long enterpriseFor(long actor) {
        var a = actors.requireId(actor);
        if ("ENTERPRISE".equals(a.role())) {
            var m = enterprise.mine(actor);
            enterprise.requireMember(actor, Long.parseLong(m.enterpriseId()));
            return Long.parseLong(m.enterpriseId());
        }
        return null;
    }
}
