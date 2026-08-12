package dev.catgirlyannick.catdrugs.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedGameplayServiceTest {

    @Test
    void progressionIsBoundedAndToleranceReducesDuration() {
        assertEquals(100, AdvancedGameplayService.nextLevel(98, 5));
        assertEquals(0, AdvancedGameplayService.nextLevel(0, -4));
        assertEquals(3, AdvancedGameplayService.addictionGain(6));
        assertEquals(2, AdvancedGameplayService.toleranceGain(4));
        assertEquals(1.0, AdvancedGameplayService.effectiveness(0, 0.45), 0.001);
        assertEquals(0.45, AdvancedGameplayService.effectiveness(100, 0.45), 0.001);
    }

    @Test
    void mixingClassifiesDownerAndUpperCombinations() {
        assertTrue(AdvancedGameplayService.isHazardousMix("opioid", "sedative"));
        assertTrue(AdvancedGameplayService.isHazardousMix("stimulant", "depressant"));
        assertTrue(AdvancedGameplayService.isHazardousMix("inhalant", "party"));
        assertFalse(AdvancedGameplayService.isHazardousMix("cannabis", "herbal"));
    }
}
