package ru.fatecore.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.fatecore.FateCore;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Msg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Порталы хаба без Multiverse-Portals: вход игрока в радиус точки из config.yml (portals.<режим>.trigger)
 * переносит его на спавн мира target-world. Права и /login проверяет и PortalListener.
 */
public final class PortalManager implements Listener {
    private record Portal(String key, String world, double x, double y, double z, double radiusSq, String target) {}

    private final FateCore plugin;
    private final List<Portal> portals = new ArrayList<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private long cooldownMs;

    public PortalManager(FateCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        portals.clear();
        cooldownMs = plugin.getConfig().getLong("portals.cooldown-seconds", 3) * 1000L;
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("portals");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null || !s.getBoolean("enabled", false)) continue;
            ConfigurationSection t = s.getConfigurationSection("trigger");
            if (t == null) continue;
            double r = t.getDouble("radius", 1.5);
            portals.add(new Portal(key, t.getString("world", "hub"),
                    t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), r * r,
                    s.getString("target-world", "")));
        }
        plugin.getLogger().info("Порталов включено: " + portals.size());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (portals.isEmpty()) return;
        Location to = e.getTo();
        if (to == null) return;
        Location from = e.getFrom();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;
        World w = to.getWorld();
        if (w == null) return;
        for (Portal p : portals) {
            if (!p.world().equals(w.getName())) continue;
            double dx = to.getX() - p.x();
            double dy = to.getY() - p.y();
            double dz = to.getZ() - p.z();
            if (dx * dx + dy * dy + dz * dz <= p.radiusSq()) {
                enter(e.getPlayer(), p);
                return;
            }
        }
    }

    private void enter(Player player, Portal p) {
        long now = System.currentTimeMillis();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < cooldownMs) return;
        lastUse.put(player.getUniqueId(), now);

        if (!Auth.isAuthed(player)) {
            Msg.send(player, "&cСначала /login");
            return;
        }
        if (!player.hasPermission("fatecore.portal.use")) {
            Msg.send(player, "&cУ тебя нет доступа к порталам.");
            return;
        }
        World target = Bukkit.getWorld(p.target());
        if (target == null) {
            Msg.send(player, "&cМир сейчас недоступен.");
            return;
        }
        player.teleport(target.getSpawnLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastUse.remove(e.getPlayer().getUniqueId());
    }
}
