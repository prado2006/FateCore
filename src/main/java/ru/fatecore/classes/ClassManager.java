package ru.fatecore.classes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.fatecore.FateCore;
import ru.fatecore.gui.FateHolder;
import ru.fatecore.util.Items;
import ru.fatecore.util.Msg;
import ru.fatecore.util.YamlStore;

import java.util.UUID;

/** МОДУЛЬ 1 — Классы: Кузнец / Воин / Странник, смена раз в 7 дней. */
public final class ClassManager {

    public enum FateClass {
        KUZNEC("kuznec", "Кузнец", Material.ANVIL, 11, "&7Мастер ремесла и ковки."),
        VOIN("voin", "Воин", Material.IRON_SWORD, 13, "&7Мастер боя и защиты."),
        STRANNIK("strannik", "Странник", Material.COMPASS, 15, "&7Исследователь миров.");

        public final String id;
        public final String title;
        public final Material icon;
        public final int slot;
        public final String desc;

        FateClass(String id, String title, Material icon, int slot, String desc) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.slot = slot;
            this.desc = desc;
        }

        public static FateClass byId(String id) {
            for (FateClass c : values()) if (c.id.equalsIgnoreCase(id)) return c;
            return null;
        }

        public static FateClass bySlot(int slot) {
            for (FateClass c : values()) if (c.slot == slot) return c;
            return null;
        }
    }

    private final FateCore plugin;
    private final YamlStore store;

    public ClassManager(FateCore plugin) {
        this.plugin = plugin;
        this.store = new YamlStore(plugin, "classes.yml");
    }

    public FateClass getClass(UUID id) {
        String v = store.cfg().getString("players." + id + ".class");
        return v == null ? null : FateClass.byId(v);
    }

    public String getTitle(UUID id) {
        FateClass c = getClass(id);
        return c == null ? "—" : c.title;
    }

    private long changeDays() {
        return store.cfg().getLong("change-days", plugin.getConfig().getLong("classes.change-days", 7));
    }

    private long cooldownMs() {
        return changeDays() * 24L * 60 * 60 * 1000;
    }

    /** Сколько мс осталось до возможности смены (0 — можно). */
    public long remainingMs(UUID id) {
        if (getClass(id) == null) return 0;
        long changedAt = store.cfg().getLong("players." + id + ".changedAt");
        return Math.max(0, changedAt + cooldownMs() - System.currentTimeMillis());
    }

    public void openGui(Player p) {
        FateHolder holder = new FateHolder("class");
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("&8Выбери класс"));
        holder.bind(inv);
        Items.fill(inv);
        FateClass cur = getClass(p.getUniqueId());
        for (FateClass c : FateClass.values()) {
            inv.setItem(c.slot, Items.make(c.icon, "&6" + c.title,
                    c.desc,
                    cur == c ? "&aТвой текущий класс" : "&eКлик — выбрать",
                    "&8Смена класса — раз в " + changeDays() + " дней"));
        }
        p.openInventory(inv);
    }

    public void handleClick(Player p, int slot) {
        FateClass c = FateClass.bySlot(slot);
        if (c == null) return;
        select(p, c);
    }

    private void select(Player p, FateClass c) {
        UUID id = p.getUniqueId();
        if (!p.hasPermission("fatecore.class.use")) {
            Msg.send(p, "&cУ тебя нет доступа к выбору класса.");
            return;
        }
        if (getClass(id) == c) {
            Msg.send(p, "&7Это уже твой класс.");
            return;
        }
        long left = remainingMs(id);
        if (left > 0) {
            long hours = left / 3_600_000L;
            Msg.send(p, "&cСменить класс можно через " + (hours / 24) + " д. " + (hours % 24) + " ч.");
            return;
        }
        store.cfg().set("players." + id + ".class", c.id);
        store.cfg().set("players." + id + ".changedAt", System.currentTimeMillis());
        store.dirty();
        p.closeInventory();
        Msg.send(p, "&aТы выбрал класс: &6" + c.title);
    }

    public void flush() {
        store.flush();
    }
}
