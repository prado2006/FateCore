package ru.fatecore.classes;

import fr.xephi.authme.events.LoginEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.fatecore.FateCore;

/** После /login предлагает выбрать класс, если он ещё не выбран. */
public final class ClassListener implements Listener {
    private final FateCore plugin;
    private final ClassManager classes;

    public ClassListener(FateCore plugin, ClassManager classes) {
        this.plugin = plugin;
        this.classes = classes;
    }

    @EventHandler
    public void onLogin(LoginEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && classes.getClass(p.getUniqueId()) == null) {
                classes.openGui(p);
            }
        }, 20L);
    }
}
