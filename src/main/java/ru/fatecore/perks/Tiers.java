package ru.fatecore.perks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/** Только чтение групп LuckPerms: какие группы (с наследованием) есть у игрока. Ничего не изменяет. */
public final class Tiers {
    private Tiers() {}

    public static Set<String> groups(Player p) {
        Set<String> out = new HashSet<>();
        out.add("default");
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(p.getUniqueId());
            if (user != null) {
                for (Group g : user.getInheritedGroups(QueryOptions.nonContextual())) {
                    out.add(g.getName().toLowerCase());
                }
            }
        } catch (IllegalStateException ignored) {
            // LuckPerms ещё не готов — остаётся только default
        }
        return out;
    }

    /** Минимум значений секции по группам игрока; fallback, если ни одна группа не найдена. */
    public static int min(Set<String> groups, ConfigurationSection sec, int fallback) {
        if (sec == null) return fallback;
        Integer best = null;
        for (String key : sec.getKeys(false)) {
            if (groups.contains(key.toLowerCase())) {
                int v = sec.getInt(key);
                if (best == null || v < best) best = v;
            }
        }
        return best == null ? fallback : best;
    }

    /** Максимум значений секции по группам игрока; fallback, если ни одна группа не найдена. */
    public static int max(Set<String> groups, ConfigurationSection sec, int fallback) {
        if (sec == null) return fallback;
        Integer best = null;
        for (String key : sec.getKeys(false)) {
            if (groups.contains(key.toLowerCase())) {
                int v = sec.getInt(key);
                if (best == null || v > best) best = v;
            }
        }
        return best == null ? fallback : best;
    }
}
