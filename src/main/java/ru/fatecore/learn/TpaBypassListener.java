package ru.fatecore.learn;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class TpaBypassListener implements Listener {
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage().toLowerCase();
        
        if (msg.startsWith("/tpa ") || msg.startsWith("/essentials:tpa ")) {
            if (p.hasPermission("fatecore.tpa.bypass")) {
                String[] args = e.getMessage().split(" ");
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target != null && target.isOnline()) {
                        p.teleport(target.getLocation());
                        p.sendMessage("§a[VIP] Вы мгновенно телепортировались к §e" + target.getName());
                        e.setCancelled(true); 
                    }
                }
            }
        }
    }
}
