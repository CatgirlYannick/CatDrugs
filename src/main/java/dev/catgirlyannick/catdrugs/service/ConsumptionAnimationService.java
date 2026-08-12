package dev.catgirlyannick.catdrugs.service;

import dev.catgirlyannick.catdrugs.integration.CatItemsBridge;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ConsumptionAnimationService {
    private final JavaPlugin plugin;
    private final CatItemsBridge catItems;
    private final Map<UUID, BukkitTask> active = new HashMap<>();
    private YamlConfiguration config;

    public ConsumptionAnimationService(JavaPlugin plugin, CatItemsBridge catItems, YamlConfiguration config) {
        this.plugin = plugin;
        this.catItems = catItems;
        this.config = config;
    }

    public void reload(YamlConfiguration config) {
        this.config = config;
    }

    public boolean isPlaying(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public void play(Player player, DrugDefinition drug, Runnable completion) {
        if (!config.getBoolean("consumption-animations.enabled", true)) {
            completion.run();
            return;
        }
        ConsumptionAnimationProfile profile = ConsumptionAnimationProfile.resolve(config, drug);
        boolean delegated = catItems.playUseAnimation(player, profile.preset(), profile.durationTicks());
        if (!delegated) {
            playFallback(player, profile);
        }
        BukkitTask completionTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            active.remove(player.getUniqueId());
            if (player.isOnline()) {
                completion.run();
            }
        }, profile.durationTicks());
        active.put(player.getUniqueId(), completionTask);
    }

    public void clear(Player player) {
        BukkitTask task = active.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        catItems.stopUseAnimation(player);
    }

    public String providerStatus() {
        return catItems.supportsUseAnimations() ? "CatItems API" : "built-in fallback";
    }

    private void playFallback(Player player, ConsumptionAnimationProfile profile) {
        new BukkitRunnable() {
            private int elapsed;

            @Override
            public void run() {
                if (!player.isOnline() || elapsed > profile.durationTicks()) {
                    cancel();
                    return;
                }
                if (elapsed % 8 == 0) {
                    player.swingMainHand();
                }
                if (elapsed % 4 == 0) {
                    Location focus = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.55));
                    player.getWorld().spawnParticle(fallbackParticle(profile.preset()), focus, 2,
                            0.05, 0.05, 0.05, 0.005);
                }
                if (elapsed == 0) {
                    player.playSound(player.getLocation(), fallbackSound(profile.preset()), 0.55f, 1.0f);
                }
                elapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private Particle fallbackParticle(String preset) {
        return switch (preset) {
            case "smoke" -> Particle.CAMPFIRE_COSY_SMOKE;
            case "snort", "inhale" -> Particle.CLOUD;
            case "inject" -> Particle.ELECTRIC_SPARK;
            case "ritual" -> Particle.WITCH;
            default -> Particle.ENCHANTED_HIT;
        };
    }

    private Sound fallbackSound(String preset) {
        return switch (preset) {
            case "smoke" -> Sound.BLOCK_CAMPFIRE_CRACKLE;
            case "snort", "inhale" -> Sound.ENTITY_PLAYER_BREATH;
            case "eat" -> Sound.ENTITY_GENERIC_EAT;
            case "inject" -> Sound.ITEM_TRIDENT_RETURN;
            case "ritual" -> Sound.BLOCK_BREWING_STAND_BREW;
            default -> Sound.ENTITY_GENERIC_DRINK;
        };
    }
}
