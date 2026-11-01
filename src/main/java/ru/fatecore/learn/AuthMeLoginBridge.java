package ru.fatecore.learn;

import fr.xephi.authme.events.LoginEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.Consumer;

public final class AuthMeLoginBridge implements Listener {
    private final Consumer<Player> afterLogin;

    public AuthMeLoginBridge(Consumer<Player> afterLogin) {
        this.afterLogin = afterLogin;
    }

    @EventHandler
    public void onLogin(LoginEvent e) {
        afterLogin.accept(e.getPlayer());
    }
}
