package dev.catgirlyannick.catdrugs.service;

import dev.catgirlyannick.catdrugs.config.DrugRegistry;
import dev.catgirlyannick.catdrugs.item.DrugItemFactory;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class VillageDealerService {
    private final JavaPlugin plugin;
    private final DrugRegistry registry;
    private final DrugItemFactory items;
    private final MessageService messages;
    private final NamespacedKey dealerKey;
    private final NamespacedKey tradeSelectionKey;
    private final NamespacedKey villageSpawnKey;
    private final NamespacedKey naturalDealerKey;
    private final NamespacedKey dealerRevisionKey;
    private YamlConfiguration config;
    private List<String> cachedTradeIds = List.of();
    private Set<String> cachedTradeIdSet = Set.of();
    private Set<String> allowedWorlds = Set.of();
    private List<String> allowedWorldPrefixes = List.of();
    private boolean allWorldsAllowed = true;
    private int dealerRevision;

    public VillageDealerService(JavaPlugin plugin, DrugRegistry registry, DrugItemFactory items,
                                MessageService messages, YamlConfiguration config) {
        this.plugin = plugin;
        this.registry = registry;
        this.items = items;
        this.messages = messages;
        this.config = config;
        this.dealerKey = new NamespacedKey(plugin, "village_dealer");
        this.tradeSelectionKey = new NamespacedKey(plugin, "village_dealer_trades");
        this.villageSpawnKey = new NamespacedKey(plugin, "natural_village_dealer_spawned");
        this.naturalDealerKey = new NamespacedKey(plugin, "natural_village_dealer");
        this.dealerRevisionKey = new NamespacedKey(plugin, "village_dealer_revision");
        rebuildRuntimeCache();
    }

    public void reload(YamlConfiguration config) {
        this.config = config;
        rebuildRuntimeCache();
    }

    public void scanChunk(Chunk chunk) {
        if (!enabled() || !worldAllowed(chunk.getWorld().getName())) {
            return;
        }
        boolean villageHandled = ensureNaturalVillageDealers(chunk);
        int max = Math.max(1, Math.min(config.getInt("village-dealers.max-per-chunk", 1), 10));
        int present = 0;
        List<Villager> candidates = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Villager villager) {
                if (isDealer(villager)) {
                    if (!isCurrent(villager)) {
                        refresh(villager);
                    }
                    present++;
                } else if (villager.getProfession() == Villager.Profession.NITWIT
                        || config.getBoolean("village-dealers.allow-converting-employed-villagers", false)) {
                    candidates.add(villager);
                }
            }
        }
        if (villageHandled) {
            return;
        }
        for (Villager candidate : candidates) {
            if (present >= max) {
                break;
            }
            if (ThreadLocalRandom.current().nextDouble() <= spawnChance()) {
                makeDealer(candidate);
                present++;
            }
        }
    }

    public void consider(Villager villager) {
        if (!enabled() || isDealer(villager) || !worldAllowed(villager.getWorld().getName())) {
            return;
        }
        if (ensureNaturalVillageDealers(villager.getChunk())) {
            return;
        }
        if (villager.getProfession() != Villager.Profession.NITWIT
                && !config.getBoolean("village-dealers.allow-converting-employed-villagers", false)) {
            return;
        }
        long existing = java.util.Arrays.stream(villager.getChunk().getEntities())
                .filter(entity -> entity instanceof Villager other && isDealer(other)).count();
        int max = Math.max(1, Math.min(config.getInt("village-dealers.max-per-chunk", 1), 10));
        if (existing < max && ThreadLocalRandom.current().nextDouble() <= spawnChance()) {
            makeDealer(villager);
        }
    }

    private boolean ensureNaturalVillageDealers(Chunk chunk) {
        if (!config.getBoolean("village-dealers.natural-village-spawning.enabled", true)) {
            return false;
        }
        boolean villageFound = false;
        for (GeneratedStructure structure : chunk.getStructures()) {
            if (!isVillageStructureKey(structure.getStructure().getKey())) {
                continue;
            }
            villageFound = true;
            ensureNaturalVillageDealer(chunk, structure);
        }
        return villageFound;
    }

    private void ensureNaturalVillageDealer(Chunk sourceChunk, GeneratedStructure structure) {
        World world = sourceChunk.getWorld();
        BoundingBox bounds = structure.getBoundingBox();
        Villager existingDealer = world.getNearbyEntities(bounds, entity ->
                        entity instanceof Villager villager && isDealer(villager)).stream()
                .map(Villager.class::cast)
                .findFirst()
                .orElse(null);
        if (existingDealer != null) {
            markNaturalDealer(existingDealer);
            structure.getPersistentDataContainer().set(villageSpawnKey, PersistentDataType.BYTE, (byte) 1);
            return;
        }
        if (structure.getPersistentDataContainer().has(villageSpawnKey, PersistentDataType.BYTE)) {
            return;
        }

        Location spawnLocation = world.getNearbyEntities(bounds, Villager.class::isInstance).stream()
                .map(Villager.class::cast)
                .map(Villager::getLocation)
                .findFirst()
                .orElseGet(() -> fallbackVillageLocation(sourceChunk, bounds));
        if (spawnLocation == null) {
            return;
        }
        try {
            Villager dealer = world.spawn(spawnLocation, Villager.class);
            dealer.getPersistentDataContainer().set(naturalDealerKey, PersistentDataType.BYTE, (byte) 1);
            makeDealer(dealer);
            structure.getPersistentDataContainer().set(villageSpawnKey, PersistentDataType.BYTE, (byte) 1);
            plugin.getLogger().info("Spawned a village pharmacist in generated village at "
                    + spawnLocation.getBlockX() + ", " + spawnLocation.getBlockY() + ", "
                    + spawnLocation.getBlockZ() + " in " + world.getName() + ".");
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Could not spawn a village pharmacist at generated village in "
                    + world.getName() + ": " + exception.getMessage());
        }
    }

    private void markNaturalDealer(Villager villager) {
        boolean newlyMarked = !villager.getPersistentDataContainer()
                .has(naturalDealerKey, PersistentDataType.BYTE);
        if (newlyMarked) {
            villager.getPersistentDataContainer().set(naturalDealerKey, PersistentDataType.BYTE, (byte) 1);
        }
        boolean aiEnabled = config.getBoolean(
                "village-dealers.natural-village-spawning.ai-enabled", true);
        if (newlyMarked || villager.hasAI() != aiEnabled) {
            refresh(villager);
        }
    }

    private Location fallbackVillageLocation(Chunk chunk, BoundingBox bounds) {
        int chunkMinX = chunk.getX() << 4;
        int chunkMinZ = chunk.getZ() << 4;
        int minX = Math.max(chunkMinX, (int) Math.ceil(bounds.getMinX()));
        int maxX = Math.min(chunkMinX + 15, (int) Math.floor(bounds.getMaxX()));
        int minZ = Math.max(chunkMinZ, (int) Math.ceil(bounds.getMinZ()));
        int maxZ = Math.min(chunkMinZ + 15, (int) Math.floor(bounds.getMaxZ()));
        if (minX > maxX || minZ > maxZ) {
            return null;
        }
        int x = minX + (maxX - minX) / 2;
        int z = minZ + (maxZ - minZ) / 2;
        return chunk.getWorld().getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES)
                .getLocation().add(0.5, 1.0, 0.5);
    }

    static boolean isVillageStructureKey(NamespacedKey key) {
        return key != null && key.getNamespace().equals(NamespacedKey.MINECRAFT)
                && key.getKey().startsWith("village_");
    }

    public void makeDealer(Villager villager) {
        villager.getPersistentDataContainer().set(dealerKey, PersistentDataType.BYTE, (byte) 1);
        villager.getPersistentDataContainer().remove(tradeSelectionKey);
        refresh(villager);
    }

    public void refresh(Villager villager) {
        applyNpcStyle(villager);
        List<MerchantRecipe> trades = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("village-dealers.trades");
        if (section == null) {
            return;
        }
        List<String> enabledIds = cachedTradeIds;
        int configuredCount = Math.max(1, Math.min(config.getInt("village-dealers.trades-per-dealer", 5), 10));
        int expectedCount = Math.min(configuredCount, enabledIds.size());
        List<String> selectedIds = storedTradeIds(villager).stream()
                .filter(cachedTradeIdSet::contains)
                .distinct()
                .toList();
        if (selectedIds.size() != expectedCount) {
            selectedIds = selectTradeIds(enabledIds, expectedCount,
                    new Random(ThreadLocalRandom.current().nextLong()));
            villager.getPersistentDataContainer().set(tradeSelectionKey, PersistentDataType.STRING,
                    String.join(",", selectedIds));
        }
        for (String id : selectedIds) {
            ConfigurationSection trade = section.getConfigurationSection(id);
            DrugDefinition definition = registry.find(id).orElse(null);
            String resultId = trade == null ? id : trade.getString("item", id);
            int amount = trade == null ? 1 : Math.max(1, trade.getInt("amount", 1));
            items.create(resultId, amount).ifPresent(result -> {
                int defaultCost = definition == null ? 5 : Math.max(4, Math.min(20, 4 + definition.dosePoints() * 2));
                int emeralds = Math.max(1, Math.min(trade == null ? defaultCost
                        : trade.getInt("emerald-cost", defaultCost), 64));
                int maxUses = Math.max(1, Math.min(trade == null ? 8 : trade.getInt("max-uses", 8), 1000));
                int xp = trade == null ? Math.max(5, definition == null ? 5 : definition.dosePoints() + 4)
                        : Math.max(0, trade.getInt("villager-xp", 5));
                MerchantRecipe recipe = new MerchantRecipe(result, 0, maxUses, true,
                        xp, 0.05f);
                recipe.addIngredient(new ItemStack(org.bukkit.Material.EMERALD, emeralds));
                trades.add(recipe);
            });
        }
        villager.setRecipes(trades);
        villager.getPersistentDataContainer().set(dealerRevisionKey, PersistentDataType.INTEGER, dealerRevision);
    }

    private List<String> enabledTradeIds(ConfigurationSection explicitTrades) {
        List<String> ids = new ArrayList<>();
        if (config.getBoolean("automatic-content.dealers.include-all-drugs", true)) {
            registry.consumables().stream()
                    .filter(DrugDefinition::enabled)
                    .map(DrugDefinition::id)
                    .filter(id -> explicitTrades.getConfigurationSection(id) == null
                            || explicitTrades.getBoolean(id + ".enabled", true))
                    .forEach(ids::add);
            explicitTrades.getKeys(false).stream()
                    .filter(id -> explicitTrades.getBoolean(id + ".enabled", true))
                    .filter(id -> !ids.contains(id))
                    .filter(id -> registry.find(id).filter(DrugDefinition::enabled).isPresent())
                    .forEach(ids::add);
        } else {
            explicitTrades.getKeys(false).stream()
                    .filter(id -> explicitTrades.getBoolean(id + ".enabled", true))
                    .forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    private void applyNpcStyle(Villager villager) {
        villager.setProfession(Villager.Profession.NITWIT);
        setVillagerType(villager);
        villager.setVillagerLevel(5);
        villager.setVillagerExperience(250);
        villager.customName(messages.raw(config.getString("village-dealers.display-name", "<light_purple>Traveling Pharmacist</light_purple>")));
        villager.setCustomNameVisible(config.getBoolean("village-dealers.show-name", true));
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setInvulnerable(config.getBoolean("village-dealers.npc.invulnerable", true));
        villager.setSilent(config.getBoolean("village-dealers.npc.silent", true));
        villager.setCollidable(!config.getBoolean("village-dealers.npc.no-collision", true));
        boolean naturalDealer = villager.getPersistentDataContainer()
                .has(naturalDealerKey, PersistentDataType.BYTE);
        villager.setAI(naturalDealer
                ? config.getBoolean("village-dealers.natural-village-spawning.ai-enabled", true)
                : !config.getBoolean("village-dealers.npc.stationary", true));
    }

    static List<String> selectTradeIds(List<String> available, int count, Random random) {
        List<String> shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled.subList(0, Math.min(Math.max(0, count), shuffled.size())));
    }

    private List<String> storedTradeIds(Villager villager) {
        String stored = villager.getPersistentDataContainer().get(tradeSelectionKey, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private void setVillagerType(Villager villager) {
        String configured = config.getString("village-dealers.npc.villager-type", "SWAMP");
        try {
            villager.setVillagerType(Villager.Type.valueOf(configured.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unknown village dealer type '" + configured + "'; keeping "
                    + villager.getVillagerType() + ".");
        }
    }

    public boolean isDealer(Villager villager) {
        return villager.getPersistentDataContainer().has(dealerKey, PersistentDataType.BYTE);
    }

    public int countLoadedDealers() {
        return plugin.getServer().getWorlds().stream().flatMap(world -> world.getEntitiesByClass(Villager.class).stream())
                .mapToInt(villager -> isDealer(villager) ? 1 : 0).sum();
    }

    private boolean enabled() {
        return config.getBoolean("features.village-dealers.enabled", true);
    }

    private double spawnChance() {
        return Math.max(0.0, Math.min(config.getDouble("village-dealers.conversion-chance", 0.35), 1.0));
    }

    private boolean worldAllowed(String name) {
        if (allWorldsAllowed) {
            return true;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return allowedWorlds.contains(lower)
                || allowedWorldPrefixes.stream().anyMatch(lower::startsWith);
    }

    private boolean isCurrent(Villager villager) {
        return !villager.getRecipes().isEmpty()
                && villager.getPersistentDataContainer().getOrDefault(
                dealerRevisionKey, PersistentDataType.INTEGER, Integer.MIN_VALUE) == dealerRevision;
    }

    private void rebuildRuntimeCache() {
        ConfigurationSection trades = config.getConfigurationSection("village-dealers.trades");
        cachedTradeIds = trades == null ? List.of() : enabledTradeIds(trades);
        cachedTradeIdSet = Set.copyOf(cachedTradeIds);

        Set<String> exact = new HashSet<>();
        List<String> prefixes = new ArrayList<>();
        List<String> configuredWorlds = config.getStringList("village-dealers.allowed-worlds");
        allWorldsAllowed = configuredWorlds.isEmpty();
        for (String configured : configuredWorlds) {
            String pattern = configured.toLowerCase(Locale.ROOT);
            if (pattern.equals("*")) {
                allWorldsAllowed = true;
                exact.clear();
                prefixes.clear();
                break;
            }
            if (pattern.endsWith("*")) {
                prefixes.add(pattern.substring(0, pattern.length() - 1));
            } else {
                exact.add(pattern);
            }
        }
        allowedWorlds = Set.copyOf(exact);
        allowedWorldPrefixes = List.copyOf(prefixes);
        dealerRevision = config.getValues(true).hashCode();
    }
}
