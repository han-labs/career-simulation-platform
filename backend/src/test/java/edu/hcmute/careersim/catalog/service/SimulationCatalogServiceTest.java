package edu.hcmute.careersim.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.hcmute.careersim.catalog.dao.CareerSimulationDao;
import edu.hcmute.careersim.catalog.entity.CareerSimulation;
import edu.hcmute.careersim.catalog.enumeration.DifficultyLevel;
import edu.hcmute.careersim.catalog.enumeration.SimulationStatus;
import edu.hcmute.careersim.catalog.mapper.CareerSimulationMapper;
import edu.hcmute.careersim.catalog.service.impl.SimulationCatalogServiceImpl;
import edu.hcmute.careersim.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimulationCatalogServiceTest {

    private CareerSimulationDao simulationDao;
    private SimulationCatalogService service;

    @BeforeEach
    void setUp() {
        simulationDao = mock(CareerSimulationDao.class);
        service = new SimulationCatalogServiceImpl(simulationDao, new CareerSimulationMapper());
    }

    @Test
    void getPublishedSimulationsMapsOnlyDaoResults() {
        CareerSimulation simulation = simulation(7L, "backend-api-triage");
        when(simulationDao.findByStatusOrderByTitleAsc(SimulationStatus.PUBLISHED))
                .thenReturn(List.of(simulation));

        var result = service.getPublishedSimulations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(7L);
        assertThat(result.get(0).slug()).isEqualTo("backend-api-triage");
        assertThat(result.get(0).difficulty()).isEqualTo("INTRODUCTORY");
    }

    @Test
    void getPublishedSimulationRejectsUnknownSlug() {
        when(simulationDao.findBySlugAndStatus("missing", SimulationStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublishedSimulation("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Published simulation was not found.");
    }

    private CareerSimulation simulation(Long id, String slug) {
        CareerSimulation simulation = mock(CareerSimulation.class);
        when(simulation.getId()).thenReturn(id);
        when(simulation.getSlug()).thenReturn(slug);
        when(simulation.getTitle()).thenReturn("Backend API Triage");
        when(simulation.getCareerTrack()).thenReturn("BACKEND_DEVELOPMENT");
        when(simulation.getSummary()).thenReturn("Practice evidence-based backend decisions.");
        when(simulation.getDifficulty()).thenReturn(DifficultyLevel.INTRODUCTORY);
        when(simulation.getEstimatedMinutes()).thenReturn(35);
        return simulation;
    }
}
