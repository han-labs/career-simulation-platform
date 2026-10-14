package edu.hcmute.careersim.simulation.access;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.List;

public interface SharedSimulationAccess {
    List<Completion> readCompletions(long actor, long student);

    Submission readSubmission(long actor, long submission);
}
