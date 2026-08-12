package dev.catgirlyannick.catdrugs.listener;

import dev.catgirlyannick.catdrugs.gui.DrugCatalogHolder;
import dev.catgirlyannick.catdrugs.gui.DrugCatalogMenu;
import dev.catgirlyannick.catdrugs.item.DrugItemFactory;
import dev.catgirlyannick.catdrugs.service.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public final class GuiListener implements Listener {
    private final DrugItemFactory items;
    private final MessageService messages;
    private final DrugCatalogMenu menu;

    public GuiListener(DrugItemFactory items, MessageService messages, DrugCatalogMenu menu) {
        this.items = items;
        this.messages = messages;
        this.menu = menu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DrugCatalogHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            if (event.getRawSlot() == 45 && holder.page() > 0) {
                menu.open((org.bukkit.entity.Player) event.getWhoClicked(), holder.page() - 1);
                return;
            }
            if (event.getRawSlot() == 53 && holder.page() + 1 < holder.totalPages()) {
                menu.open((org.bukkit.entity.Player) event.getWhoClicked(), holder.page() + 1);
                return;
            }
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()
                || !event.isShiftClick() || !event.getWhoClicked().hasPermission("catdrugs.admin.give")) {
            return;
        }
        items.identify(event.getCurrentItem()).ifPresent(drug -> {
            event.getWhoClicked().getInventory().addItem(items.create(drug, 1));
            messages.send(event.getWhoClicked(), "admin.given", Map.of("amount", "1", "drug", drug.id(),
                    "player", event.getWhoClicked().getName()));
        });
    }
}
