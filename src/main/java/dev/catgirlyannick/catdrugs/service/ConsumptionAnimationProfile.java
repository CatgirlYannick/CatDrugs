package dev.catgirlyannick.catdrugs.service;

import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.configuration.file.YamlConfiguration;

public record ConsumptionAnimationProfile(String preset, int durationTicks) {
    public static ConsumptionAnimationProfile resolve(YamlConfiguration config, DrugDefinition drug) {
        String fallback = defaultPreset(drug.category());
        String categoryPreset = config.getString("consumption-animations.category-presets." + drug.category(), fallback);
        String preset = config.getString("consumption-animations.drug-overrides." + drug.id(), categoryPreset)
                .trim().toLowerCase(java.util.Locale.ROOT);
        if (preset.isBlank()) {
            preset = fallback;
        }
        int defaultDuration = config.getInt("consumption-animations.default-duration-ticks", 32);
        int duration = config.getInt("consumption-animations.duration-by-preset." + preset, defaultDuration);
        return new ConsumptionAnimationProfile(preset, clamp(duration, 8, 120));
    }

    static String defaultPreset(String category) {
        return switch (category == null ? "" : category.toLowerCase(java.util.Locale.ROOT)) {
            case "cannabis" -> "smoke_joint";
            case "stimulant", "party", "dissociative" -> "snort_line";
            case "depressant", "sedative" -> "drink_bottle";
            case "herbal" -> "eat_edible";
            case "inhalant", "synthetic" -> "inhale_vape";
            case "opioid", "medical" -> "inject_arm";
            case "psychedelic" -> "ritual_sway";
            default -> "swallow_pill";
        };
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
