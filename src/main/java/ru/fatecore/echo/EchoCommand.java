package ru.fatecore.echo;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.earn.EarnService;
import ru.fatecore.rotation.RotationManager;
import ru.fatecore.util.Modes;
import ru.fatecore.util.Msg;

public final class EchoCommand implements CommandExecutor {
    private final EchoManager echo;
    private final RotationManager rotation;
    private final EarnService earn;

    public EchoCommand(EchoManager echo, RotationManager rotation, EarnService earn) {
        this.echo = echo;
        this.rotation = rotation;
        this.earn = earn;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            Msg.send(sender, "Команда только для игроков.");
            return true;
        }
        Msg.send(p, "&6Эхо: &f" + echo.format(echo.getEcho(p)));
        Msg.raw(p, "&7Сегодня Эхо x2: &e" + Modes.display(rotation.getBoostedMode()));
        Msg.raw(p, "&7Донат-множитель: &fx" + earn.donateMultiplier(p)
                + " &7| Буст: &fx" + echo.format(echo.boostMultiplier(p.getUniqueId())));
        return true;
    }
}
