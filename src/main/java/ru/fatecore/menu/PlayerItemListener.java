package ru.fatecore.menu;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import ru.fatecore.classes.ClassManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerItemListener implements Listener {

    private static final int COMPASS_SLOT = 8;
    private static final int DONATE_SLOT = 7;
    private static final long COOLDOWN_MS = 300;

    private final Plugin plugin;
    private final MenuItems items;
    private final ClassManager classes;
    private final HelpGui helpGui;
    private final DonateGui donateGui;
    private final boolean giveOnJoin;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    /**
     * @param giveOnJoin true — выдавать при входе; false — выдавать только вручную/из хука авторизации
     */
    public PlayerItemListener(Plugin plugin, MenuItems items, ClassManager classes,
                              HelpGui helpGui, DonateGui donateGui, boolean giveOnJoin) {
        this.plugin = plugin;
        this.items = items;
        this.classes = classes;
        this.helpGui = helpGui;
        this.donateGui = donateGui;
        this.giveOnJoin = giveOnJoin;
    }

    // ───────────────────────── выдача ─────────────────────────

    /** Выдаёт недостающие предметы. Возвращает false, если не хватило места. */
    public boolean giveMissing(Player p) {
        boolean noRoom = false;
        PlayerInventory inv = p.getInventory();
        for (MenuItems.Kind kind : MenuItems.Kind.values()) {
            if (items.has(p, kind)) continue;
            ItemStack stack = items.create(kind);
            int preferred = kind == MenuItems.Kind.COMPASS ? COMPASS_SLOT : DONATE_SLOT;
            ItemStack inSlot = inv.getItem(preferred);
            if (inSlot == null || inSlot.getType().isAir()) {
                inv.setItem(preferred, stack);
            } else if (inv.firstEmpty() != -1) {
                inv.addItem(stack);
            } else {
                noRoom = true;
            }
        }
        return !noRoom;
    }

    /** Выдача с задержкой + сообщение, если нет места. Используется при входе, респауне и из хука авторизации. */
    public void giveMissingLater(Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (!giveMissing(p)) {
                p.sendMessage(MenuItems.text("§cНет места в инвентаре. Освободите слот и введите §e/menu"));
            }
        }, 10L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (giveOnJoin) giveMissingLater(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cooldown.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(items::isMenuItem); // при смерти не выпадают
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        giveMissingLater(e.getPlayer());
    }

    // ───────────────────────── клики по предметам ─────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        MenuItems.Kind kind = items.kindOf(e.getItem());
        if (kind == null) return;

        Action a = e.getAction();
        boolean right = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
        boolean left = a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        Player p = e.getPlayer();

        // ПКМ по сундуку, двери и т.п. — не мешаем обычному взаимодействию (если не на шифте)
        Block b = e.getClickedBlock();
        if (right && b != null && b.getType().isInteractable() && !p.isSneaking()) return;

        // Звезда реагирует только на ПКМ
        if (kind == MenuItems.Kind.DONATE && !right) return;

        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setCancelled(true);

        long now = System.currentTimeMillis();
        Long last = cooldown.get(p.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) return; // защита от двойного срабатывания (обе руки)
        cooldown.put(p.getUniqueId(), now);

        if (kind == MenuItems.Kind.COMPASS) {
            if (right) classes.openGui(p);
            else helpGui.open(p);
        } else {
            donateGui.open(p);
        }
    }

    // ───────────────────────── защита предметов ─────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (items.isMenuItem(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory top = e.getView().getTopInventory();
        Inventory clicked = e.getClickedInventory();
        boolean clickedOwn = clicked != null && clicked == p.getInventory();
        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        ClickType click = e.getClick();

        // Предмет на курсоре нельзя класть ни в чужой инвентарь/крафт, ни выкидывать кликом вне окна
        if (items.isMenuItem(cursor) && !clickedOwn) {
            e.setCancelled(true);
            return;
        }
        // Шифт-клик перекидывает предмет в верхний инвентарь (сундук, печь, крафт и т.д.)
        if (items.isMenuItem(current) && e.isShiftClick() && top.getType() != InventoryType.CRAFTING) {
            e.setCancelled(true);
            return;
        }
        // Выброс клавишей Q / Ctrl+Q из инвентаря
        if (items.isMenuItem(current) && (click == ClickType.DROP || click == ClickType.CONTROL_DROP)) {
            e.setCancelled(true);
            return;
        }
        // Цифровая клавиша: предмет с хотбара в чужой инвентарь
        if (click == ClickType.NUMBER_KEY && !clickedOwn) {
            int btn = e.getHotbarButton();
            if (btn >= 0 && items.isMenuItem(p.getInventory().getItem(btn))) {
                e.setCancelled(true);
                return;
            }
        }
        // F (смена рук) над чужим инвентарём
        if (click == ClickType.SWAP_OFFHAND && !clickedOwn
                && items.isMenuItem(p.getInventory().getItemInOffHand())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!items.isMenuItem(e.getOldCursor())) return;
        int topSize = e.getView().getTopInventory().getSize();
        for (int raw : e.getRawSlots()) {
            if (raw < topSize) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        for (ItemStack s : e.getInventory().getMatrix()) {
            if (items.isMenuItem(s)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrame(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof ItemFrame
                && items.isMenuItem(e.getPlayer().getInventory().getItem(e.getHand()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (items.isMenuItem(e.getPlayerItem())) {
            e.setCancelled(true);
        }
    }
}
