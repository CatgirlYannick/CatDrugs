package dev.catgirlyannick.catdrugs.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

public final class ConfigManager {
    private static final List<String> FILES = List.of("config.yml", "messages.yml", "drugs.yml", "survival.yml",
            "gui.yml", "realistic-effects.yml");

    private final JavaPlugin plugin;
    private YamlConfiguration main;
    private YamlConfiguration messages;
    private YamlConfiguration survival;
    private YamlConfiguration gui;
    private YamlConfiguration realisticEffects;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean load() {
        plugin.getDataFolder().mkdirs();
        for (String file : FILES) {
            if (!new File(plugin.getDataFolder(), file).exists()) {
                plugin.saveResource(file, false);
            }
        }
        main = load("config.yml");
        messages = load("messages.yml");
        survival = load("survival.yml");
        gui = load("gui.yml");
        realisticEffects = load("realistic-effects.yml");
        return validateVersion("config.yml", main)
                & validateVersion("messages.yml", messages)
                & validateVersion("survival.yml", survival)
                & validateVersion("gui.yml", gui)
                & validateVersion("realistic-effects.yml", realisticEffects)
                & validateMain()
                & validateGui();
    }

    private YamlConfiguration load(String file) {
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), file));
        try (InputStream stream = plugin.getResource(file)) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                ConfigurationDefaults.mergeMissing(loaded, defaults);
            }
        } catch (java.io.IOException exception) {
            plugin.getLogger().warning("Could not load bundled defaults for " + file + ": " + exception.getMessage());
        }
        return loaded;
    }

    private boolean validateVersion(String name, YamlConfiguration config) {
        int version = config.getInt("config-version", -1);
        if (version == 1) {
            return true;
        }
        plugin.getLogger().severe(name + ": unknown config-version " + version + "; expected 1.");
        return false;
    }

    private boolean validateMain() {
        int window = main.getInt("overdose.window-seconds", 300);
        int threshold = main.getInt("overdose.threshold-points", 10);
        double damage = main.getDouble("overdose.damage", 6.0);
        if (window < 10 || window > 3600 || threshold < 1 || threshold > 1000 || damage < 0.0 || damage > 100.0) {
            plugin.getLogger().severe("config.yml: overdose values are outside the allowed ranges "
                    + "(window 10..3600, threshold 1..1000, damage 0..100).");
            return false;
        }
        return true;
    }

    private boolean validateGui() {
        int size = gui.getInt("catalog.size", 54);
        List<Integer> slots = gui.getIntegerList("catalog.content-slots");
        if (size < 9 || size > 54 || size % 9 != 0) {
            plugin.getLogger().severe("gui.yml -> catalog.size must be between 9 and 54 and divisible by 9: " + size);
            return false;
        }
        if (new HashSet<>(slots).size() != slots.size()) {
            plugin.getLogger().severe("gui.yml -> catalog.content-slots contains duplicate slots.");
            return false;
        }
        Integer invalid = slots.stream().filter(slot -> slot < 0 || slot >= size).findFirst().orElse(null);
        if (invalid != null) {
            plugin.getLogger().severe("gui.yml -> catalog.content-slots contains invalid slot " + invalid
                    + "; allowed values are 0 through " + (size - 1) + ".");
            return false;
        }
        return true;
    }

    public YamlConfiguration main() {
        return main;
    }

    public YamlConfiguration messages() {
        return messages;
    }

    public YamlConfiguration survival() {
        return survival;
    }

    public YamlConfiguration gui() {
        return gui;
    }

    public YamlConfiguration realisticEffects() {
        return realisticEffects;
    }
}
