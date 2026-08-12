package dev.catgirlyannick.catdrugs.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DoseTracker {
    private final Clock clock;
    private final Map<UUID, ArrayDeque<Dose>> doses = new HashMap<>();

    public DoseTracker() {
        this(Clock.systemUTC());
    }

    DoseTracker(Clock clock) {
        this.clock = clock;
    }

    public int add(UUID playerId, int points, int windowSeconds) {
        long now = clock.millis();
        ArrayDeque<Dose> playerDoses = doses.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        purge(playerDoses, now - windowSeconds * 1000L);
        playerDoses.addLast(new Dose(now, points));
        return playerDoses.stream().mapToInt(Dose::points).sum();
    }

    public int current(UUID playerId, int windowSeconds) {
        ArrayDeque<Dose> playerDoses = doses.get(playerId);
        if (playerDoses == null) {
            return 0;
        }
        purge(playerDoses, clock.millis() - windowSeconds * 1000L);
        return playerDoses.stream().mapToInt(Dose::points).sum();
    }

    public void clear(UUID playerId) {
        doses.remove(playerId);
    }

    private void purge(ArrayDeque<Dose> playerDoses, long cutoff) {
        while (!playerDoses.isEmpty() && playerDoses.getFirst().timestampMillis() < cutoff) {
            playerDoses.removeFirst();
        }
    }

    private record Dose(long timestampMillis, int points) {
    }
}
