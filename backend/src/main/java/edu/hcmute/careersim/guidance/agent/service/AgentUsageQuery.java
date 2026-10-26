package edu.hcmute.careersim.guidance.agent.service;

import static edu.hcmute.careersim.guidance.agent.dto.AgentUsageContracts.*;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Aggregate projections only. No Student identifiers, prompts, reports, arguments or transcripts.
 */
@Service
@RequiredArgsConstructor
public class AgentUsageQuery implements AgentUsageService {
    private final edu.hcmute.careersim.guidance.agent.dao.AgentUsageDao usage;
    private final ActorAccess actors;

    @org.springframework.transaction.annotation.Transactional(
            readOnly = true,
            isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Dashboard dashboard(long admin, Filter f) {
        ActorAccess.requireRole(actors.requireId(admin), "ADMIN");
        if (f == null
                || f.from() == null
                || f.to() == null
                || !f.to().isAfter(f.from())
                || Duration.between(f.from(), f.to()).compareTo(Duration.ofDays(366)) > 0
                || f.limit() < 1
                || f.limit() > 50
                || f.outcome() != null
                        && !Set.of(
                                        "ACCEPTED",
                                        "RUNNING",
                                        "SUCCEEDED",
                                        "FAILED",
                                        "REFUSED",
                                        "CANCELLED")
                                .contains(f.outcome())
                || f.mode() != null
                        && !Set.of("STUDENT_CHAT", "ADMIN_PREVIEW", "KB_TEST").contains(f.mode())
                || f.provider() != null && !f.provider().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,39}")
                || f.model() != null && !f.model().matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,179}"))
            CoreValues.invalid(
                    "Invalid usage filters; select a positive range of at most 366 days and limit 1 to 50.");
        Position position = null;
        if (f.cursor() != null) {
            try {
                if (f.cursor().length() > 256 || !f.cursor().matches("[A-Za-z0-9_-]+"))
                    throw new IllegalArgumentException();
                var parts =
                        new String(
                                        Base64.getUrlDecoder().decode(f.cursor()),
                                        java.nio.charset.StandardCharsets.UTF_8)
                                .split("\\|", -1);
                if (parts.length != 2) throw new IllegalArgumentException();
                position = new Position(Instant.parse(parts[0]), UUID.fromString(parts[1]));
                if (position.startedAt().isBefore(f.from())
                        || !position.startedAt().isBefore(f.to()))
                    throw new IllegalArgumentException();
            } catch (RuntimeException invalid) {
                CoreValues.invalid("Invalid usage cursor.");
            }
        }
        String unit =
                Duration.between(f.from(), f.to()).compareTo(Duration.ofHours(48)) <= 0
                        ? "hour"
                        : "day";
        var summary = usage.summary(f);
        var trend = usage.trend(f, unit);
        var rows = usage.recent(f, position);
        String next = null;
        if (rows.size() > f.limit()) {
            var last = rows.get(f.limit() - 1);
            next =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    (last.startedAt() + "|" + last.id())
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return new Dashboard(
                f.from(),
                f.to(),
                unit,
                summary,
                trend,
                usage.options(f),
                rows.stream().limit(f.limit()).toList(),
                next);
    }

    public Map<String, Object> query(long admin, Instant from, Instant to, String mode) {
        ActorAccess.requireRole(actors.requireId(admin), "ADMIN");
        if (from == null
                || to == null
                || !to.isAfter(from)
                || Duration.between(from, to).toDays() > 366
                || mode != null
                        && !Set.of("STUDENT_CHAT", "ADMIN_PREVIEW", "KB_TEST").contains(mode))
            CoreValues.invalid("Invalid usage range or mode.");
        return usage.aggregate(from, to, mode);
    }
}
