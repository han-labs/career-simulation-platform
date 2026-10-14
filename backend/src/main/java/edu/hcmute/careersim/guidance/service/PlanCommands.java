package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dto.PlanContracts.*;

public interface PlanCommands {
    Plan importConfirmed(
            long reviewer,
            long student,
            String title,
            java.util.List<String> steps,
            java.time.Instant confirmedAt);

    Plan confirm(long actor, Confirm request);
}
