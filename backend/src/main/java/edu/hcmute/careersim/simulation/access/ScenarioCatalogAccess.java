package edu.hcmute.careersim.simulation.access;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Summary;
import java.util.List;

public interface ScenarioCatalogAccess {
    List<Summary> list(Long after, int limit);

    Summary find(String slug);
}
