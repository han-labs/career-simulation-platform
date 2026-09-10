package edu.hcmute.careersim.catalog.service.impl;

import edu.hcmute.careersim.catalog.dao.CareerSimulationDao;
import edu.hcmute.careersim.catalog.dto.SimulationSummaryResponse;
import edu.hcmute.careersim.catalog.enumeration.SimulationStatus;
import edu.hcmute.careersim.catalog.mapper.CareerSimulationMapper;
import edu.hcmute.careersim.catalog.service.SimulationCatalogService;
import edu.hcmute.careersim.common.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimulationCatalogServiceImpl implements SimulationCatalogService {

    private final CareerSimulationDao simulationDao;
    private final CareerSimulationMapper simulationMapper;

    @Override
    public List<SimulationSummaryResponse> getPublishedSimulations() {
        return simulationDao.findByStatusOrderByTitleAsc(SimulationStatus.PUBLISHED).stream()
                .map(simulationMapper::toSummary)
                .toList();
    }

    @Override
    public SimulationSummaryResponse getPublishedSimulation(String slug) {
        return simulationDao
                .findBySlugAndStatus(slug, SimulationStatus.PUBLISHED)
                .map(simulationMapper::toSummary)
                .orElseThrow(() -> new NotFoundException("Published simulation was not found."));
    }
}
