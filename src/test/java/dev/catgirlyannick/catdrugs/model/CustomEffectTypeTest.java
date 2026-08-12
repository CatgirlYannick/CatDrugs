package dev.catgirlyannick.catdrugs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomEffectTypeTest {
    @Test
    void parsesEveryAdvancedCustomEffectFromYamlStyleNames() {
        assertEquals(CustomEffectType.CAMERA_DRIFT, CustomEffectType.parse("camera_drift"));
        assertEquals(CustomEffectType.VISUAL_ECHO, CustomEffectType.parse("visual-echo"));
        assertEquals(CustomEffectType.AUDITORY_DISTORTION, CustomEffectType.parse("auditory_distortion"));
        assertEquals(CustomEffectType.FOCUS_PULSE, CustomEffectType.parse("focus-pulse"));
        assertEquals(CustomEffectType.TIME_DISTORTION, CustomEffectType.parse("time_distortion"));
        assertEquals(CustomEffectType.MUSCLE_TENSION, CustomEffectType.parse("muscle-tension"));
    }
}
