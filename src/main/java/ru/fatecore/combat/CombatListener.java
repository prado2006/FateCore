package ru.fatecore.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;

public final class CombatListener implements Listener {
    private static final Set<String> BLOCKED =
            Set.of("lobby", "hub", "spawn", "espawn", "home", "ehome", "homes");

    private final CombatManager combat;

    public CombatListener(CombatManager combat) {
        this.combat = combat;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        Entity d = e.getDamager();
        if (d instanceof Player pl) {
            attacker = pl;
        } else if (d instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null || attacker.equals(victim)) return;
        combat.tag(victim);
        combat.tag(attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!combat.isTagged(p.getUniqueId())) return;

        String msg = e.getMessage();
        String first = msg.substring(1).split(" ", 2)[0].toLowerCase();
        int colon = first.indexOf(':');
        if (colon >= 0) first = first.substring(colon + 1);

        if (BLOCKED.contains(first)) {
            e.setCancelled(true);
            p.sendMessage("§cВы в бою! Телепортация запрещена.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (!combat.isTagged(p.getUniqueId())) return;
        combat.untag(p.getUniqueId());
        if (Bukkit.isStopping() || p.isDead()) return;
        p.setHealth(0.0); // Убиваем, если вышли из игры во время боя
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        combat.untag(e.getEntity().getUniqueId());
    }
}
