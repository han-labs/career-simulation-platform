package edu.hcmute.careersim.guidance.agent.service;

import edu.hcmute.careersim.guidance.agent.dto.AdminPreviewContracts.*;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.Execution;
import java.util.UUID;

public interface AdminPreviewConversationService {
    Page list(long owner, UUID agent, int limit, int offset);

    Conversation create(long owner, UUID agent, NewChat input);

    Transcript read(long owner, UUID agent, UUID chat, long before, int limit);

    Execution send(long owner, UUID agent, UUID chat, Message input);

    Conversation change(
            long owner, UUID agent, UUID chat, long revision, boolean confirmed, boolean delete);
}
