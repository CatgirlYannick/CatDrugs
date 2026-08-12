package dev.catgirlyannick.catdrugs.integration;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;
import org.bukkit.entity.Player;

public final class PacketEventsPerceptionPacketBridge implements PerceptionPacketBridge {
    public PacketEventsPerceptionPacketBridge() {
        if (PacketEvents.getAPI() == null || !PacketEvents.getAPI().isInitialized()) {
            throw new IllegalStateException("PacketEvents API is not initialized");
        }
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void driftCamera(Player player, float relativeYaw, float relativePitch) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerPlayerRotation(relativeYaw, true, relativePitch, true));
    }

    @Override
    public String providerName() {
        return "PacketEvents";
    }
}

