package edu.hcmute.careersim.assessment.service;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;

public interface AssessmentAuthoringService {
    java.util.List<Type> administrationTypes(long actor);

    java.util.List<Version> versions(long actor, long type);

    Version getVersion(long actor, long id);

    String createType(long actor, TypeRequest request);

    Version createVersion(long actor, long type, VersionDraft request);

    Version update(long actor, long id, long revision, VersionDraft request);

    Version publish(long actor, long id, long revision);
}
