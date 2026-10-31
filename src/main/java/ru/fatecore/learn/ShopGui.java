package ru.fatecore.learn;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.fatecore.FateCore;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.gui.FateHolder;
import ru.fatecore.perks.HomeBonus;
import ru.fatecore.util.Items;
import ru.fatecore.util.Msg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * МОДУЛЬ 10 — /shop: GUI 27 слотов, категории и товары из learn.shop, оплата Эхо через Vault (EssentialsX).
 * Выдача: команды консоли из shop.rewards.<id>; если список пуст — встроенная выдача.
 * Донат-тиры через магазин не выдаются (команды с «parent» пропускаются).
 */
public final class ShopGui {

    private record Category(String key, int slot, String title, Material icon) {}

    private record Item(String id, String category, String name, Material mat, int price, int slot, boolean enabled) {}

    private final FateCore plugin;
    private final EchoManager echo;
    private final HomeBonus homeBonus;

    public ShopGui(FateCore plugin, EchoManager echo) {
        this.plugin = plugin;
        this.echo = echo;
        this.homeBonus = new HomeBonus(plugin);
    }

    // ---------- чтение learn.shop ----------

    private List<Category> categories() {
        List<Category> out = new ArrayList<>();
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("learn.shop.categories");
        if (s == null) return out;
        for (String key : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(key);
            if (c == null) continue;
            Material icon = Material.matchMaterial(c.getString("icon", "CHEST"));
            out.add(new Category(key, c.getInt("slot"), c.getString("title", key),
                    icon == null ? Material.CHEST : icon));
        }
        return out;
    }

    private List<Item> items() {
        List<Item> out = new ArrayList<>();
        for (Map<?, ?> m : plugin.getConfig().getMapList("learn.shop.items")) {
            Material mat = Material.matchMaterial(String.valueOf(m.get("icon")));
            Object price = m.get("price");
            Object slot = m.get("slot");
            if (mat == null || !(price instanceof Number) || !(slot instanceof Number)) {
                plugin.getLogger().warning("learn.shop.items: пропущен товар с неверными полями: " + m);
                continue;
            }
            Object en = m.get("enabled");
            out.add(new Item(String.valueOf(m.get("id")), String.valueOf(m.get("category")),
                    String.valueOf(m.get("name")), mat, ((Number) price).intValue(),
                    ((Number) slot).intValue(), !(en instanceof Boolean b) || b));
        }
        return out;
    }

    private String title() {
        return plugin.getConfig().getString("learn.shop.title", "&8Магазин Эхо");
    }

    // ---------- GUI ----------

    public void openMain(Player p) {
        FateHolder holder = new FateHolder("shop");
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c(title()));
        holder.bind(inv);
        Items.fill(inv);
        for (Category c : categories()) {
            inv.setItem(c.slot(), Items.make(c.icon(), "&6" + c.title(), "&eКлик — открыть"));
        }
        inv.setItem(22, Items.make(Material.GOLD_INGOT, "&6Твоё Эхо: &f" + echo.format(echo.getEcho(p))));
        inv.setItem(26, Items.make(Material.BARRIER, "&cЗакрыть"));
        p.openInventory(inv);
    }

    private void openCategory(Player p, Category cat) {
        FateHolder holder = new FateHolder("shop:" + cat.key());
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("&8Магазин: " + cat.title()));
        holder.bind(inv);
        Items.fill(inv);
        for (Item it : items()) {
            if (!it.category().equals(cat.key())) continue;
            if (it.enabled()) {
                inv.setItem(it.slot(), Items.make(it.mat(), "&6" + it.name(),
                        "&7Цена: &f" + it.price() + " Эхо",
                        "&eКлик — купить",
                        "&8Эхо не даёт силы в бою"));
            } else {
                inv.setItem(it.slot(), Items.make(Material.GRAY_DYE, "&7" + it.name(),
                        "&cВременно недоступен"));
            }
        }
        inv.setItem(22, Items.make(Material.ARROW, "&7Назад"));
        inv.setItem(26, Items.make(Material.BARRIER, "&cЗакрыть"));
        p.openInventory(inv);
    }

    public void handleMain(Player p, int slot) {
        if (slot == 26) {
            p.closeInventory();
            return;
        }
        for (Category c : categories()) {
            if (c.slot() == slot) {
                openCategory(p, c);
                return;
            }
        }
    }

    public void handleCategory(Player p, String catKey, int slot) {
        if (slot == 26) {
            p.closeInventory();
            return;
        }
        if (slot == 22) {
            openMain(p);
            return;
        }
        for (Item it : items()) {
            if (it.category().equals(catKey) && it.slot() == slot) {
                purchase(p, it);
                return;
            }
        }
    }

    // ---------- покупка ----------

    private void purchase(Player p, Item item) {
        if (!item.enabled()) {
            Msg.send(p, "&cТовар временно недоступен.");
            return;
        }
        if (item.id().equals("home_plus1") && plugin.getConfig().getStringList("shop.rewards.home_plus1").isEmpty()
                && !homeBonus.canGrant(p)) {
            Msg.send(p, "&cМаксимум домов уже достигнут.");
            return;
        }
        if (!echo.take(p, item.price())) {
            Msg.send(p, "&cНе хватает Эхо: нужно " + item.price() + ", у тебя " + echo.format(echo.getEcho(p)));
            return;
        }
        try {
            deliver(p, item);
            Msg.send(p, "&aКуплено: &6" + item.name() + " &7(-" + item.price() + " Эхо)");
        } catch (Exception e) {
            echo.addEcho(p, item.price());
            Msg.send(p, "&cОшибка выдачи, Эхо возвращено.");
            plugin.getLogger().warning("Shop deliver " + item.id() + ": " + e.getMessage());
        }
    }

    private void deliver(Player p, Item item) {
        List<String> cmds = plugin.getConfig().getStringList("shop.rewards." + item.id());
        if (!cmds.isEmpty()) {
            runCommands(p, cmds);
            return;
        }
        switch (item.id()) {
            case "rename" -> Items.give(p, Items.make(Material.NAME_TAG, "&6Бирка переименования",
                    "&7Переименуй предмет на наковальне"));
            case "case_common" -> Items.give(p, CaseItems.make(plugin, false));
            case "case_rare" -> Items.give(p, CaseItems.make(plugin, true));
            case "boost_x2" -> echo.addBoost(p.getUniqueId(), 2.0, 3600);
            case "home_plus1" -> {
                if (!homeBonus.grant(p)) throw new IllegalStateException("лимит домов достигнут");
            }
            default -> throw new IllegalStateException("нет награды для " + item.id());
        }
    }

    private void runCommands(Player p, List<String> cmds) {
        for (String c : cmds) {
            String line = c.replace("%player%", p.getName());
            if (line.toLowerCase().contains("parent")) {
                plugin.getLogger().warning("Shop: команда с parent пропущена: " + line);
                continue;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
        }
    }
}
