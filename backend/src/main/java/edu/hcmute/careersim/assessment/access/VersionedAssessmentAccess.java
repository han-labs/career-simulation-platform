package edu.hcmute.careersim.assessment.access;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.Result;
import java.util.List;

public interface VersionedAssessmentAccess {
    List<Result> readOwned(long actor);
}
