package dev.catgirlyannick.catdrugs.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoseTrackerTest {
    @Test
    void sumsRecentDosesAndExpiresOldEntries() {
        MutableClock clock = new MutableClock(1_000L);
        DoseTracker tracker = new DoseTracker(clock);
        UUID player = UUID.randomUUID();

        assertEquals(4, tracker.add(player, 4, 300));
        clock.millis = 2_000L;
        assertEquals(9, tracker.add(player, 5, 300));
        clock.millis = 302_000L;
        assertEquals(5, tracker.current(player, 300));
        clock.millis = 303_000L;
        assertEquals(0, tracker.current(player, 300));
    }

    @Test
    void clearRemovesPlayerState() {
        MutableClock clock = new MutableClock(1_000L);
        DoseTracker tracker = new DoseTracker(clock);
        UUID player = UUID.randomUUID();
        tracker.add(player, 6, 300);

        tracker.clear(player);

        assertEquals(0, tracker.current(player, 300));
    }

    private static final class MutableClock extends Clock {
        private long millis;

        private MutableClock(long millis) {
            this.millis = millis;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}
