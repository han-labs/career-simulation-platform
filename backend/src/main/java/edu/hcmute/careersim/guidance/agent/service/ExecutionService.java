package edu.hcmute.careersim.guidance.agent.service;

import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Test;
import java.util.*;

public interface ExecutionService {
    Map<String, Object> options(long admin);

    Execution preview(long admin, UUID agent, UUID configuration, Preview request);

    Execution previewConversation(
            long admin,
            UUID agent,
            UUID configuration,
            Preview request,
            edu.hcmute.careersim.guidance.agent.dto.AdminPreviewContracts.Scope scope);

    Execution test(long admin, UUID kb, Test request);

    Execution read(long admin, UUID execution);

    Execution cancel(long admin, UUID execution);
}
