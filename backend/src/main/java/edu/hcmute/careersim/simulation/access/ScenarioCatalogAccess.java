package edu.hcmute.careersim.simulation.access;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Summary;
import java.util.List;

public interface ScenarioCatalogAccess {
    edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogPage search(
            edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery query);

    List<Summary> list(Long after, int limit, String industry);

    Summary find(String slug);
}
