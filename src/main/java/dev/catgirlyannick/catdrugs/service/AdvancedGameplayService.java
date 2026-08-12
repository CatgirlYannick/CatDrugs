package dev.catgirlyannick.catdrugs.service;

import dev.catgirlyannick.catdrugs.item.DrugItemFactory;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Own gameplay layer for progression, fictional lab processing, dealer quests,
 * enforcement encounters, perception events, mixed-category reactions and rescue.
 */
public final class AdvancedGameplayService implements Listener {
    private static final Set<String> DOWNERS = Set.of("depressant", "opioid", "sedative", "inhalant");
    private static final Set<String> UPPERS = Set.of("stimulant", "party");

    private final JavaPlugin plugin;
    private final DrugItemFactory items;
    private final VillageDealerService dealers;
    private final MessageService messages;
    private final NamespacedKey addictionKey;
    private final NamespacedKey toleranceKey;
    private final NamespacedKey reputationKey;
    private final NamespacedKey lastUseKey;
    private final NamespacedKey lastCategoryKey;
    private final NamespacedKey lastCategoryAtKey;
    private final NamespacedKey withdrawalAtKey;
    private final NamespacedKey questDrugKey;
    private final NamespacedKey questProgressKey;
    private final NamespacedKey questRequiredKey;
    private final Set<UUID> labJobs = new HashSet<>();
    private final Set<BukkitTask> tasks = new HashSet<>();
    private ConsumptionService consumption;
    private YamlConfiguration config;

    public AdvancedGameplayService(JavaPlugin plugin, DrugItemFactory items, VillageDealerService dealers,
                                   MessageService messages, YamlConfiguration config) {
        this.plugin = plugin;
        this.items = items;
        this.dealers = dealers;
        this.messages = messages;
        this.config = config;
        addictionKey = key("addiction");
        toleranceKey = key("tolerance");
        reputationKey = key("dealer_reputation");
        lastUseKey = key("last_use");
        lastCategoryKey = key("last_category");
        lastCategoryAtKey = key("last_category_at");
        withdrawalAtKey = key("withdrawal_at");
        questDrugKey = key("quest_drug");
        questProgressKey = key("quest_progress");
        questRequiredKey = key("quest_required");
        startWithdrawalTask();
    }

    public void bindConsumption(ConsumptionService consumption) {
        this.consumption = consumption;
    }

    public void reload(YamlConfiguration config) {
        this.config = config;
    }

    public ConsumptionModifiers onConsumption(Player player, DrugDefinition drug) {
        if (!config.getBoolean("advanced-gameplay.progression.enabled", true)) {
            return new ConsumptionModifiers(1.0, 0);
        }
        long now = System.currentTimeMillis();
        PersistentDataContainer data = player.getPersistentDataContainer();
        int addiction = nextLevel(integer(data, addictionKey), addictionGain(drug.dosePoints()));
        int tolerance = nextLevel(integer(data, toleranceKey), toleranceGain(drug.dosePoints()));
        data.set(addictionKey, PersistentDataType.INTEGER, addiction);
        data.set(toleranceKey, PersistentDataType.INTEGER, tolerance);
        data.set(lastUseKey, PersistentDataType.LONG, now);

        String previousCategory = data.getOrDefault(lastCategoryKey, PersistentDataType.STRING, "");
        long previousAt = data.getOrDefault(lastCategoryAtKey, PersistentDataType.LONG, 0L);
        int mixWindow = bounded(config.getInt("advanced-gameplay.mixing.window-seconds", 120), 10, 900);
        boolean mixed = !previousCategory.isBlank() && !previousCategory.equalsIgnoreCase(drug.category())
                && now - previousAt <= mixWindow * 1000L;
        int extraDose = 0;
        if (mixed && config.getBoolean("advanced-gameplay.mixing.enabled", true)) {
            boolean hazardous = isHazardousMix(previousCategory, drug.category());
            extraDose = hazardous
                    ? bounded(config.getInt("advanced-gameplay.mixing.hazardous-extra-dose", 4), 0, 20)
                    : bounded(config.getInt("advanced-gameplay.mixing.standard-extra-dose", 2), 0, 20);
            applyMixReaction(player, hazardous, previousCategory, drug.category());
        }
        data.set(lastCategoryKey, PersistentDataType.STRING, drug.category().toLowerCase(Locale.ROOT));
        data.set(lastCategoryAtKey, PersistentDataType.LONG, now);

        double effectiveness = effectiveness(tolerance,
                config.getDouble("advanced-gameplay.progression.minimum-effectiveness", 0.45));
        if (tolerance >= config.getInt("advanced-gameplay.progression.tolerance-message-level", 25)) {
            messages.send(player, "advanced.tolerance", Map.of(
                    "level", Integer.toString(tolerance),
                    "percent", Integer.toString((int) Math.round(effectiveness * 100))));
        }
        maybeStartPerceptionEvent(player, drug, tolerance);
        maybeStartEnforcement(player, addiction);
        maybeGiveRandomEvent(player);
        return new ConsumptionModifiers(effectiveness, extraDose);
    }

    public boolean tryLabProcessing(Player player, DrugDefinition input, ItemStack held) {
        if (!config.getBoolean("advanced-gameplay.lab.enabled", true) || labJobs.contains(player.getUniqueId())) {
            return false;
        }
        String output = config.getString("advanced-gameplay.lab.processes." + input.id(), "");
        if (output.isBlank()) {
            return false;
        }
        String stationId = config.getString("advanced-gameplay.lab.station-item", "alchemy_cauldron");
        String offhandId = items.identify(player.getInventory().getItemInOffHand())
                .map(DrugDefinition::id).orElse("");
        if (!stationId.equals(offhandId)) {
            if (input.consumable()) {
                return false;
            }
            messages.send(player, "advanced.lab-required", Map.of("station", plainId(stationId)));
            return true;
        }
        if (items.definition(output).isEmpty()) {
            plugin.getLogger().warning("Unknown lab output configured for " + input.id() + ": " + output);
            return true;
        }
        consumeOne(player, held);
        labJobs.add(player.getUniqueId());
        messages.send(player, "advanced.lab-started", Map.of(
                "input", plainId(input.id()), "output", plainId(output)));
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.1f);
        int delay = bounded(config.getInt("advanced-gameplay.lab.processing-seconds", 5), 1, 60) * 20;
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tasks.remove(holder[0]);
            labJobs.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            items.create(output, 1).ifPresent(result -> player.getInventory().addItem(result).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left)));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0),
                    14, 0.4, 0.5, 0.4, 0.02);
            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.5f);
            messages.send(player, "advanced.lab-complete", Map.of("output", plainId(output)));
        }, delay);
        tasks.add(holder[0]);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getRightClicked() instanceof Player target && player.isSneaking()) {
            DrugDefinition held = items.identify(player.getInventory().getItemInMainHand()).orElse(null);
            if (held != null && held.id().equals("antidote")) {
                event.setCancelled(true);
                rescue(player, target);
            }
            return;
        }
        if (event.getRightClicked() instanceof Villager villager && dealers.isDealer(villager)
                && config.getBoolean("advanced-gameplay.dealer-quests.enabled", true)) {
            DrugDefinition held = items.identify(player.getInventory().getItemInMainHand()).orElse(null);
            if (held != null && held.id().equals("dealer_token")) {
                event.setCancelled(true);
                redeemDealerToken(player);
                return;
            }
            showOrAssignQuest(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDealerPurchase(PlayerPurchaseEvent event) {
        if (!(event.getPlayer().getOpenInventory().getTopInventory() instanceof MerchantInventory inventory)
                || !(inventory.getMerchant() instanceof Villager villager) || !dealers.isDealer(villager)) {
            return;
        }
        Player player = event.getPlayer();
        PersistentDataContainer data = player.getPersistentDataContainer();
        int reputation = Math.min(1000, integer(data, reputationKey) + 1);
        data.set(reputationKey, PersistentDataType.INTEGER, reputation);
        String target = data.getOrDefault(questDrugKey, PersistentDataType.STRING, "");
        String purchased = items.identify(event.getTrade().getResult()).map(DrugDefinition::id).orElse("");
        if (!target.isBlank() && target.equals(purchased)) {
            int progress = integer(data, questProgressKey) + event.getTrade().getResult().getAmount();
            int required = Math.max(1, integer(data, questRequiredKey));
            if (progress >= required) {
                completeQuest(player, target, reputation);
            } else {
                data.set(questProgressKey, PersistentDataType.INTEGER, progress);
                messages.send(player, "advanced.quest-progress", Map.of(
                        "drug", plainId(target), "progress", Integer.toString(progress),
                        "required", Integer.toString(required)));
            }
        }
    }

    public void showProfile(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        messages.send(player, "advanced.profile", Map.of(
                "addiction", Integer.toString(integer(data, addictionKey)),
                "tolerance", Integer.toString(integer(data, toleranceKey)),
                "reputation", Integer.toString(integer(data, reputationKey))));
    }

    public void showQuest(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        String target = data.getOrDefault(questDrugKey, PersistentDataType.STRING, "");
        if (target.isBlank()) {
            messages.send(player, "advanced.quest-none");
            return;
        }
        messages.send(player, "advanced.quest-status", Map.of(
                "drug", plainId(target),
                "progress", Integer.toString(integer(data, questProgressKey)),
                "required", Integer.toString(integer(data, questRequiredKey))));
    }

    public void reset(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        for (NamespacedKey key : List.of(addictionKey, toleranceKey, reputationKey, lastUseKey, lastCategoryKey,
                lastCategoryAtKey, withdrawalAtKey, questDrugKey, questProgressKey, questRequiredKey)) {
            data.remove(key);
        }
        if (consumption != null) {
            consumption.stabilize(player);
        }
    }

    public void shutdown() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        labJobs.clear();
    }

    private void rescue(Player helper, Player target) {
        if (consumption == null || consumption.currentDose(target) <= 0) {
            messages.send(helper, "advanced.rescue-not-needed", Map.of("player", target.getName()));
            return;
        }
        consumeOne(helper, helper.getInventory().getItemInMainHand());
        consumption.stabilize(target);
        target.setHealth(Math.min(target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(),
                target.getHealth() + 6.0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1, false, true, true));
        target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1, 0), 8,
                0.4, 0.5, 0.4, 0.02);
        target.playSound(target.getLocation(), Sound.ITEM_TOTEM_USE, 0.7f, 1.2f);
        PersistentDataContainer helperData = helper.getPersistentDataContainer();
        helperData.set(reputationKey, PersistentDataType.INTEGER,
                Math.min(1000, integer(helperData, reputationKey) + 3));
        messages.send(helper, "advanced.rescue-helper", Map.of("player", target.getName()));
        messages.send(target, "advanced.rescue-target", Map.of("player", helper.getName()));
    }

    private void showOrAssignQuest(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (!data.getOrDefault(questDrugKey, PersistentDataType.STRING, "").isBlank()) {
            showQuest(player);
            return;
        }
        ConfigurationSection pool = config.getConfigurationSection("advanced-gameplay.dealer-quests.targets");
        List<String> targetIds = pool == null ? List.of("weed", "joint", "mushrooms", "caffeine")
                : new ArrayList<>(pool.getKeys(false));
        List<String> valid = targetIds.stream().filter(id -> items.definition(id).map(DrugDefinition::consumable)
                .orElse(false)).toList();
        if (valid.isEmpty()) {
            return;
        }
        String target = valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
        int min = bounded(config.getInt("advanced-gameplay.dealer-quests.minimum-trades", 3), 1, 32);
        int max = bounded(config.getInt("advanced-gameplay.dealer-quests.maximum-trades", 6), min, 64);
        int required = ThreadLocalRandom.current().nextInt(min, max + 1);
        data.set(questDrugKey, PersistentDataType.STRING, target);
        data.set(questProgressKey, PersistentDataType.INTEGER, 0);
        data.set(questRequiredKey, PersistentDataType.INTEGER, required);
        messages.send(player, "advanced.quest-assigned", Map.of(
                "drug", plainId(target), "required", Integer.toString(required)));
    }

    private void completeQuest(Player player, String target, int reputation) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        int reward = bounded(config.getInt("advanced-gameplay.dealer-quests.token-reward", 2), 1, 16);
        items.create("dealer_token", reward).ifPresent(item -> player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left)));
        data.set(reputationKey, PersistentDataType.INTEGER, Math.min(1000, reputation + 10));
        data.remove(questDrugKey);
        data.remove(questProgressKey);
        data.remove(questRequiredKey);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
        messages.send(player, "advanced.quest-complete", Map.of(
                "drug", plainId(target), "reward", Integer.toString(reward)));
    }

    private void redeemDealerToken(Player player) {
        List<String> rewards = config.getStringList("advanced-gameplay.dealer-quests.token-rewards");
        if (rewards.isEmpty()) {
            rewards = List.of("joint", "mushrooms", "caffeine", "kratom", "kava");
        }
        List<String> valid = rewards.stream().filter(id -> items.definition(id)
                .map(definition -> definition.enabled() && definition.consumable()).orElse(false)).toList();
        if (valid.isEmpty()) {
            return;
        }
        consumeOne(player, player.getInventory().getItemInMainHand());
        String reward = valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
        items.create(reward, 1).ifPresent(item -> player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left)));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.8f, 1.25f);
        messages.send(player, "advanced.token-redeemed", Map.of("item", plainId(reward)));
    }

    private void startWithdrawalTask() {
        long interval = bounded(config.getInt("advanced-gameplay.withdrawal.check-seconds", 60), 10, 600) * 20L;
        tasks.add(plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkWithdrawal, interval, interval));
    }

    private void checkWithdrawal() {
        if (!config.getBoolean("advanced-gameplay.withdrawal.enabled", true)) {
            return;
        }
        long now = System.currentTimeMillis();
        int addictionThreshold = bounded(config.getInt("advanced-gameplay.withdrawal.minimum-addiction", 20), 1, 100);
        long delay = bounded(config.getInt("advanced-gameplay.withdrawal.delay-minutes", 15), 1, 1440) * 60_000L;
        long notificationDelay = bounded(config.getInt("advanced-gameplay.withdrawal.message-cooldown-seconds", 180),
                30, 3600) * 1000L;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PersistentDataContainer data = player.getPersistentDataContainer();
            int addiction = integer(data, addictionKey);
            long lastUse = data.getOrDefault(lastUseKey, PersistentDataType.LONG, 0L);
            if (addiction < addictionThreshold || lastUse == 0L || now - lastUse < delay) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 45 * 20, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 35 * 20, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 20, 0, false, false, true));
            player.setExhaustion(Math.min(4.0f, player.getExhaustion() + 0.8f));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.45f, 0.65f);
            long lastNotice = data.getOrDefault(withdrawalAtKey, PersistentDataType.LONG, 0L);
            if (now - lastNotice >= notificationDelay) {
                messages.send(player, "advanced.withdrawal", Map.of("level", Integer.toString(addiction)));
                data.set(withdrawalAtKey, PersistentDataType.LONG, now);
            }
            int recoveryAfter = bounded(config.getInt("advanced-gameplay.withdrawal.recovery-after-minutes", 30),
                    1, 1440);
            if (now - lastUse >= recoveryAfter * 60_000L) {
                data.set(addictionKey, PersistentDataType.INTEGER, Math.max(0, addiction - 1));
                int tolerance = integer(data, toleranceKey);
                data.set(toleranceKey, PersistentDataType.INTEGER, Math.max(0, tolerance - 1));
            }
        }
    }

    private void maybeStartPerceptionEvent(Player player, DrugDefinition drug, int tolerance) {
        if (!config.getBoolean("advanced-gameplay.perception.enabled", true)
                || !(drug.category().equalsIgnoreCase("psychedelic")
                || drug.category().equalsIgnoreCase("dissociative")
                || drug.category().equalsIgnoreCase("synthetic"))) {
            return;
        }
        double chance = boundedChance(config.getDouble("advanced-gameplay.perception.chance", 0.35));
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        int seconds = bounded(config.getInt("advanced-gameplay.perception.duration-seconds", 18), 3, 120);
        messages.send(player, "advanced.perception");
        final int[] elapsed = {0};
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || elapsed[0]++ >= seconds) {
                tasks.remove(holder[0]);
                holder[0].cancel();
                return;
            }
            Location eye = player.getEyeLocation();
            player.getWorld().spawnParticle(Particle.PORTAL, eye, 14, 1.3, 0.8, 1.3, 0.12);
            player.getWorld().spawnParticle(Particle.WITCH, eye, 5, 0.8, 0.5, 0.8, 0.04);
            if (ThreadLocalRandom.current().nextInt(3) == 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.22f,
                        (float) ThreadLocalRandom.current().nextDouble(0.55, 1.6));
            }
            if (tolerance < 40 && ThreadLocalRandom.current().nextInt(4) == 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 35, 0, false, false, false));
            }
        }, 0L, 20L);
        tasks.add(holder[0]);
    }

    private void maybeStartEnforcement(Player player, int addiction) {
        if (!config.getBoolean("advanced-gameplay.enforcement.enabled", true)) {
            return;
        }
        double base = boundedChance(config.getDouble("advanced-gameplay.enforcement.base-chance", 0.03));
        double chance = Math.min(0.40, base + addiction / 1000.0);
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        int count = bounded(config.getInt("advanced-gameplay.enforcement.patrol-size", 2), 1, 4);
        List<Pillager> patrol = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Location location = patrolLocation(player, i);
            Pillager pillager = player.getWorld().spawn(location, Pillager.class, spawned -> {
                spawned.customName(messages.raw("<red>Contraband Patrol</red>"));
                spawned.setCustomNameVisible(true);
                spawned.setTarget(player);
                spawned.setRemoveWhenFarAway(true);
            });
            patrol.add(pillager);
        }
        if (config.getBoolean("advanced-gameplay.enforcement.confiscate-one-item", true)) {
            confiscateOne(player);
        }
        player.playSound(player.getLocation(), Sound.EVENT_RAID_HORN, 0.8f, 1.15f);
        messages.send(player, "advanced.enforcement", Map.of("count", Integer.toString(count)));
        int lifetime = bounded(config.getInt("advanced-gameplay.enforcement.lifetime-seconds", 45), 10, 180) * 20;
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tasks.remove(holder[0]);
            patrol.stream().filter(Pillager::isValid).forEach(Pillager::remove);
        }, lifetime);
        tasks.add(holder[0]);
    }

    private void maybeGiveRandomEvent(Player player) {
        if (!config.getBoolean("advanced-gameplay.random-events.enabled", true)
                || ThreadLocalRandom.current().nextDouble() > boundedChance(
                config.getDouble("advanced-gameplay.random-events.supply-find-chance", 0.04))) {
            return;
        }
        List<String> pool = config.getStringList("advanced-gameplay.random-events.supply-items");
        if (pool.isEmpty()) {
            pool = List.of("hemp_seed", "dried_herb", "empty_pouch");
        }
        String id = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        items.create(id, 1).ifPresent(item -> {
            player.getInventory().addItem(item).values().forEach(left ->
                    player.getWorld().dropItemNaturally(player.getLocation(), left));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f);
            messages.send(player, "advanced.random-supply", Map.of("item", plainId(id)));
        });
    }

    private void applyMixReaction(Player player, boolean hazardous, String first, String second) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, (hazardous ? 25 : 12) * 20,
                hazardous ? 1 : 0, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (hazardous ? 30 : 15) * 20,
                hazardous ? 1 : 0, false, true, true));
        if (hazardous) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 8 * 20, 0, false, false, true));
            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.9f, 0.65f);
        }
        messages.send(player, hazardous ? "advanced.mix-hazardous" : "advanced.mix", Map.of(
                "first", plainId(first), "second", plainId(second)));
    }

    private void confiscateOne(Player player) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            DrugDefinition definition = items.identify(stack).orElse(null);
            if (definition != null && definition.consumable()) {
                stack.setAmount(stack.getAmount() - 1);
                messages.send(player, "advanced.confiscated", Map.of("drug", plainId(definition.id())));
                return;
            }
        }
    }

    private Location patrolLocation(Player player, int index) {
        double angle = (Math.PI * 2 / 4) * index + ThreadLocalRandom.current().nextDouble(-0.4, 0.4);
        int distance = ThreadLocalRandom.current().nextInt(6, 11);
        int x = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        int y = player.getWorld().getHighestBlockYAt(x, z) + 1;
        return new Location(player.getWorld(), x + 0.5, y, z + 0.5);
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value);
    }

    private static int integer(PersistentDataContainer data, NamespacedKey key) {
        return data.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    static int nextLevel(int current, int gain) {
        return Math.max(0, Math.min(100, current + Math.max(0, gain)));
    }

    static int addictionGain(int dosePoints) {
        return Math.max(1, Math.min(5, (dosePoints + 1) / 2));
    }

    static int toleranceGain(int dosePoints) {
        return Math.max(1, Math.min(4, (dosePoints + 2) / 3));
    }

    static double effectiveness(int tolerance, double minimum) {
        double safeMinimum = Math.max(0.1, Math.min(minimum, 1.0));
        return Math.max(safeMinimum, 1.0 - Math.max(0, Math.min(tolerance, 100)) * 0.006);
    }

    static boolean isHazardousMix(String first, String second) {
        String left = first.toLowerCase(Locale.ROOT);
        String right = second.toLowerCase(Locale.ROOT);
        return (DOWNERS.contains(left) && DOWNERS.contains(right))
                || (DOWNERS.contains(left) && UPPERS.contains(right))
                || (UPPERS.contains(left) && DOWNERS.contains(right));
    }

    private static int bounded(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double boundedChance(double value) {
        return Math.max(0.0, Math.min(value, 1.0));
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
            player.getInventory().setItemInMainHand(stack);
        }
    }

    private static String plainId(String id) {
        String spaced = id.replace('_', ' ').replace('-', ' ');
        return spaced.isBlank() ? id : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    public record ConsumptionModifiers(double effectiveness, int extraDosePoints) {
    }
}
