package dev.catgirlyannick.catdrugs.service;

import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumptionAnimationProfileTest {
    @Test
    void resolvesCategoryPresetDrugOverrideAndDuration() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("consumption-animations.category-presets.stimulant", "snort_line");
        config.set("consumption-animations.drug-overrides.meth", "smoke_stimulant");
        config.set("consumption-animations.duration-by-preset.smoke_stimulant", 34);
        DrugDefinition meth = new DrugDefinition("meth", true, true, "stimulant", "Meth", List.of(),
                Material.SUGAR, "catdrugs:meth", 7, 4, List.of(), 0, List.of());

        ConsumptionAnimationProfile profile = ConsumptionAnimationProfile.resolve(config, meth);

        assertEquals("smoke_stimulant", profile.preset());
        assertEquals(34, profile.durationTicks());
    }

    @Test
    void providesSafeDefaultsForEveryGameplayCategory() {
        assertEquals("smoke_joint", ConsumptionAnimationProfile.defaultPreset("cannabis"));
        assertEquals("inject_arm", ConsumptionAnimationProfile.defaultPreset("opioid"));
        assertEquals("ritual_sway", ConsumptionAnimationProfile.defaultPreset("psychedelic"));
        assertEquals("swallow_pill", ConsumptionAnimationProfile.defaultPreset("unknown"));
    }
}
