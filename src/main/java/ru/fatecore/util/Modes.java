package ru.fatecore.util;

import org.bukkit.World;

import java.util.List;

public final class Modes {
    public static final String HUB = "hub";
    public static final String SURVIVAL_WORLD = "world_survival";
    public static final String BEDWARS_WORLD = "world_bedwars";
    public static final String SKYWARS_WORLD = "world_skywars";

    public static final String SURVIVAL = "survival";
    public static final String BEDWARS = "bedwars";
    public static final String SKYWARS = "skywars";

    public static final List<String> MODES = List.of(SURVIVAL, BEDWARS, SKYWARS);

    private Modes() {}

    /** Код режима ("survival"/"bedwars"/"skywars") по миру или null. */
    public static String of(World w) {
        if (w == null) return null;
        return switch (w.getName()) {
            case SURVIVAL_WORLD -> SURVIVAL;
            case BEDWARS_WORLD -> BEDWARS;
            case SKYWARS_WORLD -> SKYWARS;
            default -> null;
        };
    }

    public static boolean isArena(World w) {
        String m = of(w);
        return BEDWARS.equals(m) || SKYWARS.equals(m);
    }

    /** Название режима для игрока. */
    public static String display(String mode) {
        if (mode == null) return "";
        return switch (mode.toLowerCase()) {
            case SURVIVAL -> "Survival";
            case BEDWARS -> "BedWars";
            case SKYWARS -> "SkyWars";
            default -> mode;
        };
    }
}
