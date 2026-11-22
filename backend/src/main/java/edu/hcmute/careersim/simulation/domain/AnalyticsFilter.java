package edu.hcmute.careersim.simulation.domain;

import edu.hcmute.careersim.common.value.CoreValues;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;

public record AnalyticsFilter(
        Long scenario, OffsetDateTime from, OffsetDateTime until, String status) {
    public static final String TIME_ZONE = "Asia/Ho_Chi_Minh";
    private static final Set<String> STATES = Set.of("ENROLLED", "IN_PROGRESS", "COMPLETED");

    public static AnalyticsFilter of(
            String scenario, LocalDate from, LocalDate through, String status) {
        if (from != null && through != null && from.isAfter(through))
            CoreValues.invalid("The registration start date must be on or before the end date.");
        if (status != null && !STATES.contains(status))
            CoreValues.invalid("Invalid enrollment status.");
        if (through != null && through.getYear() > 9999 || from != null && from.getYear() < 1)
            CoreValues.invalid("Invalid registration date.");
        var zone = ZoneId.of(TIME_ZONE);
        return new AnalyticsFilter(
                scenario == null ? null : CoreValues.id(scenario),
                from == null ? null : from.atStartOfDay(zone).toOffsetDateTime(),
                through == null ? null : through.plusDays(1).atStartOfDay(zone).toOffsetDateTime(),
                status);
    }
}
