package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.api.IDataStore;

public final class PlaytimeCommand implements CommandExecutor {
    private final IDataStore store;

    public PlaytimeCommand(IDataStore store) {
        this.store = store;
    }

    public static String format(long minutes) {
        return (minutes / 60) + " ч " + (minutes % 60) + " мин";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        
        long m = store.getLong(p.getUniqueId(), PlaytimeTask.KEY, 0);
        
        p.sendMessage("§6Вы отыграли: §e" + format(m));
        p.sendMessage("§7До следующей награды (§e" + PlaytimeTask.REWARD_ECHO + " Эхо§7): §f"
                + (60 - m % 60) + " мин");
        return true;
    }
}
