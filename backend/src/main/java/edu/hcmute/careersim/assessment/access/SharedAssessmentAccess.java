package edu.hcmute.careersim.assessment.access;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.Result;
import java.util.List;

public interface SharedAssessmentAccess {
    List<Result> readShared(long actor, long student);
}
