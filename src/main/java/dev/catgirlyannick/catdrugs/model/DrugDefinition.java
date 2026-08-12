package dev.catgirlyannick.catdrugs.model;

import org.bukkit.Material;

import java.util.List;

public record DrugDefinition(
        String id,
        boolean enabled,
        boolean consumable,
        String category,
        String displayName,
        List<String> lore,
        Material fallbackMaterial,
        String customItemId,
        int cooldownSeconds,
        int dosePoints,
        List<EffectDefinition> immediateEffects,
        int afterDelayTicks,
        List<EffectDefinition> afterEffects
) {
}
