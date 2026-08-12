package dev.catgirlyannick.catdrugs.model;

public record CustomEffectDefinition(
        CustomEffectType type,
        int durationTicks,
        int intervalTicks,
        int intensity
) {
}
