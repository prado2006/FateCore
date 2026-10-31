package ru.fatecore.portal;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Modes;
import ru.fatecore.util.Msg;

/** МОДУЛЬ 7 — Портал: ТП в Survival/BedWars/SkyWars только после /login и с fatecore.portal.use. */
public final class PortalListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo() == null) return;
        World to = e.getTo().getWorld();
        if (Modes.of(to) == null) return;
        if (e.getFrom().getWorld() == to) return;

        Player p = e.getPlayer();
        if (!Auth.isAuthed(p)) {
            e.setCancelled(true);
            Msg.send(p, "&cСначала /login");
            return;
        }
        if (!p.hasPermission("fatecore.portal.use")) {
            e.setCancelled(true);
            Msg.send(p, "&cУ тебя нет доступа к порталам.");
        }
    }
}
