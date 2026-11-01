package ru.fatecore.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.echo.EchoNotifier;

import java.util.UUID;

public class EchoServiceAdapter implements IEchoService {
    private final EchoManager echoManager;
    private final EchoNotifier notifier;

    public EchoServiceAdapter(EchoManager echoManager, EchoNotifier notifier) {
        this.echoManager = echoManager;
        this.notifier = notifier;
    }

    @Override
    public long getBalance(UUID id) {
        return (long) echoManager.getEcho(Bukkit.getOfflinePlayer(id));
    }

    @Override
    public boolean withdraw(UUID id, long amount) {
        Player p = Bukkit.getPlayer(id);
        // Ваш метод take() требует Player. Если игрок оффлайн, вернем false.
        if (p == null) return false; 
        return echoManager.take(p, (double) amount);
    }

    @Override
    public void give(UUID id, long amount) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            echoManager.addEcho(p, (int) amount);
            notifier.onEarned(p, amount, getBalance(id));
        } else {
            // Если игрок оффлайн, Vault все равно начислит ему деньги
            echoManager.addEcho(Bukkit.getOfflinePlayer(id), (double) amount);
        }
    }
}
