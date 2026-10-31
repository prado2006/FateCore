package ru.fatecore.learn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.FateCore;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Msg;

/** /donate — кликабельная ссылка donate.url (магазин EasyDonate) + строка learn.donate.line. */
public final class DonateCommand implements CommandExecutor {
    private final FateCore plugin;

    public DonateCommand(FateCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player p && !Auth.isAuthed(p)) {
            Msg.send(p, "&cСначала /login");
            return true;
        }
        String url = plugin.getConfig().getString("donate.url", "");
        String button = plugin.getConfig().getString("learn.donate.button", "[Открыть магазин привилегий]");
        if (sender instanceof Player p) {
            p.sendMessage(Component.text(Msg.c(button), NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(Component.text(url))));
        } else {
            sender.sendMessage(url);
        }
        Msg.raw(sender, plugin.getConfig().getString("learn.donate.line",
                "&7Привилегии ускоряют Эхо, но не дают силы"));
        return true;
    }
}
