package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.fatecore.FateCore;

import java.util.List;

public final class RulesCommand implements CommandExecutor {
    private final FateCore plugin;

    public RulesCommand(FateCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> rules = plugin.getConfig().getStringList("learn.rules");
        
        sender.sendMessage("§8§m--------------------------------");
        sender.sendMessage("§6§lПРАВИЛА СЕРВЕРА §eЭХО СУДЬБЫ");
        sender.sendMessage("§7");
        
        if (rules.isEmpty()) {
            // Если правила в конфиге пусты, показываем базовые (без ошибок конфига)
            sender.sendMessage("§61. §fБез читов и запрещённых модов.");
            sender.sendMessage("§62. §fБез гриферства: чужие постройки не трогаем.");
            sender.sendMessage("§63. §fБез спама и флуда в чате.");
            sender.sendMessage("§64. §fУважай других игроков и администрацию.");
            sender.sendMessage("§65. §fДонат не даёт силы — только ускоряет Эхо.");
        } else {
            for (String rule : rules) {
                sender.sendMessage(rule.replace('&', '§'));
            }
        }
        
        sender.sendMessage("§8§m--------------------------------");
        return true;
    }
}
