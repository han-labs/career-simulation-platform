package edu.hcmute.careersim.guidance.infrastructure;

import java.time.Duration;
import java.util.function.Supplier;

/** Agent-only per-attempt cap; the outer execution deadline remains authoritative. */
public final class AgentTransportBudget {
    private static final ThreadLocal<Duration> CURRENT = new ThreadLocal<>();

    private AgentTransportBudget() {}

    public static Duration bound(Duration maximum) {
        var cap = CURRENT.get();
        return cap != null && cap.compareTo(maximum) < 0 ? cap : maximum;
    }

    public static <T> T within(Duration budget, Supplier<T> call) {
        var previous = CURRENT.get();
        CURRENT.set(budget);
        try {
            return call.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
