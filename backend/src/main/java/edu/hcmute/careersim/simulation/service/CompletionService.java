package edu.hcmute.careersim.simulation.service;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Completion;

public interface CompletionService {
    void completeIfEligible(long actor, long enrollment);

    Completion read(long actor, long id);

    String renderCertificate(long actor, long id);
}
