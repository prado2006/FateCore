package ru.fatecore.learn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class DailyListener implements Listener {
    private final Plugin plugin;
    private final DailyService daily;
    private final boolean notifyOnJoin;

    /** notifyOnJoin = false, если уведомление шлётся из AuthMe (после /login). */
    public DailyListener(Plugin plugin, DailyService daily, boolean notifyOnJoin) {
        this.plugin = plugin;
        this.daily = daily;
        this.notifyOnJoin = notifyOnJoin;
    }

    public void notifyIfAvailable(Player p) {
        if (p.isOnline() && daily.isAvailable(p.getUniqueId())) {
            p.sendMessage("§a§l[!] §eВаша ежедневная награда доступна! §fВведите §e/daily");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!notifyOnJoin) return;
        Player p = e.getPlayer();
        // Задержка в 2 секунды (40 тиков), чтобы сообщение пришло после всех приветствий
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> notifyIfAvailable(p), 40L);
    }
}
