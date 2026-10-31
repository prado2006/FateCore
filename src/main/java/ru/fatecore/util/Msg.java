package ru.fatecore.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Msg {
    public static final String PREFIX = "&6[FateCore] &r";

    private Msg() {}

    public static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void send(CommandSender to, String s) {
        to.sendMessage(c(PREFIX + s));
    }

    public static void raw(CommandSender to, String s) {
        to.sendMessage(c(s));
    }
}
