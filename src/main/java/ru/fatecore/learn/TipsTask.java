package ru.fatecore.learn;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.fatecore.FateCore;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Msg;

import java.util.List;

/**
 * Подсказки в чат раз в tips-interval-seconds (600 с), ротация по кругу из learn.tips.
 * Показываются только авторизованным игрокам. Задача отменяется в FateCore.onDisable.
 */
public final class TipsTask extends BukkitRunnable {
    private final FateCore plugin;
    private int index;

    public TipsTask(FateCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long ticks = Math.max(1, plugin.getConfig().getLong("tips-interval-seconds", 600)) * 20L;
        runTaskTimer(plugin, ticks, ticks);
    }

    @Override
    public void run() {
        List<String> tips = plugin.getConfig().getStringList("learn.tips");
        if (tips.isEmpty()) return;
        String tip = tips.get(index++ % tips.size());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Auth.isAuthed(p)) Msg.raw(p, "&e[Подсказка] &f" + tip);
        }
    }
}
