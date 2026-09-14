// Defines the use case for submitting and evaluating a simulation attempt.
package edu.hcmute.careersim.simulation.service;

import edu.hcmute.careersim.simulation.dto.SubmitAttemptRequest;
import edu.hcmute.careersim.simulation.dto.SubmitAttemptResponse;

public interface SimulationAttemptService {

    SubmitAttemptResponse submitAttempt(
            String slug, Long studentId, SubmitAttemptRequest request);
}
