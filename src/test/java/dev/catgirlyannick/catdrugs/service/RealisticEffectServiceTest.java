package dev.catgirlyannick.catdrugs.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealisticEffectServiceTest {
    @Test
    void categoryIsDefaultProfile() {
        assertEquals("stimulant", RealisticEffectService.resolveProfileKey(
                "stimulant", "cocaine", Map.of()));
    }

    @Test
    void drugOverrideWinsOverCategory() {
        assertEquals("party", RealisticEffectService.resolveProfileKey(
                "stimulant", "mdma", Map.of("mdma", "party")));
    }
}
