package dev.catgirlyannick.catdrugs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginContractTest {
    private static final List<String> EXPANSION_050 = List.of(
            "cannabis_oil", "edible", "shatter", "wax", "peyote", "ibogaine", "psilocybin",
            "amanita", "morning_glory", "fly_agaric", "ecstasy", "molly", "bath_salts", "flakka",
            "alpha_pvp", "mdpv", "bk_mdma", "dexamphetamine", "lisdexamfetamine", "modafinil",
            "caffeine", "nicotine", "alcohol", "absinthe", "moonshine", "chloroform", "butane",
            "glue", "aerosol", "amyl_nitrite", "hydrocodone", "hydromorphone", "methadone",
            "buprenorphine", "tramadol", "tapentadol", "carfentanil", "sufentanil", "lorazepam",
            "temazepam", "midazolam", "phenobarbital", "quaaludes", "pregabalin", "gabapentin",
            "desomorphine", "etizolam", "phenibut", "coca_leaf", "opium");

    @Test
    void pluginDescriptorSupportsLowestRequestedApiAndOptionalCatItems() {
        YamlConfiguration plugin = loadResource("plugin.yml");
        assertEquals("CatDrugs", plugin.getString("name"));
        assertEquals("1.21", plugin.getString("api-version"));
        assertTrue(plugin.getStringList("softdepend").contains("CatItems"));
        assertTrue(plugin.getStringList("softdepend").contains("packetevents"));
        assertNotNull(plugin.getConfigurationSection("commands.catdrugs"));
    }

    @Test
    void allStandardSubstancesHaveUniqueCatItemsIdsAndEffects() {
        YamlConfiguration drugs = loadResource("drugs.yml");
        ConfigurationSection root = drugs.getConfigurationSection("drugs");
        assertNotNull(root);
        List<String> required = List.of("weed", "hashish", "cocaine", "meth", "heroin",
                "mdma", "lsd", "mushrooms", "ketamine", "opioids", "crack", "amphetamine",
                "fentanyl", "oxycodone", "ghb", "pcp", "dmt", "salvia", "spice", "nitrous",
                "mescaline", "ayahuasca", "kratom", "kava", "poppers", "ether", "lean",
                "morphine", "codeine", "diazepam", "alprazolam", "zolpidem", "two_cb", "two_ci",
                "cathinone", "mephedrone", "methylphenidate", "clonazepam", "barbiturates", "dxm");
        assertTrue(root.getKeys(false).containsAll(required));
        assertTrue(root.getKeys(false).containsAll(EXPANSION_050));
        List<String> consumables = root.getKeys(false).stream()
                .filter(id -> root.getBoolean(id + ".consumable")).toList();
        assertEquals(90, consumables.size());
        assertEquals(consumables.size(), consumables.stream()
                .map(id -> root.getString(id + ".item.catitems-id"))
                .distinct().count());
        consumables.forEach(id -> {
            assertTrue(root.getBoolean(id + ".consumable"), id);
            assertTrue(!root.getMapList(id + ".effects.immediate").isEmpty(), id);
            assertTrue(root.getInt(id + ".consumption.dose-points") > 0, id);
        });
    }

    @Test
    void generatedTexturesHaveExpectedDimensionsAndAlpha() throws Exception {
        Path base = Path.of("catitems-addon", "assets", "catdrugs", "textures", "item");
        YamlConfiguration drugs = loadResource("drugs.yml");
        ConfigurationSection definitions = drugs.getConfigurationSection("drugs");
        assertNotNull(definitions);
        for (String id : definitions.getKeys(false)) {
            BufferedImage image = ImageIO.read(base.resolve(id + ".png").toFile());
            assertNotNull(image, id);
            assertTrue(image.getWidth() == 32 || image.getWidth() == 64, id);
            assertEquals(image.getWidth(), image.getHeight(), id);
            assertTrue(image.getColorModel().hasAlpha(), id);
        }
        File overlayFile = Path.of("catitems-addon", "assets", "minecraft", "textures",
                "entity", "villager", "profession", "nitwit.png").toFile();
        BufferedImage overlay = ImageIO.read(overlayFile);
        assertEquals(64, overlay.getWidth());
        assertEquals(64, overlay.getHeight());
        assertTrue(overlay.getColorModel().hasAlpha());
    }

    @Test
    void embeddedCatItemsAddonContainsEveryRuntimeTexture() {
        YamlConfiguration drugs = loadResource("drugs.yml");
        ConfigurationSection definitions = drugs.getConfigurationSection("drugs");
        assertNotNull(definitions);
        definitions.getKeys(false).forEach(id -> {
            String resource = "embedded-catitems/assets/catdrugs/textures/item/" + id + ".png";
            assertNotNull(getClass().getClassLoader().getResource(resource), resource);
        });
    }

    @Test
    void everyConsumableIsCoveredByAutomaticDealerTradesAndFictionalRecipes() {
        YamlConfiguration drugs = loadResource("drugs.yml");
        YamlConfiguration survival = loadResource("survival.yml");
        ConfigurationSection definitions = drugs.getConfigurationSection("drugs");
        assertNotNull(definitions);
        assertEquals(90, definitions.getKeys(false).stream()
                .filter(id -> definitions.getBoolean(id + ".consumable")).count());
        assertTrue(survival.getBoolean("automatic-content.dealers.include-all-drugs"));
        assertTrue(survival.getBoolean("automatic-content.recipes.enabled"));
    }

    @Test
    void catItemsAddonDefaultsProtectAdminChanges() {
        YamlConfiguration config = loadResource("config.yml");
        assertTrue(config.getBoolean("catitems.addon.auto-install"));
        assertTrue(config.getBoolean("catitems.addon.rebuild-after-install"));
        assertTrue(!config.getBoolean("catitems.addon.overwrite-customized-files"));
    }

    @Test
    void dealersUsePersistentNpcStyleAndFiveRandomTrades() {
        YamlConfiguration survival = loadResource("survival.yml");
        assertEquals(5, survival.getInt("village-dealers.trades-per-dealer"));
        assertTrue(survival.getBoolean("village-dealers.natural-village-spawning.enabled"));
        assertTrue(survival.getBoolean("village-dealers.natural-village-spawning.ai-enabled"));
        assertTrue(survival.getBoolean("village-dealers.npc.stationary"));
        assertTrue(survival.getBoolean("village-dealers.npc.invulnerable"));
        assertTrue(survival.getBoolean("village-dealers.npc.silent"));
        assertTrue(survival.getBoolean("village-dealers.npc.no-collision"));
        assertEquals("SWAMP", survival.getString("village-dealers.npc.villager-type"));
    }

    @Test
    void rawWeedRequiresJointPreparationAndEveryUtensilHasARecipe() {
        YamlConfiguration drugs = loadResource("drugs.yml");
        YamlConfiguration survival = loadResource("survival.yml");
        assertTrue(!drugs.getBoolean("drugs.weed.consumable"));
        assertTrue(drugs.getBoolean("drugs.joint.consumable"));
        assertEquals("joint", survival.getString("utensils.preparation-required.weed"));
        assertEquals("lighter", survival.getString("utensils.overrides.joint"));
        for (String id : List.of("rolling_paper", "lighter", "herbal_pipe", "filter_straw",
                "medicine_cup", "ritual_bowl", "sterile_applicator", "vaporizer", "joint")) {
            assertNotNull(drugs.getConfigurationSection("drugs." + id), id);
            assertTrue(survival.getBoolean("recipes." + id + ".enabled"), id);
            assertTrue(!survival.getStringList("recipes." + id + ".ingredients").isEmpty(), id);
        }
        assertEquals(105, java.util.Objects.requireNonNull(drugs.getConfigurationSection("drugs")).getKeys(false).size());
    }

    @Test
    void realisticEffectsCoverEveryConsumableCategoryAndUseCustomSymptoms() {
        YamlConfiguration drugs = loadResource("drugs.yml");
        YamlConfiguration effects = loadResource("realistic-effects.yml");
        YamlConfiguration messages = loadResource("messages.yml");
        ConfigurationSection definitions = drugs.getConfigurationSection("drugs");
        ConfigurationSection profiles = effects.getConfigurationSection("profiles");
        assertNotNull(definitions);
        assertNotNull(profiles);
        assertTrue(effects.getBoolean("enabled"));
        assertTrue(effects.getBoolean("replace-legacy-effects"));
        assertEquals(2, effects.getInt("config-version"));
        List<String> advancedCustomEffects = List.of("camera_drift", "visual_echo",
                "auditory_distortion", "focus_pulse", "time_distortion", "muscle_tension");
        List<String> configuredCustomEffects = profiles.getKeys(false).stream()
                .map(profile -> profiles.getConfigurationSection(profile + ".phases"))
                .filter(java.util.Objects::nonNull)
                .flatMap(phases -> phases.getKeys(false).stream()
                        .flatMap(phase -> phases.getMapList(phase + ".custom-effects").stream()))
                .map(value -> String.valueOf(value.get("type")))
                .toList();
        assertTrue(configuredCustomEffects.containsAll(advancedCustomEffects), configuredCustomEffects::toString);
        definitions.getKeys(false).stream()
                .filter(id -> definitions.getBoolean(id + ".consumable"))
                .forEach(id -> {
                    String profile = effects.getString("drug-overrides." + id,
                            definitions.getString(id + ".category"));
                    ConfigurationSection phases = profiles.getConfigurationSection(profile + ".phases");
                    assertNotNull(phases, id + " -> " + profile);
                    assertTrue(phases.getKeys(false).size() >= 3, id);
                    assertTrue(phases.getKeys(false).stream().anyMatch(phase ->
                            !phases.getMapList(phase + ".custom-effects").isEmpty()), id);
                    phases.getKeys(false).forEach(phase -> assertNotNull(
                            messages.getString("consumption.phases." + phase),
                            "Missing phase message: " + phase));
                });
    }

    @Test
    void doseReactionsEscalateToConfiguredSafeBlackout() {
        YamlConfiguration config = loadResource("config.yml");
        assertTrue(config.getBoolean("dose-reactions.enabled"));
        int nausea = config.getInt("dose-reactions.nausea-threshold-points");
        int vomiting = config.getInt("dose-reactions.vomiting-threshold-points");
        int blackout = config.getInt("dose-reactions.blackout-threshold-points");
        assertTrue(nausea < vomiting);
        assertTrue(vomiting < blackout);
        assertTrue(config.getInt("dose-reactions.blackout-radius-blocks") <= 150);
        assertTrue(config.getInt("dose-reactions.blackout-duration-seconds") >= 3);
        assertTrue(config.getInt("dose-reactions.vomiting.bursts") >= 3);
        assertTrue(config.getInt("dose-reactions.vomiting.interval-ticks") >= 4);
        assertTrue(config.getInt("dose-reactions.vomiting.particle-count") >= 10);
        assertTrue(config.getDouble("dose-reactions.vomiting.movement-retention") > 0.0);
        assertTrue(config.getDouble("dose-reactions.vomiting.movement-retention") < 1.0);
        YamlConfiguration messages = loadResource("messages.yml");
        assertNotNull(messages.getString("consumption.reactions.vomiting"));
        assertNotNull(messages.getString("consumption.reactions.blackout"));
        assertNotNull(messages.getString("consumption.reactions.wake-up"));
    }

    @Test
    void consumptionAnimationsUseCatItemsCompatibleProfiles() {
        YamlConfiguration config = loadResource("config.yml");
        assertTrue(config.getBoolean("consumption-animations.enabled"));
        for (String preset : List.of("smoke_joint", "smoke_pipe", "smoke_stimulant", "snort_line",
                "drink_bottle", "eat_edible", "inhale_vape", "inject_arm", "ritual_sway", "swallow_pill")) {
            assertTrue(config.getInt("consumption-animations.duration-by-preset." + preset) >= 8, preset);
        }
        assertEquals(2, config.getInt("consumption-animations.schema-version"));
        assertEquals("smoke_joint", config.getString("consumption-animations.drug-overrides.joint"));
        assertEquals("smoke_stimulant", config.getString("consumption-animations.drug-overrides.meth"));
        assertEquals("drink_bottle", config.getString("consumption-animations.drug-overrides.alcohol"));
        assertEquals("inhale_vape", config.getString("consumption-animations.category-presets.inhalant"));
    }

    @Test
    void advancedGameplaySystemsAreEnabledAndHaveRequiredItems() {
        YamlConfiguration drugs = loadResource("drugs.yml");
        YamlConfiguration survival = loadResource("survival.yml");
        YamlConfiguration messages = loadResource("messages.yml");
        for (String id : List.of("antidote", "alchemy_cauldron", "dealer_token", "empty_pouch")) {
            assertNotNull(drugs.getConfigurationSection("drugs." + id), id);
        }
        for (String feature : List.of("progression", "withdrawal", "mixing", "perception", "lab",
                "dealer-quests", "enforcement", "random-events")) {
            assertTrue(survival.getBoolean("advanced-gameplay." + feature + ".enabled"), feature);
        }
        assertEquals("alchemy_cauldron", survival.getString("advanced-gameplay.lab.station-item"));
        assertNotNull(messages.getString("advanced.withdrawal"));
        assertNotNull(messages.getString("advanced.mix-hazardous"));
        assertNotNull(messages.getString("advanced.quest-complete"));
        assertNotNull(messages.getString("advanced.rescue-target"));
    }

    private YamlConfiguration loadResource(String name) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                java.util.Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(name)),
                StandardCharsets.UTF_8));
    }
}
