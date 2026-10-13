package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.dto.PlanResponse;
import edu.hcmute.careersim.guidance.dto.SavePlanRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import java.util.UUID;

public interface GuidanceService {

    DashboardResponse getDashboard(String authenticatedEmail);

    SynMessageResponse sendMessage(String authenticatedEmail, SynMessageRequest request);

    boolean clearSynSession(String authenticatedEmail, UUID sessionId);

    edu.hcmute.careersim.guidance.dto.SynTurnResponse getTurn(String email, UUID id);

    edu.hcmute.careersim.guidance.dto.SynMessageResponse.EvidenceReference getKnowledgeSource(
            String email, long id);

    edu.hcmute.careersim.guidance.dto.SynTurnResponse getRequestTurn(String email, UUID requestId);

    void cancelTurn(String email, UUID id);

    PlanResponse savePlan(String authenticatedEmail, SavePlanRequest request);
}
