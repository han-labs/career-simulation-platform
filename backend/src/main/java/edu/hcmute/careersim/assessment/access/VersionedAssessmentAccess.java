package edu.hcmute.careersim.assessment.access;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.Result;
import java.util.List;

public interface VersionedAssessmentAccess {
    List<Result> readOwned(long actor);

    List<Result> readOwned(long actor, String assessmentType);

    edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.OwnActivity activity(
            long actor, String assessmentType);
}
