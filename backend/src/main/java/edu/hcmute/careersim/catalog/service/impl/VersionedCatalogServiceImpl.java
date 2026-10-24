package edu.hcmute.careersim.catalog.service.impl;

import edu.hcmute.careersim.catalog.service.VersionedCatalogService;
import edu.hcmute.careersim.simulation.access.ScenarioCatalogAccess;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.Summary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VersionedCatalogServiceImpl implements VersionedCatalogService {
    private final ScenarioCatalogAccess scenarios;

    public edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogPage search(
            edu.hcmute.careersim.simulation.dto.ScenarioContracts.CatalogQuery query) {
        return scenarios.search(query);
    }

    public List<Summary> list(Long after, int limit, String industry) {
        return scenarios.list(after, limit, industry);
    }

    public Summary find(String slug) {
        return scenarios.find(slug);
    }
}
