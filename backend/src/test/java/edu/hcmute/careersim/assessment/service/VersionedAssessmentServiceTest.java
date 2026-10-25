package edu.hcmute.careersim.assessment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.assessment.dao.VersionedAssessmentDao;
import edu.hcmute.careersim.assessment.domain.*;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.assessment.service.impl.VersionedAssessmentServiceImpl;
import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionedAssessmentServiceTest {
    final VersionedAssessmentDao dao = mock(VersionedAssessmentDao.class);
    final ActorAccess actors = mock(ActorAccess.class);
    final VersionedAssessmentService service =
            new VersionedAssessmentServiceImpl(
                    dao,
                    new AssessmentEvaluatorRegistry(new RiasecV1Evaluator()),
                    actors,
                    mock(AuditWriter.class));
    final Version version =
            new Version("11", "1", 1, "PUBLISHED", "riasec-sum", "1", null, "hash", 1, List.of());
    final VersionedAssessmentDao.AttemptRow attempt =
            new VersionedAssessmentDao.AttemptRow(
                    2, 7, 11, "IN_PROGRESS", 0, null, UUID.randomUUID());

    @BeforeEach
    void actor() {
        var student = new ActorAccess.Actor(7, "student@example.test", "Student", "STUDENT", 0, 0);
        when(actors.lock(7)).thenReturn(student);
        when(actors.requireId(7)).thenReturn(student);
        when(dao.owned(7, 2, true)).thenReturn(attempt);
        when(dao.version(11, false)).thenReturn(version);
        when(dao.questions(11)).thenReturn(List.of());
    }

    @Test
    void historyUsesOnlyCurrentStudentAndRejectsWrongRole() {
        when(dao.history(7)).thenReturn(List.of());
        assertThat(service.history(7)).isEmpty();
        verify(dao).history(7);
        when(actors.requireId(8))
                .thenReturn(new ActorAccess.Actor(8, "admin@example.test", "Admin", "ADMIN", 0, 0));
        assertThatThrownBy(() -> service.history(8)).isInstanceOf(ApiException.class);
        verify(dao, never()).history(8);
    }

    @Test
    void inactiveTypeBlocksStartDraftAndSubmitWithoutWritingResult() {
        when(dao.typeActive(1)).thenReturn(false);
        assertThatThrownBy(() -> service.start(7, 11, UUID.randomUUID()))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.save(7, 2, 0, new Responses(List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.submit(7, 2, 0, new Responses(List.of())))
                .isInstanceOf(ApiException.class);
        verify(dao, never()).responses(any(), any());
        verify(dao, never()).result(any(), any(), any(), any());
    }

    @Test
    void draftVersionCannotStartEvenWhenTypeIsActive() {
        when(dao.version(11, false))
                .thenReturn(
                        new Version(
                                "11",
                                "1",
                                2,
                                "DRAFT",
                                "riasec-sum",
                                "1",
                                null,
                                null,
                                0,
                                List.of()));
        assertThatThrownBy(() -> service.start(7, 11, UUID.randomUUID()))
                .isInstanceOf(ApiException.class);
        verify(dao, never()).start(anyLong(), anyLong(), any());
    }

    @Test
    void submittedRetryReturnsHistoricalResultEvenIfTypeIsNowInactive() {
        when(dao.owned(7, 2, true))
                .thenReturn(
                        new VersionedAssessmentDao.AttemptRow(
                                2,
                                7,
                                11,
                                "SUBMITTED",
                                2,
                                CoreValues.hash(List.of()),
                                attempt.startRequest()));
        var result =
                new Result("2", "RIASEC", "11", "riasec-sum", "1", CoreValues.tree(Map.of()), null);
        when(dao.result(7, 2)).thenReturn(result);
        assertThat(service.submit(7, 2, 0, new Responses(List.of()))).isEqualTo(result);
        verify(dao, never()).typeActive(anyLong());
        verify(dao, never()).responses(any(), any());
    }
}
