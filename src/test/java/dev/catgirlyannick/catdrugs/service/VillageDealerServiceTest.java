package dev.catgirlyannick.catdrugs.service;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageDealerServiceTest {
    @Test
    void selectsExactlyFiveUniqueTradesWithoutChangingTheSourceList() {
        List<String> available = new ArrayList<>();
        for (int index = 0; index < 40; index++) {
            available.add("drug_" + index);
        }
        List<String> original = List.copyOf(available);

        List<String> selected = VillageDealerService.selectTradeIds(available, 5, new Random(42L));

        assertEquals(5, selected.size());
        assertEquals(5, new HashSet<>(selected).size());
        assertTrue(available.containsAll(selected));
        assertEquals(original, available);
    }

    @Test
    void returnsAllTradesWhenFewerThanTheConfiguredLimitExist() {
        List<String> selected = VillageDealerService.selectTradeIds(
                List.of("weed", "meth", "lsd"), 5, new Random(7L));

        assertEquals(3, selected.size());
        assertEquals(3, new HashSet<>(selected).size());
    }

    @Test
    void recognizesOnlyVanillaVillageStructureKeys() {
        assertTrue(VillageDealerService.isVillageStructureKey(
                NamespacedKey.minecraft("village_plains")));
        assertTrue(VillageDealerService.isVillageStructureKey(
                NamespacedKey.minecraft("village_savanna")));
        assertTrue(!VillageDealerService.isVillageStructureKey(
                NamespacedKey.minecraft("pillager_outpost")));
        assertTrue(!VillageDealerService.isVillageStructureKey(
                new NamespacedKey("custom", "village_plains")));
    }
}
