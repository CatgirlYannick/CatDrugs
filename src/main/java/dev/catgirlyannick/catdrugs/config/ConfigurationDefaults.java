package dev.catgirlyannick.catdrugs.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Adds missing bundled leaf values in memory while preserving every explicit admin value. */
final class ConfigurationDefaults {
    private ConfigurationDefaults() {
    }

    static void mergeMissing(YamlConfiguration target, YamlConfiguration defaults) {
        for (String path : defaults.getKeys(true)) {
            Object value = defaults.get(path);
            if (value instanceof ConfigurationSection || target.contains(path, true)) {
                continue;
            }
            target.set(path, value);
        }
        target.setDefaults(defaults);
    }
}
