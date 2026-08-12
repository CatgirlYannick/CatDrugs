package dev.catgirlyannick.catdrugs.integration;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

/**
 * Optional packet-only perception effects. CatDrugs keeps a complete Paper
 * fallback and loads the PacketEvents implementation only when that plugin is
 * active, so PacketEvents never becomes a hard runtime dependency.
 */
public interface PerceptionPacketBridge {
    boolean available();

    void driftCamera(Player player, float relativeYaw, float relativePitch);

    String providerName();

    static PerceptionPacketBridge load(JavaPlugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("packetevents")) {
            return NoopPerceptionPacketBridge.INSTANCE;
        }
        try {
            Class<?> implementation = Class.forName(
                    "dev.catgirlyannick.catdrugs.integration.PacketEventsPerceptionPacketBridge",
                    true,
                    plugin.getClass().getClassLoader());
            return (PerceptionPacketBridge) implementation.getConstructor().newInstance();
        } catch (LinkageError | ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation
                    && invocation.getCause() != null ? invocation.getCause() : exception;
            plugin.getLogger().warning("PacketEvents is active but CatDrugs could not enable its perception bridge: "
                    + (cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()));
            return NoopPerceptionPacketBridge.INSTANCE;
        }
    }
}
