package dev.catgirlyannick.catdrugs.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationMergeTest {

    @Test
    void bundledNestedDefaultsJoinLegacyKeysWithoutReplacingOverrides() {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("recipes.weed.enabled", false);
        YamlConfiguration bundled = new YamlConfiguration();
        bundled.set("recipes.weed.enabled", true);
        bundled.set("recipes.antidote.enabled", true);
        bundled.set("recipes.antidote.result", "antidote");
        bundled.set("drugs.antidote.consumable", false);
        bundled.set("advanced-gameplay.lab.enabled", true);

        ConfigurationDefaults.mergeMissing(legacy, bundled);

        ConfigurationSection recipes = legacy.getConfigurationSection("recipes");
        assertTrue(recipes.getKeys(false).contains("weed"));
        assertTrue(recipes.getKeys(false).contains("antidote"));
        assertEquals(false, legacy.getBoolean("recipes.weed.enabled"));
        assertEquals("antidote", legacy.getString("recipes.antidote.result"));
        assertEquals(false, legacy.getBoolean("drugs.antidote.consumable"));
        assertTrue(legacy.getBoolean("advanced-gameplay.lab.enabled"));
    }
}
