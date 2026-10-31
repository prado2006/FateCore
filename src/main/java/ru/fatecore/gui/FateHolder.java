package ru.fatecore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Маркер GUI FateCore: по type определяется, какое меню открыто. */
public final class FateHolder implements InventoryHolder {
    private final String type;
    private Inventory inventory;

    public FateHolder(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public void bind(Inventory inv) {
        this.inventory = inv;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
