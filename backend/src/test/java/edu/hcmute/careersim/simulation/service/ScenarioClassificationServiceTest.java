package edu.hcmute.careersim.simulation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.common.storage.FileAssetService;
import edu.hcmute.careersim.enterprise.access.EnterpriseAccess;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.dao.*;
import edu.hcmute.careersim.simulation.domain.*;
import edu.hcmute.careersim.simulation.service.impl.ScenarioAuthoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScenarioClassificationServiceTest {
    private final ScenarioDao dao = mock(ScenarioDao.class);
    private final ScenarioReadDao readDao = mock(ScenarioReadDao.class);
    private final ActorAccess actors = mock(ActorAccess.class);
    private final EnterpriseAccess enterprise = mock(EnterpriseAccess.class);
    private final AuditWriter audit = mock(AuditWriter.class);
    private ScenarioAuthoringServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ScenarioAuthoringServiceImpl(
                        dao,
                        readDao,
                        mock(QuizTaskEvaluator.class),
                        actors,
                        enterprise,
                        mock(DeliverableValidator.class),
                        mock(FileAssetService.class),
                        audit);
        var actor = new ActorAccess.Actor(7, "owner@example.test", "Owner", "ENTERPRISE", 0, 0);
        when(actors.requireId(7)).thenReturn(actor);
        when(actors.lock(7)).thenReturn(actor);
    }

    @Test
    void ownerReadsAndWritesIndependentIndustryWithOptimisticRevision() {
        when(dao.root(11, false))
                .thenReturn(
                        new ScenarioDao.Root(11, 3, "api-triage", "AVAILABLE", null, 4, 22L),
                        new ScenarioDao.Root(11, 3, "api-triage", "AVAILABLE", "IT", 5, 22L));
        when(dao.root(11, true))
                .thenReturn(new ScenarioDao.Root(11, 3, "api-triage", "AVAILABLE", null, 4, 22L));

        var before = service.classification(7, 11);
        var updated = service.classify(7, 11, 4, "IT");

        assertThat(before.industryCode()).isNull();
        assertThat(updated.industryCode()).isEqualTo("IT");
        assertThat(updated.revision()).isEqualTo(5);
        verify(enterprise, times(2)).requireMember(7, 3);
        verify(dao).classification(11, "IT");
        verify(audit)
                .record(
                        eq(7L),
                        eq("SCENARIO_CLASSIFICATION_CHANGED"),
                        eq("scenario"),
                        eq(11L),
                        any());
    }

    @Test
    void staleRevisionAndUnsupportedIndustryNeverWrite() {
        when(dao.root(11, false))
                .thenReturn(new ScenarioDao.Root(11, 3, "api-triage", "AVAILABLE", null, 4, 22L));
        when(dao.root(11, true))
                .thenReturn(new ScenarioDao.Root(11, 3, "api-triage", "AVAILABLE", null, 4, 22L));

        assertThatThrownBy(() -> service.classify(7, 11, 3, "IT")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.classify(7, 11, 4, "FINANCE"))
                .isInstanceOf(ApiException.class);
        verify(dao, never()).classification(anyLong(), any());
    }

    @Test
    void ownerCanClearClassificationToNull() {
        when(dao.root(11, false))
                .thenReturn(
                        new ScenarioDao.Root(11, 3, "api-triage", "AVAILABLE", "IT", 5, 22L),
                        new ScenarioDao.Root(11, 3, "api-triage", "AVAILABLE", null, 6, 22L));
        when(dao.root(11, true))
                .thenReturn(new ScenarioDao.Root(11, 3, "api-triage", "AVAILABLE", "IT", 5, 22L));

        assertThat(service.classify(7, 11, 5, null).industryCode()).isNull();
        verify(dao).classification(11, null);
    }

    @Test
    void wrongTenantCannotReadOrWriteClassification() {
        when(dao.root(11, false))
                .thenReturn(new ScenarioDao.Root(11, 3, "foreign", "AVAILABLE", "IT", 5, 22L));
        doThrow(new ApiException(ErrorCode.FORBIDDEN)).when(enterprise).requireMember(7, 3);

        assertThatThrownBy(() -> service.classification(7, 11)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.classify(7, 11, 5, null)).isInstanceOf(ApiException.class);
        verify(dao, never()).classification(anyLong(), any());
    }

    @Test
    void catalogueIndustryValidationIsSharedByPagedAndUnpagedReads() {
        service.search(
                new edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery(
                        null, null, null, "ID_ASC", null, 20, " "));
        service.list(null, 20, " ");

        var query =
                org.mockito.ArgumentCaptor.forClass(
                        edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery.class);
        verify(readDao).search(query.capture());
        assertThat(query.getValue().industry()).isEmpty();
        verify(dao).catalog(null, 20, "");
        assertThatThrownBy(
                        () ->
                                service.search(
                                        new edu.hcmute.careersim.simulation.dto.ScenarioContracts
                                                .CatalogQuery(
                                                null, null, null, "ID_ASC", null, 20, "FINANCE")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.list(null, 20, "FINANCE"))
                .isInstanceOf(ApiException.class);
    }
}
