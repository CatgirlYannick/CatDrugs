package dev.catgirlyannick.catdrugs.service;

import dev.catgirlyannick.catdrugs.config.DrugRegistry;
import dev.catgirlyannick.catdrugs.item.DrugItemFactory;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RecipeService {
    private final JavaPlugin plugin;
    private final DrugRegistry registry;
    private final DrugItemFactory items;
    private final List<NamespacedKey> registered = new ArrayList<>();

    public RecipeService(JavaPlugin plugin, DrugRegistry registry, DrugItemFactory items) {
        this.plugin = plugin;
        this.registry = registry;
        this.items = items;
    }

    public int reload(YamlConfiguration config) {
        registered.forEach(Bukkit::removeRecipe);
        registered.clear();
        if (!config.getBoolean("features.crafting.enabled", true)) {
            return 0;
        }
        ConfigurationSection recipes = config.getConfigurationSection("recipes");
        if (recipes == null) {
            plugin.getLogger().warning("survival.yml: the 'recipes' section is missing; crafting is disabled.");
            return 0;
        }
        Set<String> explicitResults = new HashSet<>();
        for (String recipeId : recipes.getKeys(false)) {
            ConfigurationSection recipe = recipes.getConfigurationSection(recipeId);
            if (recipe == null || !recipe.getBoolean("enabled", true)) {
                continue;
            }
            try {
                register(recipeId, recipe);
                explicitResults.add(recipe.getString("result", "").toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("survival.yml -> recipes." + recipeId + ": " + exception.getMessage());
            }
        }
        if (config.getBoolean("automatic-content.recipes.enabled", true)) {
            registry.consumables().stream()
                    .filter(DrugDefinition::enabled)
                    .filter(drug -> !explicitResults.contains(drug.id()))
                    .forEach(this::registerAutomatic);
        }
        return registered.size();
    }

    private void registerAutomatic(DrugDefinition drug) {
        ItemStack result = items.create(drug.id(), 1)
                .orElseThrow(() -> new IllegalArgumentException("unknown result: " + drug.id()));
        NamespacedKey key = new NamespacedKey(plugin, "craft_auto_" + drug.id());
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (Material ingredient : automaticIngredients(drug.id(), drug.fallbackMaterial())) {
            recipe.addIngredient(ingredient);
        }
        if (Bukkit.addRecipe(recipe)) {
            registered.add(key);
        } else {
            plugin.getLogger().warning("Automatic recipe could not be registered: " + key);
        }
    }

    static List<Material> automaticIngredients(String id, Material fallback) {
        Material[] catalysts = {
                Material.AMETHYST_SHARD, Material.GLOWSTONE_DUST, Material.REDSTONE,
                Material.QUARTZ, Material.PRISMARINE_CRYSTALS, Material.BLAZE_POWDER,
                Material.ECHO_SHARD, Material.PHANTOM_MEMBRANE, Material.ENDER_PEARL,
                Material.GHAST_TEAR, Material.SLIME_BALL, Material.HONEYCOMB
        };
        Material[] binders = {
                Material.PAPER, Material.GLASS_BOTTLE, Material.BOWL, Material.RABBIT_HIDE,
                Material.STRING, Material.CLAY_BALL, Material.WHEAT, Material.SUGAR
        };
        int hash = id.hashCode() & Integer.MAX_VALUE;
        return List.of(fallback, catalysts[hash % catalysts.length],
                binders[(hash / catalysts.length) % binders.length]);
    }

    private void register(String recipeId, ConfigurationSection section) {
        String resultId = section.getString("result", "");
        int amount = Math.max(1, Math.min(section.getInt("amount", 1), 64));
        ItemStack result = items.create(resultId, amount)
                .orElseThrow(() -> new IllegalArgumentException("unknown result: " + resultId));
        List<String> ingredients = section.getStringList("ingredients");
        if (ingredients.isEmpty() || ingredients.size() > 9) {
            throw new IllegalArgumentException("ingredients must contain 1 through 9 entries");
        }
        NamespacedKey key = new NamespacedKey(plugin, "craft_" + recipeId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_/-]", "_"));
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (String ingredient : ingredients) {
            if (ingredient.startsWith("catdrugs:")) {
                String customId = ingredient.substring("catdrugs:".length());
                ItemStack exact = items.create(customId, 1)
                        .orElseThrow(() -> new IllegalArgumentException("unknown CatDrugs ingredient: " + ingredient));
                recipe.addIngredient(new RecipeChoice.ExactChoice(exact));
            } else {
                String materialName = ingredient.startsWith("minecraft:")
                        ? ingredient.substring("minecraft:".length()) : ingredient;
                Material material = Material.matchMaterial(materialName);
                if (material == null || material.isAir()) {
                    throw new IllegalArgumentException("unknown vanilla ingredient: " + ingredient);
                }
                recipe.addIngredient(material);
            }
        }
        if (Bukkit.addRecipe(recipe)) {
            registered.add(key);
        } else {
            throw new IllegalArgumentException("Recipe could not be registered: " + key);
        }
    }
}
