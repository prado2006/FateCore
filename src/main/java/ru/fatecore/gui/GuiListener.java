package ru.fatecore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import ru.fatecore.classes.ClassManager;
import ru.fatecore.learn.ShopGui;

public final class GuiListener implements Listener {
    private final ClassManager classes;
    private final ShopGui shop;

    public GuiListener(ClassManager classes, ShopGui shop) {
        this.classes = classes;
        this.shop = shop;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof FateHolder h)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        String type = h.type();
        if (type.equals("class")) {
            classes.handleClick(p, slot);
        } else if (type.equals("quests")) {
            if (slot == 26) p.closeInventory();
        } else if (type.equals("shop")) {
            shop.handleMain(p, slot);
        } else if (type.startsWith("shop:")) {
            shop.handleCategory(p, type.substring(5), slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof FateHolder) e.setCancelled(true);
    }
}
