package edu.hcmute.careersim.simulation.service;

import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.enterprise.access.EnterpriseAccess;
import edu.hcmute.careersim.simulation.dao.ScenarioAnalyticsDao;
import edu.hcmute.careersim.simulation.domain.AnalyticsFilter;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioAnalyticsService {
    private static final int MIN_PARTICIPANTS = 5;
    private final EnterpriseAccess enterprises;
    private final ScenarioAnalyticsDao dao;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Analytics analytics(long actor, long enterprise, AnalyticsFilter filter) {
        var programs = authorize(actor, enterprise, filter);
        var counts = dao.counts(enterprise, filter);
        if (counts.enrollments() == 0)
            return new Analytics(programs, counts, AnalyticsDetail.EMPTY, List.of());
        if (counts.participants() < MIN_PARTICIPANTS)
            return new Analytics(programs, counts, AnalyticsDetail.INSUFFICIENT, List.of());
        return new Analytics(
                programs, counts, AnalyticsDetail.AVAILABLE, dao.tasks(enterprise, filter));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AnalyticsEvidencePage evidence(
            long actor, long enterprise, AnalyticsFilter filter, String before, int limit) {
        authorize(actor, enterprise, filter);
        if (limit < 1 || limit > 50)
            CoreValues.invalid("Evidence page size must be between 1 and 50.");
        var items =
                dao.evidence(
                        enterprise,
                        filter,
                        before == null ? null : CoreValues.id(before),
                        limit + 1);
        if (items.size() <= limit) return new AnalyticsEvidencePage(items, null);
        return new AnalyticsEvidencePage(
                List.copyOf(items.subList(0, limit)), items.get(limit - 1).reference());
    }

    private List<AnalyticsProgram> authorize(long actor, long enterprise, AnalyticsFilter filter) {
        enterprises.requireMember(actor, enterprise);
        var programs = dao.programs(enterprise);
        if (filter.scenario() != null
                && programs.stream()
                        .noneMatch(p -> p.scenarioId().equals(filter.scenario().toString())))
            throw new ApiException(
                    ErrorCode.FORBIDDEN, "You do not have permission to view these analytics.");
        return programs;
    }
}
