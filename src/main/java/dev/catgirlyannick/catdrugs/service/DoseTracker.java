package dev.catgirlyannick.catdrugs.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DoseTracker {
    private final Clock clock;
    private final Map<UUID, DoseWindow> doses = new HashMap<>();

    public DoseTracker() {
        this(Clock.systemUTC());
    }

    DoseTracker(Clock clock) {
        this.clock = clock;
    }

    public int add(UUID playerId, int points, int windowSeconds) {
        long now = clock.millis();
        DoseWindow playerDoses = doses.computeIfAbsent(playerId, ignored -> new DoseWindow());
        purge(playerDoses, now - windowSeconds * 1000L);
        playerDoses.entries.addLast(new Dose(now, points));
        playerDoses.total += points;
        return playerDoses.total;
    }

    public int current(UUID playerId, int windowSeconds) {
        DoseWindow playerDoses = doses.get(playerId);
        if (playerDoses == null) {
            return 0;
        }
        purge(playerDoses, clock.millis() - windowSeconds * 1000L);
        if (playerDoses.entries.isEmpty()) {
            doses.remove(playerId);
        }
        return playerDoses.total;
    }

    public void clear(UUID playerId) {
        doses.remove(playerId);
    }

    private void purge(DoseWindow playerDoses, long cutoff) {
        while (!playerDoses.entries.isEmpty() && playerDoses.entries.getFirst().timestampMillis() < cutoff) {
            playerDoses.total -= playerDoses.entries.removeFirst().points();
        }
    }

    private static final class DoseWindow {
        private final ArrayDeque<Dose> entries = new ArrayDeque<>();
        private int total;
    }

    private record Dose(long timestampMillis, int points) {
    }
}
