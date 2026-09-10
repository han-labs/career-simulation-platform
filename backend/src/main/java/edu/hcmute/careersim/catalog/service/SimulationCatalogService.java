package edu.hcmute.careersim.catalog.service;

import edu.hcmute.careersim.catalog.dto.SimulationSummaryResponse;
import java.util.List;

public interface SimulationCatalogService {

    List<SimulationSummaryResponse> getPublishedSimulations();

    SimulationSummaryResponse getPublishedSimulation(String slug);
}
