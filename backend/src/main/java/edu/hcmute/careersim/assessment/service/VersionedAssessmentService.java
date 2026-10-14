package edu.hcmute.careersim.assessment.service;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import java.util.*;

public interface VersionedAssessmentService {
    List<Type> catalog();

    Attempt start(long actor, long version, UUID request);

    Attempt get(long actor, long id);

    Attempt save(long actor, long id, long revision, Responses request);

    Result submit(long actor, long id, long revision, Responses request);

    Result result(long actor, long id);
}
