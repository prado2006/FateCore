package ru.fatecore.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class Items {
    private Items() {}

    public static ItemStack make(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Msg.c(name));
        if (lore.length > 0) {
            meta.setLore(Arrays.stream(lore).map(Msg::c).toList());
        }
        it.setItemMeta(meta);
        return it;
    }

    public static void fill(Inventory inv) {
        ItemStack f = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, f);
    }

    /** Выдаёт предмет, лишнее выбрасывает под ноги. */
    public static void give(Player p, ItemStack it) {
        p.getInventory().addItem(it).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }
}
