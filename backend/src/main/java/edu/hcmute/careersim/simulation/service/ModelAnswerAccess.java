package edu.hcmute.careersim.simulation.service;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.List;

public interface ModelAnswerAccess {
    Released readAnswer(long actor, long enrollment, long task);

    record Released(Answer answer, List<Resource> resources) {}
}
