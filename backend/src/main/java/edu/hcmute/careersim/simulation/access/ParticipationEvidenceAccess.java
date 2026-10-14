package edu.hcmute.careersim.simulation.access;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.List;

public interface ParticipationEvidenceAccess {
    boolean ownsFinal(long actor, String submission, String version, String skillCode);

    List<Participation> readOwned(long actor, int limit);

    List<Completion> completions(long actor);
}
