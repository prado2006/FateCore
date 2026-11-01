package ru.fatecore.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HelpGui implements Listener {

    private static final int SIZE = 27;
    private static final int SLOT_CLOSE = 22;
    private static final int SLOT_DONATE = 16;

    /** слот -> команда, которую выполнит игрок */
    private static final Map<Integer, String> COMMANDS = new HashMap<>();
    static {
        COMMANDS.put(10, "help");
        COMMANDS.put(12, "quests");
        COMMANDS.put(14, "shop");
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    private final Plugin plugin;
    private final DonateGui donateGui;

    public HelpGui(Plugin plugin, DonateGui donateGui) {
        this.plugin = plugin;
        this.donateGui = donateGui;
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, MenuItems.text("§6Помощь и команды"));
        holder.inventory = inv;

        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, "§r", List.of());
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        inv.setItem(10, button(Material.BOOK, "§e/help",
                List.of("§7Список всех команд сервера", "", "§aНажми, чтобы выполнить")));
        inv.setItem(12, button(Material.WRITABLE_BOOK, "§e/quests",
                List.of("§7Ежедневные задания и квесты", "", "§aНажми, чтобы выполнить")));
        inv.setItem(14, button(Material.EMERALD, "§e/shop",
                List.of("§7Магазин сервера", "", "§aНажми, чтобы выполнить")));
        inv.setItem(SLOT_DONATE, button(Material.NETHER_STAR, "§e/donate",
                List.of("§7Привилегии и поддержка проекта", "", "§aНажми, чтобы открыть")));
        inv.setItem(SLOT_CLOSE, button(Material.BARRIER, "§cЗакрыть", List.of()));

        player.openInventory(inv);
    }

    private static ItemStack button(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(m -> {
            m.displayName(MenuItems.text(name));
            if (!lore.isEmpty()) {
                m.lore(lore.stream().map(MenuItems::text).toList());
            }
        });
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true); // в меню ничего нельзя двигать (в т.ч. шифт-кликом из инвентаря)
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        int slot = e.getSlot();
        if (slot == SLOT_CLOSE) {
            p.closeInventory();
        } else if (slot == SLOT_DONATE) {
            Bukkit.getScheduler().runTask(plugin, () -> donateGui.open(p));
        } else if (COMMANDS.containsKey(slot)) {
            String cmd = COMMANDS.get(slot);
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> p.performCommand(cmd));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
