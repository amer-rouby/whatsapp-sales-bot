package com.salesbot.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptsTest {

    /** A clock the test moves by hand. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-08T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttempts attempts = new LoginAttempts(clock);

    @Test
    void blocksAfterMaxFailuresAndUnblocksWhenTheyLeaveTheWindow() {
        for (int i = 0; i < LoginAttempts.MAX_FAILURES; i++) {
            assertThat(attempts.isBlocked("a@test.local")).isFalse();
            attempts.recordFailure("a@test.local");
        }
        assertThat(attempts.isBlocked("a@test.local")).isTrue();
        assertThat(attempts.isBlocked("b@test.local")).isFalse();

        clock.advance(LoginAttempts.WINDOW.plusSeconds(1));
        assertThat(attempts.isBlocked("a@test.local")).isFalse();
    }

    @Test
    void forgetsEmailsWhoseFailuresExpired() {
        attempts.recordFailure("a@test.local");
        clock.advance(LoginAttempts.WINDOW.plusSeconds(1));

        attempts.isBlocked("a@test.local");

        assertThat(attempts.trackedEmails()).isZero();
    }

    @Test
    void sprayingRandomEmailsDoesNotGrowForever() {
        for (int i = 0; i < LoginAttempts.SWEEP_THRESHOLD; i++) {
            attempts.recordFailure("spray" + i + "@test.local");
        }
        clock.advance(LoginAttempts.WINDOW.plusSeconds(1));

        attempts.recordFailure("one-more@test.local"); // crosses the threshold → expired ones are swept

        assertThat(attempts.trackedEmails()).isEqualTo(1);
    }

    @Test
    void successfulLoginClearsFailures() {
        attempts.recordFailure("a@test.local");
        attempts.reset("a@test.local");
        assertThat(attempts.trackedEmails()).isZero();
    }
}
