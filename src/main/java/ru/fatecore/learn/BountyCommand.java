package ru.fatecore.learn;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.api.IEchoService;

import java.util.UUID;

public final class BountyCommand implements CommandExecutor {
    private static final long MIN_AMOUNT = 10;

    private final IEchoService echo;
    private final BountyManager bounties;

    public BountyCommand(IEchoService echo, BountyManager bounties) {
        this.echo = echo;
        this.bounties = bounties;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (args.length < 2) {
            p.sendMessage("§7Использование: §e/bounty <ник> <сумма>");
            return true;
        }

        UUID targetId;
        String targetName;
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online != null) {
            targetId = online.getUniqueId();
            targetName = online.getName();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(args[0]);
            if (off == null || !off.hasPlayedBefore()) {
                p.sendMessage("§cИгрок не найден.");
                return true;
            }
            targetId = off.getUniqueId();
            targetName = off.getName() != null ? off.getName() : args[0];
        }
        
        if (targetId.equals(p.getUniqueId())) {
            p.sendMessage("§cНельзя назначить награду за свою голову.");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            p.sendMessage("§cСумма должна быть целым числом.");
            return true;
        }
        
        if (amount < MIN_AMOUNT) {
            p.sendMessage("§cМинимальная награда — §e" + MIN_AMOUNT + " Эхо§c.");
            return true;
        }
        
        if (!echo.withdraw(p.getUniqueId(), amount)) {
            p.sendMessage("§cНедостаточно Эхо. Ваш баланс: §e" + echo.getBalance(p.getUniqueId()));
            return true;
        }

        long total = bounties.add(targetId, targetName, amount);
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[Баунти] §eИгрок " + p.getName() + " назначил награду §c" + amount
                + " Эхо §eза голову игрока " + targetName + "!"));
        p.sendMessage("§7Общая награда за голову §f" + targetName + "§7: §e" + total + " Эхо");
        return true;
    }
}
