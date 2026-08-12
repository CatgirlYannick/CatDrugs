package dev.catgirlyannick.catdrugs.model;

import java.util.Locale;

public enum CustomEffectType {
    HEARTBEAT,
    TREMOR,
    IMPAIRED_COORDINATION,
    DEHYDRATION,
    RESPIRATORY_DEPRESSION,
    PERCEPTION_SHIFT,
    SEDATION,
    FATIGUE,
    APPETITE,
    OVERHEATING,
    DIZZINESS;

    public static CustomEffectType parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
