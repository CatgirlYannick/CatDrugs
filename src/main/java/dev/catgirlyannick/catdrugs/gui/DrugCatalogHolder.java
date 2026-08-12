package dev.catgirlyannick.catdrugs.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class DrugCatalogHolder implements InventoryHolder {
    private final Inventory inventory;
    private final int page;
    private final int totalPages;

    public DrugCatalogHolder(int size, Component title, int page, int totalPages) {
        this.page = page;
        this.totalPages = totalPages;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public int page() {
        return page;
    }

    public int totalPages() {
        return totalPages;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
