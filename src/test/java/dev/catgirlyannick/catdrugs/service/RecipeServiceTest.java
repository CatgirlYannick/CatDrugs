package dev.catgirlyannick.catdrugs.service;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeServiceTest {
    @Test
    void automaticRecipesAreFictionalThreeIngredientMinecraftRecipes() {
        List<Material> ingredients = RecipeService.automaticIngredients("cannabis_oil", Material.HONEY_BOTTLE);
        assertEquals(3, ingredients.size());
        assertEquals(Material.HONEY_BOTTLE, ingredients.getFirst());
        assertTrue(ingredients.stream().noneMatch(material -> material == Material.AIR));
    }

    @Test
    void differentIdsProduceDifferentAutomaticCombinations() {
        assertNotEquals(
                RecipeService.automaticIngredients("cannabis_oil", Material.HONEY_BOTTLE),
                RecipeService.automaticIngredients("opium", Material.BLACK_DYE));
    }
}
