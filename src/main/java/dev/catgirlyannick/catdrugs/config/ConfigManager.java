package dev.catgirlyannick.catdrugs.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ConfigManager {
    private static final List<String> FILES = List.of("config.yml", "messages.yml", "drugs.yml", "survival.yml",
            "gui.yml", "realistic-effects.yml");
    private static final Set<String> PERCEPTION_EFFECTS = Set.of(
            "camera_drift", "visual_echo", "auditory_distortion",
            "focus_pulse", "time_distortion", "muscle_tension");

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
        File target = new File(plugin.getDataFolder(), file);
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(target);
        boolean migratedAnimations = "config.yml".equals(file)
                && loaded.getInt("consumption-animations.schema-version", 1) < 2;
        boolean migratedPerceptionEffects = "realistic-effects.yml".equals(file)
                && loaded.getInt("config-version", 1) < 2;
        if (migratedAnimations) {
            File backup = new File(plugin.getDataFolder(), "config-before-body-emotes.yml");
            try {
                if (!backup.isFile()) {
                    Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                }
                loaded.set("consumption-animations", null);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Could not back up config.yml before the body-emote migration", exception);
            }
        }
        if (migratedPerceptionEffects) {
            File backup = new File(plugin.getDataFolder(), "realistic-effects-before-custom-perception.yml");
            try {
                if (!backup.isFile()) {
                    Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                }
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(
                        "Could not back up realistic-effects.yml before the perception-effect migration", exception);
            }
        }
        try (InputStream stream = plugin.getResource(file)) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                ConfigurationDefaults.mergeMissing(loaded, defaults);
                if (migratedPerceptionEffects) {
                    mergePerceptionEffects(loaded, defaults);
                }
            }
            if (migratedAnimations) {
                loaded.save(target);
                plugin.getLogger().info("Upgraded consumption animation mappings; previous config saved as "
                        + "config-before-body-emotes.yml.");
            }
            if (migratedPerceptionEffects) {
                loaded.set("config-version", 2);
                loaded.save(target);
                plugin.getLogger().info("Added the new custom perception effects; previous settings saved as "
                        + "realistic-effects-before-custom-perception.yml.");
            }
        } catch (java.io.IOException exception) {
            plugin.getLogger().warning("Could not load bundled defaults for " + file + ": " + exception.getMessage());
        }
        return loaded;
    }

    static void mergePerceptionEffects(YamlConfiguration loaded, YamlConfiguration defaults) {
        var profiles = defaults.getConfigurationSection("profiles");
        if (profiles == null) {
            return;
        }
        for (String profile : profiles.getKeys(false)) {
            var phases = defaults.getConfigurationSection("profiles." + profile + ".phases");
            if (phases == null) {
                continue;
            }
            for (String phase : phases.getKeys(false)) {
                String path = "profiles." + profile + ".phases." + phase + ".custom-effects";
                List<Map<?, ?>> merged = new ArrayList<>(loaded.getMapList(path));
                Set<String> present = new HashSet<>();
                for (Map<?, ?> entry : merged) {
                    present.add(String.valueOf(entry.get("type")).toLowerCase(Locale.ROOT));
                }
                for (Map<?, ?> entry : defaults.getMapList(path)) {
                    String type = String.valueOf(entry.get("type")).toLowerCase(Locale.ROOT);
                    if (PERCEPTION_EFFECTS.contains(type) && present.add(type)) {
                        Map<String, Object> copy = new LinkedHashMap<>();
                        entry.forEach((key, value) -> copy.put(String.valueOf(key), value));
                        merged.add(copy);
                    }
                }
                loaded.set(path, merged);
            }
        }
    }

    private boolean validateVersion(String name, YamlConfiguration config) {
        int version = config.getInt("config-version", -1);
        int expected = "realistic-effects.yml".equals(name) ? 2 : 1;
        if (version == expected) {
            return true;
        }
        plugin.getLogger().severe(name + ": unknown config-version " + version + "; expected " + expected + ".");
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
