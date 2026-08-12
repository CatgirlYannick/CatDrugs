package dev.catgirlyannick.catdrugs.item;

import dev.catgirlyannick.catdrugs.config.DrugRegistry;
import dev.catgirlyannick.catdrugs.integration.CatItemsBridge;
import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import dev.catgirlyannick.catdrugs.service.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Optional;

public final class DrugItemFactory {
    private final DrugRegistry registry;
    private final CatItemsBridge catItems;
    private final MessageService messages;
    private final NamespacedKey drugIdKey;

    public DrugItemFactory(JavaPlugin plugin, DrugRegistry registry, CatItemsBridge catItems, MessageService messages) {
        this.registry = registry;
        this.catItems = catItems;
        this.messages = messages;
        this.drugIdKey = new NamespacedKey(plugin, "drug_id");
    }

    public Optional<ItemStack> create(String id, int amount) {
        return registry.find(id).filter(DrugDefinition::enabled).map(definition -> create(definition, amount));
    }

    public Optional<DrugDefinition> definition(String id) {
        return registry.find(id);
    }

    public ItemStack create(DrugDefinition definition, int amount) {
        ItemStack item = catItems.createItem(definition.customItemId(), amount)
                .orElseGet(() -> new ItemStack(definition.fallbackMaterial(), amount));
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(drugIdKey, PersistentDataType.STRING, definition.id());
        if (catItems.identify(item).isEmpty()) {
            meta.displayName(messages.raw(definition.displayName()));
            ArrayList<Component> lore = new ArrayList<>();
            definition.lore().forEach(line -> lore.add(messages.raw(line)));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        item.setItemMeta(meta);
        item.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        return item;
    }

    public Optional<DrugDefinition> identify(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(drugIdKey, PersistentDataType.STRING);
        if (id != null) {
            return registry.find(id);
        }
        return catItems.identify(item).flatMap(registry::findByCustomItemId);
    }
}
