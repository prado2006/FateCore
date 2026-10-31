package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Msg;

/** /shop — GUI магазина за Эхо (только после /login). */
public final class ShopCommand implements CommandExecutor {
    private final ShopGui shop;

    public ShopCommand(ShopGui shop) {
        this.shop = shop;
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
        shop.openMain(p);
        return true;
    }
}
