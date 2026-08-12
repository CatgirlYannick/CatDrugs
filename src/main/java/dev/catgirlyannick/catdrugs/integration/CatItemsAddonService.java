package dev.catgirlyannick.catdrugs.integration;

import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class CatItemsAddonService {
    private static final String EMBEDDED_ASSETS = "embedded-catitems/assets/";
    private static final List<UtilityItem> UTILITY_ITEMS = List.of(
            new UtilityItem("dealer_token", "EMERALD", "<green>Dealer Token</green>", "catdrugs.admin.dealer"),
            new UtilityItem("alchemy_cauldron", "CAULDRON", "<dark_purple>Alchemy Cauldron</dark_purple>", "catdrugs.admin"),
            new UtilityItem("antidote", "HONEY_BOTTLE", "<gold>Stabilizer</gold>", "catdrugs.admin"),
            new UtilityItem("empty_pouch", "RABBIT_HIDE", "<gray>Empty Pouch</gray>", "catdrugs.admin")
    );

    private final JavaPlugin plugin;
    private final CatItemsBridge bridge;

    public CatItemsAddonService(JavaPlugin plugin, CatItemsBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    public InstallResult install(Collection<DrugDefinition> definitions, boolean force) {
        Plugin catItems = plugin.getServer().getPluginManager().getPlugin("CatItems");
        if (catItems == null || !catItems.isEnabled()) {
            return new InstallResult(false, 0, 0, List.of(), "CatItems is not active");
        }
        try {
            Path dataRoot = catItems.getDataFolder().toPath().toAbsolutePath().normalize();
            Files.createDirectories(dataRoot.resolve("items"));
            File itemFile = dataRoot.resolve("items/catdrugs.yml").toFile();
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(itemFile);
            yaml.set("config-version", 1);
            yaml.set("namespace", "catdrugs");
            int itemsWritten = 0;
            int assetsWritten = 0;
            List<String> conflicts = new ArrayList<>();

            for (DrugDefinition definition : definitions) {
                if (definition.customItemId().isBlank()) {
                    continue;
                }
                String localId = definition.customItemId().substring(definition.customItemId().indexOf(':') + 1);
                String root = "items." + localId;
                if (!yaml.isConfigurationSection(root) || force) {
                    yaml.set(root + ".enabled", definition.enabled());
                    yaml.set(root + ".material", definition.fallbackMaterial().name());
                    yaml.set(root + ".display-name", definition.displayName());
                    yaml.set(root + ".lore", definition.lore());
                    yaml.set(root + ".custom-model-data", "auto");
                    yaml.set(root + ".item-model", definition.customItemId());
                    yaml.set(root + ".texture", "catdrugs:item/" + localId);
                    yaml.set(root + ".permission", "catdrugs.use");
                    yaml.set(root + ".glint", false);
                    itemsWritten++;
                }
                CopyResult copied = copyAsset(dataRoot,
                        "catdrugs/textures/item/" + localId + ".png", force);
                assetsWritten += copied.written() ? 1 : 0;
                if (copied.conflict()) conflicts.add(localId + ".png");
            }
            for (UtilityItem utility : UTILITY_ITEMS) {
                String root = "items." + utility.id();
                if (!yaml.isConfigurationSection(root) || force) {
                    yaml.set(root + ".enabled", true);
                    yaml.set(root + ".material", utility.material());
                    yaml.set(root + ".display-name", utility.displayName());
                    yaml.set(root + ".lore", List.of("<gray>CatDrugs utility item</gray>"));
                    yaml.set(root + ".custom-model-data", "auto");
                    yaml.set(root + ".item-model", "catdrugs:" + utility.id());
                    yaml.set(root + ".texture", "catdrugs:item/" + utility.id());
                    yaml.set(root + ".permission", utility.permission());
                    yaml.set(root + ".glint", false);
                    itemsWritten++;
                }
                CopyResult copied = copyAsset(dataRoot,
                        "catdrugs/textures/item/" + utility.id() + ".png", force);
                assetsWritten += copied.written() ? 1 : 0;
                if (copied.conflict()) conflicts.add(utility.id() + ".png");
            }
            CopyResult villager = copyAsset(dataRoot,
                    "minecraft/textures/entity/villager/profession/nitwit.png", force);
            assetsWritten += villager.written() ? 1 : 0;
            if (villager.conflict()) conflicts.add("minecraft/.../nitwit.png");
            yaml.save(itemFile);
            return new InstallResult(true, itemsWritten, assetsWritten, List.copyOf(conflicts), "");
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().warning("The CatItems addon could not be installed: " + exception.getMessage());
            return new InstallResult(true, 0, 0, List.of(), exception.getMessage());
        }
    }

    public boolean rebuild() {
        return bridge.available() && plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(), "catitems reload");
    }

    public String status(Collection<DrugDefinition> definitions) {
        return bridge.status(definitions);
    }

    private CopyResult copyAsset(Path dataRoot, String relative, boolean force) throws IOException {
        String resource = EMBEDDED_ASSETS + relative;
        byte[] source;
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) {
                throw new IOException("Embedded asset is missing: " + resource);
            }
            source = input.readAllBytes();
        }
        Path assetRoot = dataRoot.resolve("pack/assets").normalize();
        Path destination = assetRoot.resolve(relative).normalize();
        if (!destination.startsWith(assetRoot)) {
            throw new IOException("Unsafe asset path: " + relative);
        }
        if (Files.exists(destination)) {
            if (java.util.Arrays.equals(source, Files.readAllBytes(destination))) {
                return new CopyResult(false, false);
            }
            if (!force) {
                return new CopyResult(false, true);
            }
        }
        Files.createDirectories(Objects.requireNonNull(destination.getParent()));
        try (InputStream input = plugin.getResource(resource)) {
            Files.copy(Objects.requireNonNull(input), destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return new CopyResult(true, false);
    }

    private record CopyResult(boolean written, boolean conflict) {
    }

    private record UtilityItem(String id, String material, String displayName, String permission) {
    }

    public record InstallResult(boolean catItemsAvailable, int itemsWritten, int assetsWritten,
                                List<String> conflicts, String error) {
        public boolean changed() {
            return itemsWritten > 0 || assetsWritten > 0;
        }

        public boolean successful() {
            return catItemsAvailable && error.isBlank();
        }
    }
}
