package dev.catgirlyannick.catdrugs.config;

import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import dev.catgirlyannick.catdrugs.model.EffectDefinition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class DrugRegistry {
    private final JavaPlugin plugin;
    private final Map<String, DrugDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, DrugDefinition> byCustomItemId = new LinkedHashMap<>();

    public DrugRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean reload() {
        definitions.clear();
        byCustomItemId.clear();
        File file = new File(plugin.getDataFolder(), "drugs.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try (InputStream stream = plugin.getResource("drugs.yml")) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                ConfigurationDefaults.mergeMissing(config, defaults);
            }
        } catch (java.io.IOException exception) {
            plugin.getLogger().warning("Could not load bundled drug defaults: " + exception.getMessage());
        }
        int version = config.getInt("config-version", -1);
        if (version != 1) {
            plugin.getLogger().severe("drugs.yml: unknown config-version " + version + "; expected 1.");
            return false;
        }
        ConfigurationSection root = config.getConfigurationSection("drugs");
        if (root == null) {
            plugin.getLogger().severe("drugs.yml: required 'drugs' section is missing.");
            return false;
        }
        boolean valid = true;
        for (String id : root.getKeys(false)) {
            try {
                DrugDefinition definition = parse(id, root.getConfigurationSection(id));
                if (definitions.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("duplicate ID");
                }
                if (!definition.customItemId().isBlank()) {
                    DrugDefinition replaced = byCustomItemId.putIfAbsent(definition.customItemId(), definition);
                    if (replaced != null) {
                        throw new IllegalArgumentException("CatItems ID is already used by " + replaced.id());
                    }
                }
            } catch (IllegalArgumentException exception) {
                valid = false;
                plugin.getLogger().severe("drugs.yml -> drugs." + id + ": " + exception.getMessage());
            }
        }
        if (definitions.isEmpty()) {
            plugin.getLogger().severe("drugs.yml does not contain any valid definitions.");
            return false;
        }
        return valid;
    }

    private DrugDefinition parse(String rawId, ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Definition is not a YAML object");
        }
        String id = rawId.toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("ID may only contain a-z, 0-9, _, and -");
        }
        Material material = Material.matchMaterial(section.getString("item.fallback-material", "PAPER"));
        if (material == null || material.isAir()) {
            throw new IllegalArgumentException("invalid material at item.fallback-material");
        }
        String customItemId = section.getString("item.catitems-id",
                section.getString("item.itemsadder-id", "")).toLowerCase(Locale.ROOT);
        if (!customItemId.isBlank() && !customItemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("invalid CatItems ID: " + customItemId);
        }
        int cooldown = bounded(section.getInt("consumption.cooldown-seconds", 5), 0, 3600,
                "consumption.cooldown-seconds");
        int points = bounded(section.getInt("consumption.dose-points", 1), 0, 100,
                "consumption.dose-points");
        int delay = bounded(section.getInt("effects.after-delay-ticks", 0), 0, 72000,
                "effects.after-delay-ticks");
        return new DrugDefinition(
                id,
                section.getBoolean("enabled", true),
                section.getBoolean("consumable", true),
                section.getString("category", "drug"),
                section.getString("display-name", id),
                List.copyOf(section.getStringList("lore")),
                material,
                customItemId,
                cooldown,
                points,
                parseEffects(section.getMapList("effects.immediate"), id, "immediate"),
                delay,
                parseEffects(section.getMapList("effects.after"), id, "after")
        );
    }

    private List<EffectDefinition> parseEffects(List<Map<?, ?>> values, String id, String path) {
        List<EffectDefinition> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            Map<?, ?> value = values.get(index);
            Object rawType = value.containsKey("type") ? value.get("type") : "";
            String key = String.valueOf(rawType).toLowerCase(Locale.ROOT);
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(key));
            if (type == null) {
                throw new IllegalArgumentException("unknown effect at effects." + path + "[" + index + "]: " + key);
            }
            int seconds = number(value.get("duration-seconds"), 1);
            int amplifier = number(value.get("amplifier"), 0);
            if (seconds < 1 || seconds > 3600 || amplifier < 0 || amplifier > 10) {
                throw new IllegalArgumentException("effect outside the allowed range at " + id + ".effects." + path + "[" + index + "]");
            }
            result.add(new EffectDefinition(type, seconds * 20, amplifier,
                    bool(value.get("ambient"), false), bool(value.get("particles"), true), bool(value.get("icon"), true)));
        }
        return List.copyOf(result);
    }

    private static int bounded(int value, int min, int max, String path) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(path + " must be between " + min + " and " + max);
        }
        return value;
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean bool ? bool : fallback;
    }

    public Optional<DrugDefinition> find(String id) {
        return Optional.ofNullable(definitions.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<DrugDefinition> findByCustomItemId(String id) {
        return Optional.ofNullable(byCustomItemId.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<DrugDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public Collection<DrugDefinition> consumables() {
        return definitions.values().stream().filter(DrugDefinition::consumable).toList();
    }
}
