package dev.catgirlyannick.catdrugs.model;

import org.bukkit.potion.PotionEffectType;

public record EffectDefinition(PotionEffectType type, int durationTicks, int amplifier,
                               boolean ambient, boolean particles, boolean icon) {
}
