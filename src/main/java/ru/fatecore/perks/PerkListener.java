package ru.fatecore.perks;

import com.earth2me.essentials.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.fatecore.FateCore;
import ru.fatecore.util.Cmd;
import ru.fatecore.util.Modes;
import ru.fatecore.util.Msg;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * МОДУЛЬ 9 — донат-перки и киты не работают на аренах и в бою.
 * Сами команды выполняет EssentialsX; здесь только блокировка (дублирует blocked-cmds WorldGuard).
 */
public final class PerkListener implements Listener {
    /** Блок на аренах BedWars/SkyWars. */
    private static final Set<String> ARENA_BLOCKED = Set.of(
            "fly", "efly", "god", "egod", "tgod",
            "speed", "espeed", "flyspeed", "eflyspeed", "walkspeed", "ewalkspeed",
            "feed", "efeed", "eat", "eeat",
            "heal", "eheal",
            "repair", "erepair", "fix", "efix",
            "back", "eback", "return", "ereturn",
            "kit", "ekit", "kits", "ekits",
            "tpa", "etpa", "call", "ecall", "tpask", "etpask",
            "tpahere", "etpahere");

    /** Блок при PvP (урон за последние N секунд) в любом мире. */
    private static final Set<String> COMBAT_BLOCKED = Set.of(
            "fly", "efly", "god", "egod", "tgod",
            "speed", "espeed", "flyspeed", "eflyspeed", "walkspeed", "ewalkspeed",
            "kit", "ekit",
            "tpa", "etpa", "call", "ecall", "tpask", "etpask",
            "tpahere", "etpahere");

    private final FateCore plugin;
    private final Map<UUID, Long> lastCombat = new HashMap<>();

    public PerkListener(FateCore plugin) {
        this.plugin = plugin;
    }

    private boolean inCombat(Player p) {
        Long t = lastCombat.get(p.getUniqueId());
        long window = plugin.getConfig().getLong("perks.combat-seconds", 10) * 1000L;
        return t != null && System.currentTimeMillis() - t < window;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String cmd = Cmd.base(e.getMessage());
        if (cmd.isEmpty()) return;
        Player p = e.getPlayer();

        if (ARENA_BLOCKED.contains(cmd) && Modes.isArena(p.getWorld())) {
            e.setCancelled(true);
            Msg.send(p, "&cНедоступно на аренах");
            return;
        }
        if (COMBAT_BLOCKED.contains(cmd) && inCombat(p)) {
            e.setCancelled(true);
            Msg.send(p, "&cНедоступно в бою");
        }
    }

    /** Фиксируем PvP и снимаем полёт/god с обоих участников. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        Entity d = e.getDamager();
        if (d instanceof Player pl) {
            attacker = pl;
        } else if (d instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null || attacker.equals(victim)) return;
        long now = System.currentTimeMillis();
        lastCombat.put(victim.getUniqueId(), now);
        lastCombat.put(attacker.getUniqueId(), now);
        stripPerks(victim);
        stripPerks(attacker);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (Modes.isArena(e.getPlayer().getWorld())) stripPerks(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastCombat.remove(e.getPlayer().getUniqueId());
    }

    /** Выключает полёт и режим бога (god — через IEssentials). */
    private void stripPerks(Player p) {
        GameMode gm = p.getGameMode();
        if (gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE) {
            if (p.getAllowFlight() || p.isFlying()) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
        }
        try {
            if (Bukkit.getPluginManager().getPlugin("Essentials") instanceof IEssentials ess) {
                var user = ess.getUser(p);
                if (user != null && user.isGodModeEnabled()) {
                    user.setGodModeEnabled(false);
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось снять god: " + ex.getMessage());
        }
    }
}
