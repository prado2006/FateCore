package ru.fatecore.menu;

import fr.xephi.authme.events.LoginEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class AuthMeHook implements Listener {
    private final PlayerItemListener items;

    public AuthMeHook(PlayerItemListener items) {
        this.items = items;
    }

    @EventHandler
    public void onLogin(LoginEvent e) {
        items.giveMissingLater(e.getPlayer());
    }
}
