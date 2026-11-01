package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.FateCore; // <--- ВОТ ЭТОТ ИМПОРТ БЫЛ ПРОПУЩЕН

public final class DonateCommand implements CommandExecutor {
    private final FateCore plugin;

    public DonateCommand(FateCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }

        String url = plugin.getConfig().getString("donate.url", "https://easyDonate.example/fatecore");
        String line = plugin.getConfig().getString("learn.donate.line", "&7Привилегии ускоряют Эхо, но не дают силы");

        p.sendMessage("§8§m--------------------------------");
        p.sendMessage("§6§lПРИВИЛЕГИИ §eЭХО СУДЬБЫ");
        p.sendMessage("§7");
        p.sendMessage(line.replace('&', '§'));
        p.sendMessage("§7");
        
        net.kyori.adventure.text.Component link = net.kyori.adventure.text.Component.text("[Открыть магазин привилегий]", net.kyori.adventure.text.format.NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.UNDERLINED)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(url))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text(url)));
        
        p.sendMessage(link);
        p.sendMessage("§8§m--------------------------------");
        
        return true;
    }
}
