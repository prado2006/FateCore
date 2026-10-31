package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.FateCore;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Msg;

/** /rules — 5 строк из learn.rules (только после /login). */
public final class RulesCommand implements CommandExecutor {
    private final FateCore plugin;

    public RulesCommand(FateCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player p && !Auth.isAuthed(p)) {
            Msg.send(p, "&cСначала /login");
            return true;
        }
        Msg.raw(sender, "&6=== Правила ===");
        for (String line : plugin.getConfig().getStringList("learn.rules")) {
            Msg.raw(sender, "&f" + line);
        }
        return true;
    }
}
