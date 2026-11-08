package com.salesbot.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Brute-force guard: after {@link #MAX_FAILURES} wrong passwords for one email inside {@link #WINDOW},
 * further attempts are refused until the oldest failure leaves the window.
 * <p>
 * Memory stays bounded: each email keeps at most {@link #MAX_FAILURES} timestamps, an email is forgotten
 * as soon as its failures leave the window, and when many emails are tracked (someone spraying random
 * addresses) the expired ones are swept out.
 * <p>
 * Known trade-off: someone who knows the owner's email can keep that email paused by failing on purpose.
 * Per-client limits need the client's real address, which only the dashboard server sees; revisit when
 * the dashboard is exposed publicly. In memory on purpose: one backend instance today.
 */
@Component
class LoginAttempts {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    /** Above this many tracked emails, a failure triggers a sweep of the expired ones. */
    static final int SWEEP_THRESHOLD = 10_000;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    LoginAttempts() {
        this(Clock.systemUTC());
    }

    LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    boolean isBlocked(String email) {
        // compute* runs atomically per key; returning null removes an email whose failures all expired
        Deque<Instant> recent = failures.computeIfPresent(email, (key, deque) -> pruned(deque));
        return recent != null && recent.size() >= MAX_FAILURES;
    }

    void recordFailure(String email) {
        failures.compute(email, (key, deque) -> {
            Deque<Instant> recent = deque != null ? deque : new ArrayDeque<>();
            prune(recent);
            recent.addLast(clock.instant());
            while (recent.size() > MAX_FAILURES) {
                recent.pollFirst(); // only the latest MAX_FAILURES matter for blocking
            }
            return recent;
        });
        if (failures.size() > SWEEP_THRESHOLD) {
            failures.keySet().forEach(key -> failures.computeIfPresent(key, (k, deque) -> pruned(deque)));
        }
    }

    void reset(String email) {
        failures.remove(email);
    }

    int trackedEmails() {
        return failures.size();
    }

    /** The deque without expired failures, or null (= forget the email) when nothing is left. */
    private Deque<Instant> pruned(Deque<Instant> deque) {
        prune(deque);
        return deque.isEmpty() ? null : deque;
    }

    private void prune(Deque<Instant> recent) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
            recent.pollFirst();
        }
    }
}
