package dev.catgirlyannick.catdrugs.service;

import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import dev.catgirlyannick.catdrugs.model.EffectDefinition;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class ConsumptionService {
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final RealisticEffectService realisticEffects;
    private final DoseReactionService doseReactions;
    private final AdvancedGameplayService advancedGameplay;
    private final ConsumptionAnimationService animations;
    private final DoseTracker doseTracker = new DoseTracker();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private YamlConfiguration config;
    private boolean consumptionEnabled;
    private boolean overdoseEnabled;
    private int doseWindowSeconds;
    private int overdoseThreshold;
    private double overdoseDamage;

    public ConsumptionService(JavaPlugin plugin, MessageService messages, YamlConfiguration config,
                              RealisticEffectService realisticEffects, DoseReactionService doseReactions,
                              AdvancedGameplayService advancedGameplay, ConsumptionAnimationService animations) {
        this.plugin = plugin;
        this.messages = messages;
        this.config = config;
        this.realisticEffects = realisticEffects;
        this.doseReactions = doseReactions;
        this.advancedGameplay = advancedGameplay;
        this.animations = animations;
        cacheSettings();
    }

    public void reload(YamlConfiguration config) {
        this.config = config;
        animations.reload(config);
        cacheSettings();
    }

    public boolean consume(Player player, DrugDefinition drug, BooleanSupplier commitItem) {
        if (!consumptionEnabled) {
            messages.send(player, "errors.feature-disabled");
            return false;
        }
        if (!drug.enabled() || !drug.consumable()) {
            messages.send(player, "errors.not-consumable");
            return false;
        }
        if (animations.isPlaying(player)) {
            return false;
        }
        long now = System.currentTimeMillis();
        long next = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .getOrDefault(drug.id(), 0L);
        if (next > now) {
            long seconds = Math.max(1, (next - now + 999) / 1000);
            messages.send(player, "errors.cooldown", Map.of("seconds", Long.toString(seconds)));
            return false;
        }
        cooldowns.get(player.getUniqueId()).put(drug.id(), now + drug.cooldownSeconds() * 1000L);
        animations.play(player, drug, () -> {
            if (!commitItem.getAsBoolean()) {
                Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
                if (playerCooldowns != null) {
                    playerCooldowns.remove(drug.id());
                }
                return;
            }
            completeConsumption(player, drug);
        });
        return true;
    }

    private void completeConsumption(Player player, DrugDefinition drug) {
        AdvancedGameplayService.ConsumptionModifiers modifiers = advancedGameplay.onConsumption(player, drug);
        if (!realisticEffects.replacesLegacyEffects()) {
            apply(player, drug.immediateEffects(), modifiers.effectiveness());
        }
        realisticEffects.start(player, drug, modifiers.effectiveness());
        if (!realisticEffects.replacesLegacyEffects() && !drug.afterEffects().isEmpty()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    apply(player, drug.afterEffects(), modifiers.effectiveness());
                }
            }, drug.afterDelayTicks());
        }
        messages.send(player, "consumption.used", Map.of("drug", plainName(drug.displayName())));
        checkOverdose(player, drug, modifiers.extraDosePoints());
    }

    private void checkOverdose(Player player, DrugDefinition drug, int extraDosePoints) {
        int previous = doseTracker.current(player.getUniqueId(), doseWindowSeconds);
        int total = doseTracker.add(player.getUniqueId(), drug.dosePoints() + Math.max(0, extraDosePoints),
                doseWindowSeconds);
        doseReactions.evaluate(player, previous, total);
        if (!overdoseEnabled) {
            return;
        }
        if (total < overdoseThreshold) {
            return;
        }
        if (overdoseDamage > 0.0) {
            player.damage(overdoseDamage);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 20, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30 * 20, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 1, false, true, true));
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.7f);
        messages.send(player, "consumption.overdose", Map.of("points", Integer.toString(total)));
    }

    private void apply(Player player, Iterable<EffectDefinition> effects, double effectiveness) {
        for (EffectDefinition effect : effects) {
            int duration = Math.max(20, (int) Math.round(effect.durationTicks() * effectiveness));
            player.addPotionEffect(new PotionEffect(effect.type(), duration, effect.amplifier(),
                    effect.ambient(), effect.particles(), effect.icon()));
        }
    }

    public int currentDose(Player player) {
        return doseTracker.current(player.getUniqueId(), doseWindowSeconds);
    }

    public void clear(Player player) {
        UUID playerId = player.getUniqueId();
        animations.clear(player);
        cooldowns.remove(playerId);
        doseTracker.clear(playerId);
        realisticEffects.clear(playerId);
        doseReactions.clear(playerId);
    }

    public void stabilize(Player player) {
        UUID playerId = player.getUniqueId();
        doseTracker.clear(playerId);
        realisticEffects.clear(playerId);
        doseReactions.clear(playerId);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void cacheSettings() {
        consumptionEnabled = config.getBoolean("features.consumption.enabled", true);
        overdoseEnabled = config.getBoolean("features.overdose.enabled", true);
        doseWindowSeconds = clamp(config.getInt("overdose.window-seconds", 300), 10, 3600);
        overdoseThreshold = clamp(config.getInt("overdose.threshold-points", 10), 1, 1000);
        overdoseDamage = Math.max(0.0, Math.min(config.getDouble("overdose.damage", 6.0), 100.0));
    }

    private static String plainName(String miniMessage) {
        return miniMessage.replaceAll("<[^>]+>", "");
    }
}
