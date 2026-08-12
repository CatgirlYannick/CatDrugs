package dev.catgirlyannick.catdrugs.model;

import java.util.List;

public record EffectPhaseDefinition(
        String id,
        int delayTicks,
        List<EffectDefinition> potionEffects,
        List<CustomEffectDefinition> customEffects
) {
}
