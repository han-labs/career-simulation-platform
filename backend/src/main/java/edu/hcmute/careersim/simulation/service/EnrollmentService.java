package edu.hcmute.careersim.simulation.service;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Enrollment;
import java.util.List;

public interface EnrollmentService {
    Enrollment enroll(long actor, long version);

    Enrollment read(long actor, long id);

    edu.hcmute.careersim.simulation.dto.ScenarioContracts.Results results(long actor, long id);

    List<Enrollment> list(long actor, long after, int limit);
}
