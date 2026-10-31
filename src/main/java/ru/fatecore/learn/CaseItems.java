package ru.fatecore.learn;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.fatecore.FateCore;
import ru.fatecore.util.Items;

/** Предмет-кейс из /shop (открытия пока нет — см. DEFERRED.md). */
public final class CaseItems {
    private CaseItems() {}

    public static ItemStack make(FateCore plugin, boolean rare) {
        ItemStack chest = Items.make(rare ? Material.ENDER_CHEST : Material.CHEST,
                rare ? "&5Редкий кейс" : "&aОбычный кейс", "&7Кейс из /shop");
        ItemMeta m = chest.getItemMeta();
        m.getPersistentDataContainer().set(plugin.key("case"), PersistentDataType.STRING, rare ? "rare" : "common");
        chest.setItemMeta(m);
        return chest;
    }
}
