package ru.fatecore.learn;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import ru.fatecore.api.IEchoService;

public final class BountyListener implements Listener {
    private final Plugin plugin;
    private final BountyManager bounties;
    private final IEchoService echo;
    private final boolean notifyOnJoin;

    /** notifyOnJoin = false, если уведомление шлётся из AuthMe (после /login). */
    public BountyListener(Plugin plugin, BountyManager bounties, IEchoService echo, boolean notifyOnJoin) {
        this.plugin = plugin;
        this.bounties = bounties;
        this.echo = echo;
        this.notifyOnJoin = notifyOnJoin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        
        // Если убийцы нет (упал в лаву/умер сам) или убил сам себя
        if (killer == null || killer.equals(victim)) return;

        long amount = bounties.get(victim.getUniqueId());
        if (amount <= 0) return;

        // Забираем баунти и отдаем убийце
        bounties.remove(victim.getUniqueId());
        echo.give(killer.getUniqueId(), amount);
        
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[Баунти] §eИгрок " + killer.getName() + " убил игрока "
                + victim.getName() + " и получил " + amount + " Эхо!"));
    }

    public void notifyBounty(Player p) {
        if (!p.isOnline()) return;
        long amount = bounties.get(p.getUniqueId());
        if (amount > 0) {
            p.sendMessage("§c§l[!] §cЗа вашу голову назначена награда: §e" + amount + " Эхо§c. Будьте осторожны!");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!notifyOnJoin) return;
        Player p = e.getPlayer();
        // Задержка в 2 секунды (40 тиков), чтобы сообщение пришло после приветствия
        Bukkit.getScheduler().runTaskLater(plugin, () -> notifyBounty(p), 40L);
    }
}
