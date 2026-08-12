package dev.catgirlyannick.catdrugs.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
    @Test
    void perceptionMigrationPreservesExistingEntriesAndAddsOnlyMissingNewTypes() {
        YamlConfiguration current = new YamlConfiguration();
        current.set("profiles.test.phases.peak.custom-effects", List.of(
                Map.of("type", "heartbeat", "intensity", 9),
                Map.of("type", "camera_drift", "intensity", 7)));

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("profiles.test.phases.peak.custom-effects", List.of(
                Map.of("type", "heartbeat", "intensity", 1),
                Map.of("type", "camera_drift", "intensity", 2),
                Map.of("type", "visual_echo", "intensity", 3)));

        ConfigManager.mergePerceptionEffects(current, defaults);

        List<Map<?, ?>> merged = current.getMapList("profiles.test.phases.peak.custom-effects");
        assertEquals(3, merged.size());
        assertEquals(9, merged.getFirst().get("intensity"));
        assertEquals(7, merged.get(1).get("intensity"));
        assertTrue(merged.stream().anyMatch(entry -> "visual_echo".equals(entry.get("type"))));
    }
}
