package ru.fatecore.rotation;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import ru.fatecore.FateCore;
import ru.fatecore.util.Modes;
import ru.fatecore.util.Msg;
import ru.fatecore.util.YamlStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** МОДУЛЬ 3 — Ротация: раз в 24 ч один из режимов получает Эхо x2. */
public final class RotationManager {
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final FateCore plugin;
    private final YamlStore store;
    private String boosted;
    private long rotatedAt;

    public RotationManager(FateCore plugin) {
        this.plugin = plugin;
        this.store = new YamlStore(plugin, "rotation.yml");
    }

    public void start() {
        boosted = store.cfg().getString("mode");
        rotatedAt = store.cfg().getLong("rotatedAt");
        if (boosted == null || !Modes.MODES.contains(boosted)
                || System.currentTimeMillis() - rotatedAt >= DAY_MS) {
            rotate(false);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - rotatedAt >= DAY_MS) rotate(true);
            }
        }.runTaskTimer(plugin, 6000L, 6000L);
    }

    private void rotate(boolean announce) {
        List<String> pool = new ArrayList<>(Modes.MODES);
        pool.remove(boosted);
        boosted = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        rotatedAt = System.currentTimeMillis();
        store.cfg().set("mode", boosted);
        store.cfg().set("rotatedAt", rotatedAt);
        store.dirty();
        if (announce) {
            Bukkit.broadcastMessage(Msg.c(Msg.PREFIX + "&aСегодня Эхо &ex2 &aв режиме &6"
                    + Modes.display(boosted) + "&a!"));
        }
    }

    public String getBoostedMode() {
        return boosted;
    }

    /** 2.0 для режима дня, иначе 1.0. Принимает только "survival"/"bedwars"/"skywars". */
    public double getMultiplier(String mode) {
        if (mode == null) return 1.0;
        String m = mode.toLowerCase();
        if (!Modes.MODES.contains(m)) return 1.0;
        return m.equals(boosted) ? 2.0 : 1.0;
    }

    public void flush() {
        store.flush();
    }
}
