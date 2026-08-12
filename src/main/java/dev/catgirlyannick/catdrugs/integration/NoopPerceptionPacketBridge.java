package dev.catgirlyannick.catdrugs.integration;

import org.bukkit.entity.Player;

final class NoopPerceptionPacketBridge implements PerceptionPacketBridge {
    static final NoopPerceptionPacketBridge INSTANCE = new NoopPerceptionPacketBridge();

    private NoopPerceptionPacketBridge() {
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public void driftCamera(Player player, float relativeYaw, float relativePitch) {
        // The Paper fallback keeps visual echoes, sound displacement and pulse feedback active.
    }

    @Override
    public String providerName() {
        return "Paper fallback";
    }
}

