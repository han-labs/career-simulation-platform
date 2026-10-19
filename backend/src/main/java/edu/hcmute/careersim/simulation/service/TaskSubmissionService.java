package edu.hcmute.careersim.simulation.service;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;

public interface TaskSubmissionService {
    Submission selfReview(long actor, long enrollment, long task);

    Submission read(long actor, long enrollment, long task);

    Submission save(long actor, long enrollment, long task, long revision, Items request);

    Submission finalizeTask(
            long actor, long enrollment, long task, long revision, Finalize request);
}
