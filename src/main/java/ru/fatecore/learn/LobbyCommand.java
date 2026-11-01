package ru.fatecore.learn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.fatecore.combat.CombatManager;
import ru.fatecore.util.HubTeleport;

public final class LobbyCommand implements CommandExecutor {
    private final Plugin plugin;
    private final CombatManager combat;

    public LobbyCommand(Plugin plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        
        // Если игрок в бою - не пускаем
        if (combat.isTagged(p.getUniqueId())) {
            p.sendMessage("§cВы в бою! Телепортация запрещена.");
            return true;
        }
        
        // Телепортируем в хаб
        if (HubTeleport.toHub(plugin, p)) {
            p.sendMessage("§aВы вернулись в хаб.");
        } else {
            p.sendMessage("§cХаб сейчас недоступен.");
        }
        return true;
    }
}
