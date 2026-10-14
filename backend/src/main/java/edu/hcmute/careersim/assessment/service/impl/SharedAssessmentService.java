package edu.hcmute.careersim.assessment.service.impl;

import edu.hcmute.careersim.assessment.access.SharedAssessmentAccess;
import edu.hcmute.careersim.assessment.dao.VersionedAssessmentDao;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.Result;
import edu.hcmute.careersim.identity.access.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SharedAssessmentService implements SharedAssessmentAccess {
    private final SharingRecipientAccess recipient;
    private final ActorAccess actors;
    private final ConsentAccess consent;
    private final VersionedAssessmentDao dao;

    @Transactional(readOnly = true)
    public List<Result> readShared(long actor, long student) {
        long tenant = recipient.requireEnterprise(actor);
        ActorAccess.requireRole(actors.requireId(student), "STUDENT");
        consent.require(student, "ASSESSMENT_SHARE", "enterprise:" + tenant);
        return dao.results(student);
    }
}
