package dev.catgirlyannick.catdrugs.gui;

import dev.catgirlyannick.catdrugs.config.DrugRegistry;
import dev.catgirlyannick.catdrugs.item.DrugItemFactory;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import dev.catgirlyannick.catdrugs.service.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class DrugCatalogMenu {
    private final DrugRegistry registry;
    private final DrugItemFactory items;
    private final MessageService messages;
    private YamlConfiguration config;

    public DrugCatalogMenu(DrugRegistry registry, DrugItemFactory items, MessageService messages, YamlConfiguration config) {
        this.registry = registry;
        this.items = items;
        this.messages = messages;
        this.config = config;
    }

    public void reload(YamlConfiguration config) {
        this.config = config;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int requestedPage) {
        int size = normalizeSize(config.getInt("catalog.size", 54));
        List<Integer> slots = config.getIntegerList("catalog.content-slots");
        List<DrugDefinition> drugs = registry.all().stream()
                .filter(DrugDefinition::enabled).toList();
        int pageSize = Math.max(1, slots.size());
        int totalPages = Math.max(1, (drugs.size() + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        String title = config.getString("catalog.title", "<dark_gray>CatDrugs Catalog")
                + " <gray>(" + (page + 1) + "/" + totalPages + ")</gray>";
        DrugCatalogHolder holder = new DrugCatalogHolder(size, messages.raw(title), page, totalPages);
        Inventory inventory = holder.getInventory();
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, filler);
        }
        int index = 0;
        int from = page * pageSize;
        int to = Math.min(from + pageSize, drugs.size());
        for (DrugDefinition drug : drugs.subList(from, to)) {
            int slot = slots.get(index++);
            if (slot < 0 || slot >= size) {
                continue;
            }
            ItemStack icon = items.create(drug.id(), 1).orElse(new ItemStack(drug.fallbackMaterial()));
            ItemMeta meta = icon.getItemMeta();
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(messages.raw("<gray>ID: <white>" + drug.id() + "</white></gray>"));
            lore.add(messages.raw("<gray>Cooldown: <white>" + drug.cooldownSeconds() + "s</white></gray>"));
            if (player.hasPermission("catdrugs.admin.give")) {
                lore.add(messages.raw("<yellow>Shift-click: receive one item</yellow>"));
            }
            meta.lore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(slot, icon);
        }
        if (page > 0) {
            inventory.setItem(config.getInt("catalog.previous-slot", 45),
                    named(Material.ARROW, "<yellow>Previous page</yellow>", List.of()));
        }
        inventory.setItem(config.getInt("catalog.page-slot", 49), named(Material.BOOK,
                "<white>Page " + (page + 1) + " of " + totalPages + "</white>", List.of()));
        if (page + 1 < totalPages) {
            inventory.setItem(config.getInt("catalog.next-slot", 53),
                    named(Material.ARROW, "<yellow>Next page</yellow>", List.of()));
        }
        player.openInventory(inventory);
    }

    private ItemStack named(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.raw(name));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int normalizeSize(int size) {
        int bounded = Math.max(9, Math.min(size, 54));
        return bounded - bounded % 9;
    }
}
