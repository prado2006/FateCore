package ru.fatecore.earn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.util.Modes;

public final class EarnListener implements Listener {
    private final EarnService earn;
    private final EchoManager echo;

    public EarnListener(EarnService earn, EchoManager echo) {
        this.earn = earn;
        this.echo = echo;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        String mode = Modes.of(victim.getWorld());
        if (!Modes.BEDWARS.equals(mode) && !Modes.SKYWARS.equals(mode)) return;
        earn.addKillEcho(killer, mode);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBuild(BlockPlaceEvent e) {
        if (!Modes.SURVIVAL.equals(Modes.of(e.getBlock().getWorld()))) return;
        echo.addEcho(e.getPlayer(), 0.1);
    }
}
