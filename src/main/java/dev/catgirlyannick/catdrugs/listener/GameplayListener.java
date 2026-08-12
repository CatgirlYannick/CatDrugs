package dev.catgirlyannick.catdrugs.listener;

import dev.catgirlyannick.catdrugs.item.DrugItemFactory;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import dev.catgirlyannick.catdrugs.service.ConsumptionService;
import dev.catgirlyannick.catdrugs.service.AdvancedGameplayService;
import dev.catgirlyannick.catdrugs.service.MessageService;
import dev.catgirlyannick.catdrugs.service.VillageDealerService;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Locale;
import java.util.Map;

public final class GameplayListener implements Listener {
    private final JavaPlugin plugin;
    private final DrugItemFactory items;
    private final ConsumptionService consumption;
    private final VillageDealerService dealers;
    private final AdvancedGameplayService advancedGameplay;
    private final MessageService messages;
    private YamlConfiguration config;

    public GameplayListener(JavaPlugin plugin, DrugItemFactory items, ConsumptionService consumption,
                            VillageDealerService dealers, AdvancedGameplayService advancedGameplay,
                            MessageService messages, YamlConfiguration config) {
        this.plugin = plugin;
        this.items = items;
        this.consumption = consumption;
        this.dealers = dealers;
        this.advancedGameplay = advancedGameplay;
        this.messages = messages;
        this.config = config;
    }

    public void reload(YamlConfiguration config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!isProtectedInteraction(event.getAction())) {
            return;
        }
        ItemStack held = event.getItem();
        if (held == null || held.getType().isAir()) {
            held = event.getHand() == EquipmentSlot.OFF_HAND
                    ? event.getPlayer().getInventory().getItemInOffHand()
                    : event.getPlayer().getInventory().getItemInMainHand();
        }
        DrugDefinition drug = items.identify(held).orElse(null);
        if (drug == null) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!event.getPlayer().hasPermission("catdrugs.use")) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "errors.no-permission");
            return;
        }
        if (advancedGameplay.tryLabProcessing(event.getPlayer(), drug, held)) {
            event.setCancelled(true);
            return;
        }
        if (!drug.consumable()) {
            String preparedId = config.getString("utensils.preparation-required." + drug.id(), "");
            if (!preparedId.isBlank()) {
                event.setCancelled(true);
                messages.send(event.getPlayer(), "errors.preparation-required", Map.of(
                        "drug", plainId(drug.id()), "result", plainId(preparedId)));
            }
            return;
        }
        event.setCancelled(true);
        String required = requiredUtensil(config, drug);
        if (!required.isBlank()) {
            ItemStack offHand = event.getPlayer().getInventory().getItemInOffHand();
            String present = items.identify(offHand).map(DrugDefinition::id).orElse("");
            if (!present.equals(required)) {
                String displayName = items.definition(required).map(DrugDefinition::displayName)
                        .map(GameplayListener::plainName).orElse(plainId(required));
                messages.send(event.getPlayer(), "errors.utensil-required", Map.of(
                        "drug", plainName(drug.displayName()), "utensil", displayName));
                return;
            }
        }
        consumption.consume(event.getPlayer(), drug,
                () -> consumeOne(event.getPlayer(), drug.id()));
    }

    static boolean isConsumptionInteraction(Action action, EquipmentSlot hand) {
        return hand == EquipmentSlot.HAND
                && isProtectedInteraction(action);
    }

    static boolean isProtectedInteraction(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (items.identify(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
        }
    }

    static String requiredUtensil(YamlConfiguration config, DrugDefinition drug) {
        String overridePath = "utensils.overrides." + drug.id();
        if (config.contains(overridePath)) {
            return config.getString(overridePath, "").toLowerCase(Locale.ROOT);
        }
        return config.getString("utensils.category-defaults." + drug.category(), "")
                .toLowerCase(Locale.ROOT);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Villager villager) {
            plugin.getServer().getScheduler().runTask(plugin, () -> dealers.consider(villager));
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> dealers.scanChunk(event.getChunk()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoot(LootGenerateEvent event) {
        if (!config.getBoolean("features.loot.enabled", true) || event.getLootTable() == null) {
            return;
        }
        String key = event.getLootTable().getKey().toString();
        ConfigurationSection tables = config.getConfigurationSection("loot.tables");
        if (tables == null) {
            return;
        }
        for (String id : tables.getKeys(false)) {
            ConfigurationSection entry = tables.getConfigurationSection(id);
            if (entry == null || !key.contains(entry.getString("table-contains", id))) {
                continue;
            }
            double chance = Math.max(0.0, Math.min(entry.getDouble("chance", 0.1), 1.0));
            if (ThreadLocalRandom.current().nextDouble() <= chance) {
                items.create(entry.getString("item", "hemp_seed"), randomAmount(entry)).ifPresent(event.getLoot()::add);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        if (!config.getBoolean("features.cultivation.enabled", true)
                || event.getBlock().getType() != Material.WHEAT
                || !(event.getBlock().getBlockData() instanceof Ageable ageable)
                || ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }
        double chance = Math.max(0.0, Math.min(config.getDouble("cultivation.mature-wheat-drop-chance", 0.18), 1.0));
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            items.create("dried_herb", 1).ifPresent(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));
        }
        if (ThreadLocalRandom.current().nextDouble() <= chance / 2.0) {
            items.create("hemp_seed", 1).ifPresent(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        consumption.clear(event.getPlayer());
    }

    private int randomAmount(ConfigurationSection entry) {
        int min = Math.max(1, entry.getInt("min-amount", 1));
        int max = Math.max(min, Math.min(entry.getInt("max-amount", min), 64));
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private boolean consumeOne(org.bukkit.entity.Player player, String drugId) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (matches(mainHand, drugId)) {
            decrement(mainHand);
            player.getInventory().setItemInMainHand(mainHand.getAmount() <= 0 ? null : mainHand);
            return true;
        }
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack candidate = storage[slot];
            if (!matches(candidate, drugId)) {
                continue;
            }
            decrement(candidate);
            player.getInventory().setItem(slot, candidate.getAmount() <= 0 ? null : candidate);
            return true;
        }
        return false;
    }

    private boolean matches(ItemStack stack, String drugId) {
        return stack != null && !stack.getType().isAir()
                && items.identify(stack).map(DrugDefinition::id).filter(drugId::equals).isPresent();
    }

    private static void decrement(ItemStack stack) {
        stack.setAmount(Math.max(0, stack.getAmount() - 1));
    }

    private static String plainName(String miniMessage) {
        return miniMessage.replaceAll("<[^>]+>", "");
    }

    private static String plainId(String id) {
        String spaced = id.replace('_', ' ').replace('-', ' ');
        return spaced.isBlank() ? id : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
