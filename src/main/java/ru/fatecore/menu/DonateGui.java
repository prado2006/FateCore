package ru.fatecore.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DonateGui implements Listener {

    private static final int SIZE = 54;
    private static final int SLOT_INFO = 4;
    private static final int SLOT_CLOSE = 49;

    /** id = ключ в config.yml (donate.tiers.<id> и donate-multipliers.<id>). */
    private enum Tier {
        VIP("vip", "VIP", Material.GOLD_INGOT, 11),
        PREMIUM("premium", "Премиум", Material.DIAMOND, 13),
        LEGEND("legend", "Легенда", Material.EMERALD, 15),
        ELITE("elite", "Элита", Material.AMETHYST_SHARD, 29),
        MYTHIC("mythic", "Мифик", Material.NETHERITE_INGOT, 31),
        TITAN("titan", "Титан", Material.DRAGON_EGG, 33);

        final String id;
        final String title;
        final Material icon;
        final int slot;

        Tier(String id, String title, Material icon, int slot) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.slot = slot;
        }
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    private final Plugin plugin;

    public DonateGui(Plugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, MenuItems.text("§6Привилегии (Донат)"));
        holder.inventory = inv;

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        filler.editMeta(m -> m.displayName(MenuItems.text("§r")));
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        for (Tier t : Tier.values()) {
            inv.setItem(t.slot, tierItem(cfg, t));
        }

        ItemStack info = new ItemStack(Material.BOOK);
        info.editMeta(m -> {
            m.displayName(MenuItems.text("§eКак купить?"));
            m.lore(List.of(
                    MenuItems.text("§7Выбери привилегию и нажми на неё —"),
                    MenuItems.text("§7в чат придёт ссылка на покупку."),
                    MenuItems.text(""),
                    MenuItems.text("§aНажми, чтобы получить ссылку")));
        });
        inv.setItem(SLOT_INFO, info);

        ItemStack close = new ItemStack(Material.BARRIER);
        close.editMeta(m -> m.displayName(MenuItems.text("§cЗакрыть")));
        inv.setItem(SLOT_CLOSE, close);

        player.openInventory(inv);
    }

    private ItemStack tierItem(FileConfiguration cfg, Tier t) {
        String base = "donate.tiers." + t.id;
        String price = cfg.getString(base + ".price", "уточняйте на сайте");
        String mult = cfg.isSet("donate-multipliers." + t.id)
                ? String.format(Locale.US, "x%.2f", cfg.getDouble("donate-multipliers." + t.id))
                : "—";

        List<Component> lore = new ArrayList<>();
        lore.add(MenuItems.text("§7Цена: §e" + price));
        lore.add(MenuItems.text(""));
        lore.add(MenuItems.text("§7Перки:"));
        lore.add(MenuItems.text("§7- Множитель Эхо " + mult));
        for (String perk : cfg.getStringList(base + ".perks")) {
            lore.add(MenuItems.text("§7- " + perk));
        }
        lore.add(MenuItems.text(""));
        lore.add(MenuItems.text("§aНажми, чтобы купить!"));

        ItemStack item = new ItemStack(t.icon);
        item.editMeta(m -> {
            m.displayName(MenuItems.text("§6[Тир] " + t.title));
            m.lore(lore);
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
            return;
        }
        if (slot == SLOT_INFO) {
            p.closeInventory();
            sendLink(p, null);
            return;
        }
        for (Tier t : Tier.values()) {
            if (t.slot == slot) {
                p.closeInventory();
                sendLink(p, t);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    /** Кликабельная ссылка из donate.url. */
    private void sendLink(Player p, Tier tier) {
        String url = plugin.getConfig().getString("donate.url", "");
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            p.sendMessage(MenuItems.text("§cСсылка на донат не настроена. Обратитесь к администрации."));
            return;
        }
        String head = tier == null ? "§6Привилегии сервера §7— " : "§6[Тир] " + tier.title + " §7— ";
        Component link = Component.text("[Открыть страницу покупки]", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MenuItems.text("§7" + url)));
        p.sendMessage(MenuItems.text(head).append(link));
    }
}
