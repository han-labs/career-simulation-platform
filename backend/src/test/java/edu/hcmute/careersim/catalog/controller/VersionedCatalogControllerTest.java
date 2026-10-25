package edu.hcmute.careersim.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.catalog.service.VersionedCatalogService;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogPage;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Metadata;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.PublicMedia;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Summary;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.TaskOutline;
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

    @Test
    void publicDetailMapsMediaAndWorkplaceDescriptionWithoutLockedFields() throws Exception {
        var detail =
                new Summary(
                        "1",
                        "api-triage",
                        "2",
                        "3",
                        "CareerSim Studio",
                        "IT",
                        new Metadata(
                                "API Triage",
                                "Developer",
                                "Backend",
                                "Summary",
                                "Introduction",
                                "Observe the incident",
                                "INTRODUCTORY",
                                30,
                                new ObjectMapper().createObjectNode(),
                                new ObjectMapper().createObjectNode()),
                        new PublicMedia(
                                "https://cdn.theforage.com/card.png",
                                "https://cdn.theforage.com/hero.png",
                                "https://cdn.theforage.com/logo.png",
                                "https://fast.wistia.net/embed/iframe/abc123"),
                        List.of(
                                new TaskOutline(
                                        "4",
                                        1,
                                        "Inspect the incident",
                                        "Review the public workplace context.",
                                        true,
                                        "PRACTICAL")),
                        List.of("API debugging", "Incident triage"));

        String json = new ObjectMapper().writeValueAsString(detail);

        assertThat(json)
                .contains(
                        "cardImageUrl",
                        "overviewVideoUrl",
                        "workplace context",
                        "API debugging",
                        "Incident triage");
        assertThat(json)
                .doesNotContain(
                        "instructions", "resources", "deliverables", "modelAnswer", "rubric");
    }
}
