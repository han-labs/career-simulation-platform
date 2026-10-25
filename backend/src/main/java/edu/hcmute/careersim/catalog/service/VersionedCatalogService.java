package edu.hcmute.careersim.catalog.service;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Summary;
import java.util.List;

public interface VersionedCatalogService {
    edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogPage search(
            edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery query);

    List<Summary> list(Long after, int limit, String industry);

    Summary find(String slug);
}
