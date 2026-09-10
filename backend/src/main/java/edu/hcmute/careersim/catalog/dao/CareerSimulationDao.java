package edu.hcmute.careersim.catalog.dao;

import edu.hcmute.careersim.catalog.entity.CareerSimulation;
import edu.hcmute.careersim.catalog.enumeration.SimulationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerSimulationDao extends JpaRepository<CareerSimulation, Long> {

    List<CareerSimulation> findByStatusOrderByTitleAsc(SimulationStatus status);

    Optional<CareerSimulation> findBySlugAndStatus(String slug, SimulationStatus status);
}
