package ru.fatecore.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VoidListener implements Listener {
    private final Plugin plugin;
    private final Set<String> worlds;
    private final double minY;
    private final Set<UUID> pending = new HashSet<>();

    public VoidListener(Plugin plugin) {
        this.plugin = plugin;
        List<String> list = plugin.getConfig().getStringList("void-protection.worlds");
        this.worlds = new HashSet<>(list.isEmpty() ? List.of("hub") : list);
        this.minY = plugin.getConfig().getDouble("void-protection.min-y", 0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();
        if (to == null || to.getY() >= minY) return;
        Player p = e.getPlayer();
        if (!worlds.contains(p.getWorld().getName())) return;
        if (!pending.add(p.getUniqueId())) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pending.remove(p.getUniqueId());
            if (!p.isOnline()) return;
            if (HubTeleport.toHub(plugin, p)) {
                p.sendMessage("§cВы упали в пустоту и вернулись в хаб!");
            }
        });
    }
}
