package ru.fatecore.util;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.entity.Player;

public final class Auth {
    private Auth() {}

    public static boolean isAuthed(Player p) {
        return AuthMeApi.getInstance().isAuthenticated(p);
    }
}
