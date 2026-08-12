package dev.catgirlyannick.catdrugs.listener;

import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameplayListenerContractTest {
    @Test
    void acceptsMainHandRightClickInAirAndOnBlocks() {
        assertTrue(GameplayListener.isConsumptionInteraction(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        assertTrue(GameplayListener.isConsumptionInteraction(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.HAND));
    }

    @Test
    void rejectsOffHandAndNonUseActions() {
        assertFalse(GameplayListener.isConsumptionInteraction(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND));
        assertFalse(GameplayListener.isConsumptionInteraction(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND));
        assertFalse(GameplayListener.isConsumptionInteraction(Action.PHYSICAL, EquipmentSlot.HAND));
        assertTrue(GameplayListener.isProtectedInteraction(Action.RIGHT_CLICK_BLOCK));
        assertFalse(GameplayListener.isProtectedInteraction(Action.LEFT_CLICK_BLOCK));
    }

    @Test
    void resolvesJointOverrideBeforeCategoryDefault() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("utensils.category-defaults.cannabis", "herbal_pipe");
        config.set("utensils.overrides.joint", "lighter");
        assertEquals("lighter", GameplayListener.requiredUtensil(config, definition("joint", "cannabis")));
        assertEquals("herbal_pipe", GameplayListener.requiredUtensil(config, definition("hashish", "cannabis")));
    }

    private DrugDefinition definition(String id, String category) {
        return new DrugDefinition(id, true, true, category, id, java.util.List.of(), Material.PAPER,
                "catdrugs:" + id, 1, 1, java.util.List.of(), 0, java.util.List.of());
    }
}
