package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dto.PlanContracts.Plan;
import java.util.*;

public interface ExplorationPlanService {
    List<Plan> list(long actor);

    Optional<Plan> current(long actor);
}
