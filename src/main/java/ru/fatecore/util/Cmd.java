package ru.fatecore.util;

import java.util.Arrays;

/** Разбор строки команды из PlayerCommandPreprocessEvent. */
public final class Cmd {
    private Cmd() {}

    /** Имя команды в нижнем регистре без слэша и без префикса "plugin:". Пусто, если команды нет. */
    public static String base(String message) {
        String m = message.trim();
        if (m.startsWith("/")) m = m.substring(1);
        if (m.isEmpty()) return "";
        String first = m.split("\\s+")[0].toLowerCase();
        int colon = first.lastIndexOf(':');
        return colon >= 0 ? first.substring(colon + 1) : first;
    }

    /** Аргументы после имени команды. */
    public static String[] args(String message) {
        String m = message.trim();
        if (m.startsWith("/")) m = m.substring(1);
        String[] parts = m.split("\\s+");
        return parts.length <= 1 ? new String[0] : Arrays.copyOfRange(parts, 1, parts.length);
    }
}
