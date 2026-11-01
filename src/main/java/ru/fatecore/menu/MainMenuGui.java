package ru.fatecore.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ru.fatecore.classes.ClassManager;
import ru.fatecore.combat.CombatManager;

import java.util.ArrayList;
import java.util.List;

public final class MainMenuGui implements Listener {
    private static final int SIZE = 27;

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    private final Plugin plugin;
    private final ClassManager classes;
    private final HelpGui helpGui;
    private final DonateGui donateGui;

    public MainMenuGui(Plugin plugin, ClassManager classes, HelpGui helpGui, DonateGui donateGui, CombatManager combat) {
        this.plugin = plugin;
        this.classes = classes;
        this.helpGui = helpGui;
        this.donateGui = donateGui;
    }

    public void open(Player p) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, MenuItems.text("&8Главное меню"));
        holder.inventory = inv;

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        filler.editMeta(m -> m.displayName(MenuItems.text("&r")));
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        inv.setItem(10, button(Material.IRON_SWORD, "&6Выбор класса", List.of("&7Выбери свой путь: Кузнец, Воин или Странник.")));
        
        int survOnline = getOnline("world_survival");
        int bwOnline = getOnline("world_bedwars");
        int swOnline = getOnline("world_skywars");

        List<String> teleportLore = new ArrayList<>();
        teleportLore.add("&7Перейди на арену:");
        teleportLore.add("&aSurvival &7(Игроков: " + survOnline + ")");
        teleportLore.add("&cBedWars &7(Игроков: " + bwOnline + ")");
        teleportLore.add("&bSkyWars &7(Игроков: " + swOnline + ")");
        
        inv.setItem(12, button(Material.COMPASS, "&6Телепортация", teleportLore));
        inv.setItem(14, button(Material.NETHER_STAR, "&6Привилегии", List.of("&7Узнай о донат-тирах и поддержи проект.")));
        inv.setItem(16, button(Material.BOOK, "&6Помощь и правила", List.of("&7Все команды сервера и полезные советы.")));

        p.openInventory(inv);
    }

    private int getOnline(String worldName) {
        World w = Bukkit.getWorld(worldName);
        return w != null ? w.getPlayers().size() : 0;
    }

    private ItemStack button(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(m -> {
            m.displayName(MenuItems.text(name));
            m.lore(lore.stream().map(MenuItems::text).toList());
        });
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        switch (e.getSlot()) {
            case 10 -> { p.closeInventory(); classes.openGui(p); }
            case 14 -> { p.closeInventory(); donateGui.open(p); }
            case 16 -> { p.closeInventory(); helpGui.open(p); }
            case 12 -> {
                p.closeInventory();
                p.sendMessage(MenuItems.text("&6Выбери режим (напиши в чат):"));
                p.sendMessage(MenuItems.text("&e/survival &7- Выживание"));
                p.sendMessage(MenuItems.text("&e/bedwars &7- БедВарс"));
                p.sendMessage(MenuItems.text("&e/skywars &7- СкайВарс"));
                p.sendMessage(MenuItems.text("&e/lobby &7- Вернуться в хаб"));
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
