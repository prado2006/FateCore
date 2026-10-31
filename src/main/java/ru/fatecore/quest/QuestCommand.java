package ru.fatecore.quest;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Msg;

/** /quests — открывает тот же GUI, что и клик по Квестовому Мастеру (только после /login). */
public final class QuestCommand implements CommandExecutor {
    private final QuestGui gui;

    public QuestCommand(QuestGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            Msg.send(sender, "Команда только для игроков.");
            return true;
        }
        if (!Auth.isAuthed(p)) {
            Msg.send(p, "&cСначала /login");
            return true;
        }
        gui.open(p);
        return true;
    }
}
