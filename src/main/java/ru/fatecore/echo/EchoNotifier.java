package ru.fatecore.echo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.fatecore.util.Text; // Убедитесь, что класс Text есть, или замените на вашу утилиту

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EchoNotifier implements Listener {
    private static final int HINT_EVERY = 5;
    private final Map<UUID, Integer> counters = new HashMap<>();

    public void onEarned(Player p, long amount, long balanceAfter) {
        // Если у вас нет класса Text, используйте Component.text(...)
        p.sendActionBar(net.kyori.adventure.text.Component.text("§a+" + amount + " Эхо §7| §eБаланс: " + balanceAfter));
        int n = counters.merge(p.getUniqueId(), 1, Integer::sum);
        if (n % HINT_EVERY == 0) {
            p.sendMessage("§7Эхо — валюта сервера. Тратить в §e/shop");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        counters.remove(e.getPlayer().getUniqueId());
    }
}
