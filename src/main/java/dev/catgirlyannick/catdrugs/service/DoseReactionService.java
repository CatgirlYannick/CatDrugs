package dev.catgirlyannick.catdrugs.service;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Applies escalating fictional acute reactions when dose thresholds are crossed. */
public final class DoseReactionService {
    enum Reaction { NONE, NAUSEA, VOMITING, BLACKOUT }

    private static final Set<Material> UNSAFE_GROUND = EnumSet.of(
            Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK,
            Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.POWDER_SNOW, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE);

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, BukkitTask> blackoutTasks = new HashMap<>();
    private YamlConfiguration config;

    public DoseReactionService(JavaPlugin plugin, MessageService messages, YamlConfiguration config) {
        this.plugin = plugin;
        this.messages = messages;
        this.config = config;
    }

    public void reload(YamlConfiguration config) {
        this.config = config;
        clearAll();
    }

    public void evaluate(Player player, int previousPoints, int totalPoints) {
        if (!config.getBoolean("dose-reactions.enabled", true)) {
            return;
        }
        int nausea = clamp(config.getInt("dose-reactions.nausea-threshold-points", 6), 1, 1000);
        int vomiting = clamp(config.getInt("dose-reactions.vomiting-threshold-points", 10), nausea, 1000);
        int blackout = clamp(config.getInt("dose-reactions.blackout-threshold-points", 16), vomiting, 1000);
        switch (selectReaction(previousPoints, totalPoints, nausea, vomiting, blackout)) {
            case NAUSEA -> nausea(player, totalPoints);
            case VOMITING -> vomit(player, totalPoints);
            case BLACKOUT -> blackout(player, totalPoints);
            case NONE -> { }
        }
    }

    static Reaction selectReaction(int previous, int total, int nausea, int vomiting, int blackout) {
        if (previous < blackout && total >= blackout) {
            return Reaction.BLACKOUT;
        }
        if (previous < vomiting && total >= vomiting) {
            return Reaction.VOMITING;
        }
        if (previous < nausea && total >= nausea) {
            return Reaction.NAUSEA;
        }
        return Reaction.NONE;
    }

    private void nausea(Player player, int points) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 14 * 20, 0, false, false, true));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.45f, 0.65f);
        messages.send(player, "consumption.reactions.nausea", Map.of("points", Integer.toString(points)));
    }

    private void vomit(Player player, int points) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 22 * 20, 1, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 18 * 20, 0, false, false, true));
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - 4));
        player.setSaturation(Math.max(0.0f, player.getSaturation() - 5.0f));
        messages.send(player, "consumption.reactions.vomiting", Map.of("points", Integer.toString(points)));
        for (int burst = 0; burst < 4; burst++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                Location mouth = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.45));
                player.getWorld().spawnParticle(Particle.ITEM, mouth, 16, 0.12, 0.08, 0.12, 0.08,
                        new ItemStack(Material.SLIME_BALL));
                player.getWorld().spawnParticle(Particle.SPLASH, mouth, 8, 0.15, 0.08, 0.15, 0.05);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.9f,
                        (float) ThreadLocalRandom.current().nextDouble(0.5, 0.8));
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.35f, 0.8f);
            }, burst * 9L);
        }
    }

    private void blackout(Player player, int points) {
        UUID playerId = player.getUniqueId();
        if (blackoutTasks.containsKey(playerId)) {
            return;
        }
        int seconds = clamp(config.getInt("dose-reactions.blackout-duration-seconds", 7), 3, 30);
        int radius = clamp(config.getInt("dose-reactions.blackout-radius-blocks", 150), 16, 150);
        int attempts = clamp(config.getInt("dose-reactions.safe-location-attempts", 24), 4, 48);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (seconds + 3) * 20,
                0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20,
                10, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (seconds + 8) * 20,
                2, false, false, true));
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 0.5f);
        messages.send(player, "consumption.reactions.blackout", Map.of("points", Integer.toString(points)));

        Location origin = player.getLocation().clone();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            blackoutTasks.remove(playerId);
            if (!player.isOnline()) {
                return;
            }
            Location destination = findSafeWakeLocation(origin, radius, attempts);
            boolean moved = destination != null;
            if (!moved) {
                destination = origin;
            }
            player.teleport(destination);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 3 * 20,
                    0, false, false, true));
            player.playSound(destination, Sound.BLOCK_PORTAL_TRAVEL, 0.35f, 0.65f);
            messages.send(player, moved ? "consumption.reactions.wake-up" : "consumption.reactions.wake-up-fallback");
        }, seconds * 20L);
        blackoutTasks.put(playerId, task);
    }

    Location findSafeWakeLocation(Location origin, int radius, int attempts) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = random.nextDouble(Math.PI * 2.0);
            double distance = Math.sqrt(random.nextDouble()) * radius;
            int x = origin.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            int groundY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Location candidate = new Location(world, x + 0.5, groundY + 1.0, z + 0.5,
                    origin.getYaw(), origin.getPitch());
            if (isSafe(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static boolean isSafe(Location feet) {
        World world = feet.getWorld();
        if (world == null || feet.getBlockY() <= world.getMinHeight() + 1
                || feet.getBlockY() + 1 >= world.getMaxHeight()
                || !world.getWorldBorder().isInside(feet)) {
            return false;
        }
        Block ground = feet.clone().subtract(0, 1, 0).getBlock();
        Block body = feet.getBlock();
        Block head = feet.clone().add(0, 1, 0).getBlock();
        if (!ground.getType().isSolid() || UNSAFE_GROUND.contains(ground.getType())
                || !body.isPassable() || !head.isPassable()
                || body.isLiquid() || head.isLiquid()) {
            return false;
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Material nearby = body.getRelative(x, 0, z).getType();
                if (UNSAFE_GROUND.contains(nearby) || nearby == Material.WATER) {
                    return false;
                }
            }
        }
        return true;
    }

    public void clear(UUID playerId) {
        BukkitTask task = blackoutTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void clearAll() {
        blackoutTasks.values().forEach(BukkitTask::cancel);
        blackoutTasks.clear();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
