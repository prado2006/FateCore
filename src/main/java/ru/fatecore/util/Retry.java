package ru.fatecore.util;

import org.bukkit.scheduler.BukkitRunnable;
import ru.fatecore.FateCore;

import java.util.function.BooleanSupplier;

/** Повторяет действие, пока оно не вернёт true (нужно, пока Multiverse догружает миры). */
public final class Retry {
    private Retry() {}

    public static void run(FateCore plugin, long firstDelayTicks, int attempts, BooleanSupplier action) {
        new BukkitRunnable() {
            int left = attempts;

            @Override
            public void run() {
                boolean ok = false;
                try {
                    ok = action.getAsBoolean();
                } catch (Exception e) {
                    plugin.getLogger().warning("Retry: " + e.getMessage());
                }
                if (ok || --left <= 0) cancel();
            }
        }.runTaskTimer(plugin, firstDelayTicks, 200L);
    }
}
