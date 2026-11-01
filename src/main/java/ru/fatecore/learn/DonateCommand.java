package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin; // <-- Используем стандартный JavaPlugin

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class DonateCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public DonateCommand(JavaPlugin plugin) {
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
        
        Component link = Component.text("[Открыть магазин привилегий]", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.openUrl(url))
            .hoverEvent(HoverEvent.showText(Component.text(url)));
        
        p.sendMessage(link);
        p.sendMessage("§8§m--------------------------------");
        
        return true;
    }
}
