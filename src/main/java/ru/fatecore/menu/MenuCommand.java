package ru.fatecore.menu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MenuCommand implements CommandExecutor {
    private final PlayerItemListener listener;

    public MenuCommand(PlayerItemListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (listener.giveMissing(p)) {
            p.sendMessage(MenuItems.text("§aПредметы меню на месте."));
        } else {
            p.sendMessage(MenuItems.text("§cНет места в инвентаре. Освободите слот и повторите §e/menu"));
        }
        return true;
    }
}
