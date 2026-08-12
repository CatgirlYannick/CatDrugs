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
        config.set("consumption-animations.category-presets.stimulant", "snort");
        config.set("consumption-animations.drug-overrides.meth", "smoke");
        config.set("consumption-animations.duration-by-preset.smoke", 44);
        DrugDefinition meth = new DrugDefinition("meth", true, true, "stimulant", "Meth", List.of(),
                Material.SUGAR, "catdrugs:meth", 7, 4, List.of(), 0, List.of());

        ConsumptionAnimationProfile profile = ConsumptionAnimationProfile.resolve(config, meth);

        assertEquals("smoke", profile.preset());
        assertEquals(44, profile.durationTicks());
    }

    @Test
    void providesSafeDefaultsForEveryGameplayCategory() {
        assertEquals("smoke", ConsumptionAnimationProfile.defaultPreset("cannabis"));
        assertEquals("inject", ConsumptionAnimationProfile.defaultPreset("opioid"));
        assertEquals("ritual", ConsumptionAnimationProfile.defaultPreset("psychedelic"));
        assertEquals("swallow", ConsumptionAnimationProfile.defaultPreset("unknown"));
    }
}
