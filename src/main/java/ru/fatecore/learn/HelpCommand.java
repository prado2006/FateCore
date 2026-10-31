package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.fatecore.FateCore;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Cmd;
import ru.fatecore.util.Msg;

/** МОДУЛЬ 10 — кастомный /help (после /login). Перехватывает ванильный и Essentials /help. Текст: learn.help. */
public final class HelpCommand implements CommandExecutor, Listener {
    private final FateCore plugin;

    public HelpCommand(FateCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        show(sender);
        return true;
    }

    /** /help, /?, /ehelp и их варианты с префиксом plugin: — подменяем нашим списком. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreprocess(PlayerCommandPreprocessEvent e) {
        String cmd = Cmd.base(e.getMessage());
        if (cmd.equals("help") || cmd.equals("?") || cmd.equals("ehelp")) {
            e.setCancelled(true);
            show(e.getPlayer());
        }
    }

    private void show(CommandSender sender) {
        if (sender instanceof Player p && !Auth.isAuthed(p)) {
            Msg.send(p, "&cСначала /login");
            return;
        }
        Msg.raw(sender, plugin.getConfig().getString("learn.help.header", "&6=== Команды сервера ==="));
        for (String line : plugin.getConfig().getStringList("learn.help.lines")) {
            Msg.raw(sender, line);
        }
    }
}
