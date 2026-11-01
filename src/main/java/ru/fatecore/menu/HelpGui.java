package ru.fatecore.menu;

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

    private static final int SIZE = 54;
    private static final int SLOT_TITLE = 4;
    private static final int SLOT_DONATE = 16;
    private static final int SLOT_CLOSE = 49;

    private static final Map<Integer, String> COMMANDS = new HashMap<>();
    static {
        COMMANDS.put(10, "help");
        COMMANDS.put(12, "quests");
        COMMANDS.put(14, "shop");
        COMMANDS.put(20, "daily");
        COMMANDS.put(22, "playtime");
        COMMANDS.put(32, "lobby");
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
        Inventory inv = Bukkit.createInventory(holder, SIZE, MenuItems.text("&6Помощь и возможности сервера"));
        holder.inventory = inv;

        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, "&r", List.of());
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        inv.setItem(SLOT_TITLE, button(Material.BOOK, "&6&lГид по серверу",
                List.of("&7Здесь собраны все фишки сервера.", "&7Нажимайте на предметы — команды выполнятся сами.")));

        inv.setItem(10, button(Material.BOOK, "&e/help", List.of("&7Список всех команд сервера", "", "&aНажми, чтобы выполнить")));
        inv.setItem(12, button(Material.WRITABLE_BOOK, "&e/quests", List.of("&7Ежедневные задания Квестового Мастера", "", "&aНажми, чтобы выполнить")));
        inv.setItem(14, button(Material.EMERALD, "&e/shop", List.of("&7Магазин сервера за Эхо", "", "&aНажми, чтобы выполнить")));
        inv.setItem(SLOT_DONATE, button(Material.NETHER_STAR, "&e/donate", List.of("&7Привилегии и поддержка проекта", "", "&aНажми, чтобы открыть")));

        inv.setItem(20, button(Material.GOLD_INGOT, "&6Ежедневная награда",
                List.of("&7Команда: &e/daily", "&7Заходите каждый день — чем длиннее серия,", "&7тем больше награда.", "", "&aНажми, чтобы получить")));
        inv.setItem(22, button(Material.CLOCK, "&6Награда за игру",
                List.of("&7За каждый час игры дают &e100 Эхо&7.", "&7Проверить время: &e/playtime", "", "&aНажми, чтобы проверить")));
        inv.setItem(24, button(Material.SKELETON_SKULL, "&6Баунти",
                List.of("&7Назначьте награду за голову игрока:", "&e/bounty <ник> <сумма>", "&7Тот, кто убьёт его, получит это Эхо.")));
        inv.setItem(30, button(Material.SHIELD, "&6Боевой режим",
                List.of("&7В бою нельзя телепортироваться &e15 сек&7.", "&7Выход из игры во время боя — смерть.")));
        inv.setItem(32, button(Material.FEATHER, "&6Возврат в хаб",
                List.of("&7Команда: &e/lobby", "&7Защита от падения в пустоту.", "", "&aНажми, чтобы вернуться")));

        inv.setItem(SLOT_CLOSE, button(Material.BARRIER, "&cЗакрыть", List.of()));

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
        e.setCancelled(true);
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
