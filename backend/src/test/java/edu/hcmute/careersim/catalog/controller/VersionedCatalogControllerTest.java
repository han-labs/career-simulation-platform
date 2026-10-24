package edu.hcmute.careersim.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.catalog.service.VersionedCatalogService;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogPage;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class VersionedCatalogControllerTest {
    @Test
    void optionalIndustryIsForwardedWithoutChangingAbsentFilter() {
        var service = mock(VersionedCatalogService.class);
        when(service.search(any())).thenReturn(new CatalogPage(List.of(), null));
        var controller = new VersionedCatalogController(service);

        controller.search("api", "BACKEND", "INTRODUCTORY", "IT", "ID_ASC", null, 20);
        controller.search("api", "BACKEND", "INTRODUCTORY", null, "ID_ASC", null, 20);

        var query = org.mockito.ArgumentCaptor.forClass(CatalogQuery.class);
        verify(service, times(2)).search(query.capture());
        assertThat(query.getAllValues().get(0).industry()).isEqualTo("IT");
        assertThat(query.getAllValues().get(1).industry()).isNull();
    }

    @Test
    void unpagedCatalogueForwardsIndustryAndPreservesAbsentBehavior() {
        var service = mock(VersionedCatalogService.class);
        when(service.list(any(), anyInt(), any())).thenReturn(List.of());
        var controller = new VersionedCatalogController(service);

        controller.list(null, "IT", 20);
        controller.list(null, null, 20);

        verify(service).list(null, 20, "IT");
        verify(service).list(null, 20, null);
    }
}
