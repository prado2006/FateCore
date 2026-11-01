package ru.fatecore.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class MenuItems {

    public enum Kind { COMPASS, DONATE }

    private final NamespacedKey key;

    public MenuItems(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "menu_item");
    }

    /** Строка с кодами § -> Component без курсива (для названий и лора). */
    public static Component text(String legacy) {
        return LegacyComponentSerializer.legacySection()
                .deserialize(legacy)
                .decoration(TextDecoration.ITALIC, false);
    }

    public ItemStack create(Kind kind) {
        ItemStack item = new ItemStack(kind == Kind.COMPASS ? Material.COMPASS : Material.NETHER_STAR);
        item.editMeta(meta -> {
            if (kind == Kind.COMPASS) {
                meta.displayName(text("§6Меню игрока"));
                meta.lore(List.of(
                        text("§7ПКМ - Выбор класса"),
                        text("§7ЛКМ - Помощь и команды")));
            } else {
                meta.displayName(text("§eПривилегии (Донат)"));
                meta.lore(List.of(
                        text("§7Узнай о привилегиях и поддержи"),
                        text("§7проект!"),
                        text("§aНажми ПКМ")));
            }
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, kind.name());
        });
        return item;
    }

    public Kind kindOf(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String v = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (v == null) return null;
        try {
            return Kind.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isMenuItem(ItemStack item) {
        return kindOf(item) != null;
    }

    public boolean has(Player p, Kind kind) {
        for (ItemStack s : p.getInventory().getContents()) {
            if (kindOf(s) == kind) return true;
        }
        return false;
    }
}
