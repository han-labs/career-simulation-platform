package edu.hcmute.careersim.assessment.service;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;

public interface AssessmentAuthoringService {
    Version getVersion(long actor, long id);

    String createType(long actor, TypeRequest request);

    Version createVersion(long actor, long type, VersionDraft request);

    Version update(long actor, long id, long revision, VersionDraft request);

    Version publish(long actor, long id, long revision);
}
