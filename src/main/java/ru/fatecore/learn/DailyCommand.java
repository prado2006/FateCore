package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DailyCommand implements CommandExecutor {
    private final DailyService daily;

    public DailyCommand(DailyService daily) {
        this.daily = daily;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        
        DailyService.Claim claim = daily.claim(p);
        
        // Если claim == null, значит награда еще не доступна
        if (claim == null) {
            long ms = daily.remainingMs(p.getUniqueId());
            long h = ms / 3_600_000L;
            long m = (ms / 60_000L) % 60;
            p.sendMessage("§cВы уже получали награду. Следующая через §e" + h + " ч " + m + " мин§c.");
            return true;
        }
        
        DailyService.Reward r = claim.reward();
        p.sendMessage("§a§l[✔] §eНаграда за §f" + claim.streak() + " §eдень подряд: §a+" + r.echo() + " Эхо");
        
        if (r.caseType() != DailyService.CaseType.NONE) {
            String name = r.caseType() == DailyService.CaseType.RARE ? "§dредкий" : "§fобычный";
            p.sendMessage("§a§l[✔] §eБонус: " + name + " §eкейс");
        }
        
        p.sendMessage("§7Заходите завтра — серия растёт, награда тоже. Не пропускайте больше 48 часов!");
        return true;
    }
}
