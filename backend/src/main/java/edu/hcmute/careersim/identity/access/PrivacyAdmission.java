package edu.hcmute.careersim.identity.access;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Single-instance admission coordination. Stripes bound memory; collisions only serialize. */
@Component
public final class PrivacyAdmission {
    private final ReentrantLock[] locks = new ReentrantLock[1024];

    public PrivacyAdmission() {
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock(true);
    }

    public <T> T serialized(long owner, Supplier<T> action) {
        var lock = locks[Math.floorMod(Long.hashCode(owner), locks.length)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
