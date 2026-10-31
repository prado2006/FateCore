package ru.fatecore.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class ConfigUtil {
    private ConfigUtil() {}

    /** Читает {world, x, y, z[, yaw, pitch]}. Возвращает null, если мир не загружен. */
    public static Location location(ConfigurationSection s) {
        if (s == null) return null;
        World w = Bukkit.getWorld(s.getString("world", "hub"));
        if (w == null) return null;
        return new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }
}
