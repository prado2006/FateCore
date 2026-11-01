package ru.fatecore.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import ru.fatecore.classes.ClassManager;
import ru.fatecore.combat.CombatManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerItemListener implements Listener {

    private static final int COMPASS_SLOT = 0;
    private static final int DONATE_SLOT = 1;
    private static final long COOLDOWN_MS = 300;

    private final Plugin plugin;
    private final MenuItems items;
    private final ClassManager classes;
    private final HelpGui helpGui;
    private final DonateGui donateGui;
    private final MainMenuGui mainMenuGui;
    private final CombatManager combat;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public PlayerItemListener(Plugin plugin, MenuItems items, ClassManager classes, HelpGui helpGui, DonateGui donateGui, MainMenuGui mainMenuGui, CombatManager combat) {
        this.plugin = plugin;
        this.items = items;
        this.classes = classes;
        this.helpGui = helpGui;
        this.donateGui = donateGui;
        this.mainMenuGui = mainMenuGui;
        this.combat = combat;
    }

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

    public void giveMissingLater(Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !giveMissing(p)) {
                p.sendMessage(MenuItems.text("&cНет места в инвентаре. Освободите слот и введите &e/menu"));
            }
        }, 10L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        giveMissingLater(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        giveMissingLater(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        giveMissingLater(e.getPlayer());
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        e.getDrops().removeIf(items::isMenuItem);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        MenuItems.Kind kind = items.kindOf(e.getItem());
        if (kind == null) return;

        Action a = e.getAction();
        boolean right = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
        boolean left = a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        Player p = e.getPlayer();
        
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setCancelled(true);

        long now = System.currentTimeMillis();
        Long last = cooldown.get(p.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) return;
        cooldown.put(p.getUniqueId(), now);

        if (kind == MenuItems.Kind.COMPASS) {
            if (right) {
                if (combat.isTagged(p.getUniqueId())) {
                    p.sendMessage(MenuItems.text("&cВы в бою! Меню заблокировано."));
                    return;
                }
                mainMenuGui.open(p);
            } else {
                helpGui.open(p);
            }
        } else {
            donateGui.open(p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (items.isMenuItem(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        if (items.isMenuItem(current) || items.isMenuItem(cursor)) {
            if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getWhoClicked().getInventory())) {
                e.setCancelled(true);
            }
        }
    }
}
