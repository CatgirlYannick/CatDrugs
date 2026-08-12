package dev.catgirlyannick.catdrugs.command;

import dev.catgirlyannick.catdrugs.CatDrugsPlugin;
import dev.catgirlyannick.catdrugs.config.DrugRegistry;
import dev.catgirlyannick.catdrugs.gui.DrugCatalogMenu;
import dev.catgirlyannick.catdrugs.integration.CatItemsAddonService;
import dev.catgirlyannick.catdrugs.integration.CatItemsBridge;
import dev.catgirlyannick.catdrugs.item.DrugItemFactory;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import dev.catgirlyannick.catdrugs.service.ConsumptionService;
import dev.catgirlyannick.catdrugs.service.AdvancedGameplayService;
import dev.catgirlyannick.catdrugs.service.MessageService;
import dev.catgirlyannick.catdrugs.service.VillageDealerService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CatDrugsCommand implements CommandExecutor, TabCompleter {
    private final CatDrugsPlugin plugin;
    private final DrugRegistry registry;
    private final DrugItemFactory items;
    private final MessageService messages;
    private final ConsumptionService consumption;
    private final VillageDealerService dealers;
    private final AdvancedGameplayService advancedGameplay;
    private final CatItemsBridge catItems;
    private final CatItemsAddonService catItemsAddon;
    private final DrugCatalogMenu menu;

    public CatDrugsCommand(CatDrugsPlugin plugin, DrugRegistry registry, DrugItemFactory items,
                           MessageService messages, ConsumptionService consumption,
                           VillageDealerService dealers, AdvancedGameplayService advancedGameplay,
                           CatItemsBridge catItems,
                           CatItemsAddonService catItemsAddon, DrugCatalogMenu menu) {
        this.plugin = plugin;
        this.registry = registry;
        this.items = items;
        this.messages = messages;
        this.consumption = consumption;
        this.dealers = dealers;
        this.advancedGameplay = advancedGameplay;
        this.catItems = catItems;
        this.catItemsAddon = catItemsAddon;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                menu.open(player);
            } else {
                help(sender);
            }
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender);
            case "list" -> list(sender);
            case "info" -> info(sender, args);
            case "give" -> give(sender, args);
            case "dealer" -> dealer(sender, args);
            case "profile" -> profile(sender);
            case "quest" -> quest(sender);
            case "reset" -> reset(sender, args);
            case "reload" -> reload(sender);
            case "status" -> status(sender);
            case "catitems" -> catItemsAdmin(sender, args);
            default -> messages.send(sender, "errors.unknown-command");
        }
        return true;
    }

    private void help(CommandSender sender) {
        messages.send(sender, "help.header");
        messages.send(sender, "help.public");
        if (sender.hasPermission("catdrugs.admin")) {
            messages.send(sender, "help.admin");
        }
    }

    private void list(CommandSender sender) {
        String joined = registry.consumables().stream().filter(DrugDefinition::enabled)
                .map(DrugDefinition::id).sorted().reduce((left, right) -> left + ", " + right).orElse("-");
        messages.send(sender, "commands.list", Map.of("drugs", joined));
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "errors.usage-info");
            return;
        }
        DrugDefinition drug = registry.find(args[1]).orElse(null);
        if (drug == null) {
            messages.send(sender, "errors.unknown-drug", Map.of("drug", args[1]));
            return;
        }
        messages.send(sender, "commands.info", Map.of(
                "id", drug.id(),
                "name", drug.displayName().replaceAll("<[^>]+>", ""),
                "cooldown", Integer.toString(drug.cooldownSeconds()),
                "effects", Integer.toString(drug.immediateEffects().size() + drug.afterEffects().size())
        ));
    }

    private void give(CommandSender sender, String[] args) {
        if (!require(sender, "catdrugs.admin.give") || args.length < 3) {
            if (args.length < 3) messages.send(sender, "errors.usage-give");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", Map.of("player", args[1]));
            return;
        }
        int amount = args.length >= 4 ? parseAmount(args[3]) : 1;
        items.create(args[2], amount).ifPresentOrElse(item -> {
            target.getInventory().addItem(item).values().forEach(leftover ->
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            messages.send(sender, "admin.given", Map.of("player", target.getName(), "drug", args[2],
                    "amount", Integer.toString(amount)));
        }, () -> messages.send(sender, "errors.unknown-drug", Map.of("drug", args[2])));
    }

    private void dealer(CommandSender sender, String[] args) {
        if (!require(sender, "catdrugs.admin.dealer") || !(sender instanceof Player player)) {
            return;
        }
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "spawn";
        if (action.equals("spawn")) {
            Villager villager = player.getWorld().spawn(player.getLocation(), Villager.class);
            dealers.makeDealer(villager);
            messages.send(sender, "admin.dealer-spawned");
            return;
        }
        if (action.equals("refresh")) {
            Villager nearest = player.getNearbyEntities(8, 8, 8).stream()
                    .filter(Villager.class::isInstance).map(Villager.class::cast)
                    .min(java.util.Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation())))
                    .orElse(null);
            if (nearest == null) {
                messages.send(sender, "errors.no-villager-nearby");
            } else {
                dealers.makeDealer(nearest);
                messages.send(sender, "admin.dealer-refreshed");
            }
            return;
        }
        messages.send(sender, "errors.usage-dealer");
    }

    private void reload(CommandSender sender) {
        if (!require(sender, "catdrugs.admin.reload")) {
            return;
        }
        if (plugin.reloadCatDrugs()) {
            messages.send(sender, "admin.reload-success");
        } else {
            messages.send(sender, "admin.reload-failed");
        }
    }

    private void status(CommandSender sender) {
        if (!require(sender, "catdrugs.admin.status")) {
            return;
        }
        int dose = sender instanceof Player player ? consumption.currentDose(player) : 0;
        messages.send(sender, "admin.status", Map.of(
                "version", plugin.getPluginMeta().getVersion(),
                "drugs", Integer.toString(registry.consumables().size()),
                "dealers", Integer.toString(dealers.countLoadedDealers()),
                "catitems", catItemsAddon.status(registry.all()),
                "dose", Integer.toString(dose)
        ));
    }

    private void profile(CommandSender sender) {
        if (sender instanceof Player player) {
            advancedGameplay.showProfile(player);
        }
    }

    private void quest(CommandSender sender) {
        if (sender instanceof Player player) {
            advancedGameplay.showQuest(player);
        }
    }

    private void reset(CommandSender sender, String[] args) {
        if (!require(sender, "catdrugs.admin.reset") || args.length < 2) {
            if (args.length < 2) messages.send(sender, "errors.usage-reset");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", Map.of("player", args[1]));
            return;
        }
        advancedGameplay.reset(target);
        messages.send(sender, "admin.progress-reset", Map.of("player", target.getName()));
    }

    private void catItemsAdmin(CommandSender sender, String[] args) {
        if (!require(sender, "catdrugs.admin.catitems")) {
            return;
        }
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "verify";
        if (action.equals("install")) {
            boolean force = args.length >= 3 && args[2].equalsIgnoreCase("force");
            CatItemsAddonService.InstallResult result = catItemsAddon.install(registry.all(), force);
            if (!result.catItemsAvailable()) {
                messages.send(sender, "errors.catitems-unavailable");
                return;
            }
            if (!result.error().isBlank()) {
                messages.send(sender, "admin.catitems-install-failed", Map.of("error", result.error()));
                return;
            }
            messages.send(sender, "admin.catitems-installed", Map.of(
                    "items", Integer.toString(result.itemsWritten()),
                    "assets", Integer.toString(result.assetsWritten()),
                    "conflicts", Integer.toString(result.conflicts().size())
            ));
            return;
        }
        if (action.equals("verify")) {
            if (!catItems.available()) {
                messages.send(sender, "errors.catitems-unavailable");
                return;
            }
            List<String> missing = catItems.missingItems(registry.all());
            if (missing.isEmpty()) {
                messages.send(sender, "admin.catitems-verify-success", Map.of(
                        "items", Integer.toString(registry.all().size())));
            } else {
                messages.send(sender, "admin.catitems-verify-missing", Map.of(
                        "missing", String.join(", ", missing)));
            }
            return;
        }
        if (action.equals("rebuild")) {
            if (!catItems.available()) {
                messages.send(sender, "errors.catitems-unavailable");
                return;
            }
            if (catItemsAddon.rebuild()) {
                messages.send(sender, "admin.catitems-rebuild-started");
            } else {
                messages.send(sender, "admin.catitems-rebuild-failed");
            }
            return;
        }
        messages.send(sender, "errors.usage-catitems");
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.send(sender, "errors.no-permission");
        return false;
    }

    private int parseAmount(String value) {
        try {
            return Math.max(1, Math.min(Integer.parseInt(value), 64));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> choices = new ArrayList<>();
        if (args.length == 1) {
            choices.addAll(List.of("help", "list", "info", "profile", "quest"));
            if (sender.hasPermission("catdrugs.admin.give")) choices.add("give");
            if (sender.hasPermission("catdrugs.admin.dealer")) choices.add("dealer");
            if (sender.hasPermission("catdrugs.admin.reload")) choices.add("reload");
            if (sender.hasPermission("catdrugs.admin.status")) choices.add("status");
            if (sender.hasPermission("catdrugs.admin.catitems")) choices.add("catitems");
            if (sender.hasPermission("catdrugs.admin.reset")) choices.add("reset");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            choices.addAll(registry.consumables().stream().map(DrugDefinition::id).toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("catdrugs.admin.give")) {
            choices.addAll(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset") && sender.hasPermission("catdrugs.admin.reset")) {
            choices.addAll(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("catdrugs.admin.give")) {
            choices.addAll(registry.all().stream().map(DrugDefinition::id).toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("dealer") && sender.hasPermission("catdrugs.admin.dealer")) {
            choices.addAll(List.of("spawn", "refresh"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("catitems")
                && sender.hasPermission("catdrugs.admin.catitems")) {
            choices.addAll(List.of("install", "verify", "rebuild"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("catitems")
                && args[1].equalsIgnoreCase("install") && sender.hasPermission("catdrugs.admin.catitems")) {
            choices.add("force");
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
