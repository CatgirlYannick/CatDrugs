package dev.catgirlyannick.catdrugs.service;

import dev.catgirlyannick.catdrugs.model.CustomEffectDefinition;
import dev.catgirlyannick.catdrugs.model.CustomEffectType;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import dev.catgirlyannick.catdrugs.model.EffectDefinition;
import dev.catgirlyannick.catdrugs.model.EffectPhaseDefinition;
import dev.catgirlyannick.catdrugs.model.EffectProfile;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies fictional, gameplay-oriented symptom phases. The profiles deliberately
 * avoid dose advice and are not a medical simulation.
 */
public final class RealisticEffectService {
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, List<BukkitTask>> activeTasks = new HashMap<>();
    private Map<String, EffectProfile> profiles = Map.of();
    private Map<String, String> overrides = Map.of();
    private boolean enabled;
    private boolean phaseMessages;
    private boolean replaceLegacyEffects;

    public RealisticEffectService(JavaPlugin plugin, MessageService messages, YamlConfiguration config) {
        this.plugin = plugin;
        this.messages = messages;
        reload(config);
    }

    public void reload(YamlConfiguration config) {
        enabled = config.getBoolean("enabled", true);
        phaseMessages = config.getBoolean("phase-messages", true);
        replaceLegacyEffects = config.getBoolean("replace-legacy-effects", true);
        profiles = parseProfiles(config);
        ConfigurationSection overrideSection = config.getConfigurationSection("drug-overrides");
        Map<String, String> parsedOverrides = new HashMap<>();
        if (overrideSection != null) {
            overrideSection.getKeys(false).forEach(id -> parsedOverrides.put(
                    id.toLowerCase(Locale.ROOT), overrideSection.getString(id, "").toLowerCase(Locale.ROOT)));
        }
        overrides = Map.copyOf(parsedOverrides);
        clearAll();
    }

    public void start(Player player, DrugDefinition drug) {
        start(player, drug, 1.0);
    }

    public void start(Player player, DrugDefinition drug, double effectiveness) {
        if (!enabled) {
            return;
        }
        String profileKey = resolveProfileKey(drug.category(), drug.id(), overrides);
        EffectProfile profile = profiles.get(profileKey);
        if (profile == null) {
            plugin.getLogger().warning("No realistic effect profile '" + profileKey + "' for " + drug.id() + ".");
            return;
        }
        double safeEffectiveness = Math.max(0.1, Math.min(effectiveness, 1.0));
        for (EffectPhaseDefinition phase : profile.phases()) {
            track(player.getUniqueId(), plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> applyPhase(player, drug, phase, safeEffectiveness), phase.delayTicks()));
        }
    }

    private void applyPhase(Player player, DrugDefinition drug, EffectPhaseDefinition phase,
                            double effectiveness) {
        if (!player.isOnline()) {
            return;
        }
        for (EffectDefinition effect : phase.potionEffects()) {
            int duration = Math.max(20, (int) Math.round(effect.durationTicks() * effectiveness));
            player.addPotionEffect(new PotionEffect(effect.type(), duration, effect.amplifier(),
                    effect.ambient(), effect.particles(), effect.icon()));
        }
        for (CustomEffectDefinition effect : phase.customEffects()) {
            startCustomEffect(player, effect);
        }
        if (phaseMessages) {
            messages.send(player, "consumption.phases." + phase.id(), Map.of(
                    "drug", plainName(drug.displayName())));
        }
    }

    private void startCustomEffect(Player player, CustomEffectDefinition effect) {
        UUID playerId = player.getUniqueId();
        long started = System.currentTimeMillis();
        long durationMillis = effect.durationTicks() * 50L;
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || System.currentTimeMillis() - started >= durationMillis) {
                    cancel();
                    return;
                }
                applyCustomTick(player, effect);
            }
        }.runTaskTimer(plugin, 0L, effect.intervalTicks());
        track(playerId, task);
    }

    private void applyCustomTick(Player player, CustomEffectDefinition effect) {
        int intensity = effect.intensity();
        switch (effect.type()) {
            case HEARTBEAT -> player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT,
                    0.18f + intensity * 0.08f, Math.max(0.55f, 1.05f - intensity * 0.1f));
            case TREMOR -> {
                player.swingMainHand();
                player.getWorld().spawnParticle(Particle.CRIT, player.getEyeLocation(), intensity,
                        0.18, 0.12, 0.18, 0.01);
            }
            case IMPAIRED_COORDINATION -> nudgeSideways(player, intensity);
            case DEHYDRATION -> player.setExhaustion(Math.min(4.0f,
                    player.getExhaustion() + 0.16f * intensity));
            case RESPIRATORY_DEPRESSION -> player.setRemainingAir(Math.max(40,
                    player.getRemainingAir() - 8 * intensity));
            case PERCEPTION_SHIFT -> {
                player.getWorld().spawnParticle(Particle.PORTAL, player.getEyeLocation(), 3 * intensity,
                        0.45, 0.3, 0.45, 0.05);
                if (ThreadLocalRandom.current().nextInt(3) == 0) {
                    player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.18f, randomPitch());
                }
            }
            case SEDATION -> slowHorizontalMotion(player, Math.max(0.45, 0.92 - intensity * 0.08));
            case FATIGUE -> {
                player.setExhaustion(Math.min(4.0f, player.getExhaustion() + 0.12f * intensity));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, Math.min(2, intensity - 1),
                        false, false, true));
            }
            case APPETITE -> player.setFoodLevel(Math.max(0, player.getFoodLevel() - 1));
            case OVERHEATING -> {
                player.setExhaustion(Math.min(4.0f, player.getExhaustion() + 0.2f * intensity));
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), intensity,
                        0.25, 0.3, 0.25, 0.01);
            }
            case DIZZINESS -> player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,
                    35, Math.min(1, intensity - 1), false, false, true));
        }
    }

    private void nudgeSideways(Player player, int intensity) {
        if (!player.isOnGround() || player.getVelocity().lengthSquared() < 0.005) {
            return;
        }
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Vector sideways = new Vector(-direction.getZ(), 0, direction.getX())
                .multiply((ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * 0.025 * intensity);
        player.setVelocity(player.getVelocity().add(sideways));
    }

    private void slowHorizontalMotion(Player player, double factor) {
        Vector velocity = player.getVelocity();
        player.setVelocity(new Vector(velocity.getX() * factor, velocity.getY(), velocity.getZ() * factor));
    }

    private Map<String, EffectProfile> parseProfiles(YamlConfiguration config) {
        ConfigurationSection root = config.getConfigurationSection("profiles");
        if (root == null) {
            return Map.of();
        }
        Map<String, EffectProfile> parsed = new LinkedHashMap<>();
        for (String profileId : root.getKeys(false)) {
            ConfigurationSection phases = root.getConfigurationSection(profileId + ".phases");
            if (phases == null) {
                continue;
            }
            List<EffectPhaseDefinition> parsedPhases = new ArrayList<>();
            for (String phaseId : phases.getKeys(false)) {
                ConfigurationSection phase = phases.getConfigurationSection(phaseId);
                if (phase == null) {
                    continue;
                }
                int delay = bounded(phase.getInt("delay-seconds", 0), 0, 3600) * 20;
                parsedPhases.add(new EffectPhaseDefinition(phaseId, delay,
                        parsePotionEffects(phase.getMapList("potion-effects")),
                        parseCustomEffects(phase.getMapList("custom-effects"))));
            }
            parsed.put(profileId.toLowerCase(Locale.ROOT),
                    new EffectProfile(profileId.toLowerCase(Locale.ROOT), List.copyOf(parsedPhases)));
        }
        return Map.copyOf(parsed);
    }

    private List<EffectDefinition> parsePotionEffects(List<Map<?, ?>> values) {
        List<EffectDefinition> result = new ArrayList<>();
        for (Map<?, ?> value : values) {
            Object rawType = value.containsKey("type") ? value.get("type") : "";
            String key = String.valueOf(rawType).toLowerCase(Locale.ROOT);
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(key));
            if (type == null) {
                plugin.getLogger().warning("Unknown realistic potion effect: " + key);
                continue;
            }
            int seconds = bounded(number(value.get("duration-seconds"), 1), 1, 3600);
            int amplifier = bounded(number(value.get("amplifier"), 0), 0, 10);
            result.add(new EffectDefinition(type, seconds * 20, amplifier, false,
                    bool(value.get("particles"), false), bool(value.get("icon"), true)));
        }
        return List.copyOf(result);
    }

    private List<CustomEffectDefinition> parseCustomEffects(List<Map<?, ?>> values) {
        List<CustomEffectDefinition> result = new ArrayList<>();
        for (Map<?, ?> value : values) {
            try {
                Object rawType = value.containsKey("type") ? value.get("type") : "";
                CustomEffectType type = CustomEffectType.parse(String.valueOf(rawType));
                int seconds = bounded(number(value.get("duration-seconds"), 10), 1, 3600);
                int interval = bounded(number(value.get("interval-ticks"), 40), 5, 1200);
                int intensity = bounded(number(value.get("intensity"), 1), 1, 5);
                result.add(new CustomEffectDefinition(type, seconds * 20, interval, intensity));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Unknown realistic custom effect: " + value.get("type"));
            }
        }
        return List.copyOf(result);
    }

    static String resolveProfileKey(String category, String drugId, Map<String, String> overrides) {
        return overrides.getOrDefault(drugId.toLowerCase(Locale.ROOT), category.toLowerCase(Locale.ROOT));
    }

    public boolean replacesLegacyEffects() {
        return enabled && replaceLegacyEffects;
    }

    public void clear(UUID playerId) {
        List<BukkitTask> tasks = activeTasks.remove(playerId);
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
        }
    }

    public void clearAll() {
        activeTasks.values().stream().flatMap(List::stream).forEach(BukkitTask::cancel);
        activeTasks.clear();
    }

    private void track(UUID playerId, BukkitTask task) {
        activeTasks.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(task);
    }

    private static int bounded(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static float randomPitch() {
        return (float) ThreadLocalRandom.current().nextDouble(0.55, 1.35);
    }

    private static String plainName(String miniMessage) {
        return miniMessage.replaceAll("<[^>]+>", "");
    }
}
