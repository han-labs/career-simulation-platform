package edu.hcmute.careersim.guidance.knowledge.service.impl;

import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.storage.FileUseAuthorizer;
import edu.hcmute.careersim.guidance.knowledge.dao.KnowledgeStudioDao;
import edu.hcmute.careersim.identity.access.ActorAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class KnowledgeSourceAuthorization implements FileUseAuthorizer {
    private final ActorAccess actors;
    private final KnowledgeStudioDao dao;

    public boolean supports(String type) {
        return "KNOWLEDGE_SOURCE".equals(type);
    }

    public void authorize(long actor, long file, String type, long useId) {
        ActorAccess.requireRole(actors.requireId(actor), "ADMIN");
        if (!supports(type) || file != useId || !dao.linkedSource(file))
            throw new NotFoundException("Knowledge source unavailable.");
    }
}
