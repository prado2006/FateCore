package ru.fatecore.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class HubTeleport {
    private HubTeleport() {}

    public static boolean toHub(Plugin plugin, Player p) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("onboarding.spawn");
        World w = Bukkit.getWorld(s != null ? s.getString("world", "hub") : "hub");
        if (w == null) return false;
        
        Location loc = s == null
                ? w.getSpawnLocation()
                : new Location(w, s.getDouble("x") + 0.5, s.getDouble("y"), s.getDouble("z") + 0.5,
                        (float) s.getDouble("yaw", 0), (float) s.getDouble("pitch", 0));
                        
        p.setFallDistance(0f);
        p.setVelocity(new Vector(0, 0, 0));
        return p.teleport(loc);
    }
}
