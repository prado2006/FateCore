package ru.fatecore.earn;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.fatecore.FateCore;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.rotation.RotationManager;
import ru.fatecore.util.Modes;

import java.util.Set;
import java.util.stream.Collectors;

/** МОДУЛЬ 4 — расчёт множителей и начисление Эхо. */
public final class EarnService {
    private final FateCore plugin;
    private final EchoManager echo;
    private final RotationManager rotation;

    public EarnService(FateCore plugin, EchoManager echo, RotationManager rotation) {
        this.plugin = plugin;
        this.echo = echo;
        this.rotation = rotation;
    }

    /**
     * Множитель из config.yml (donate-multipliers) по группам LuckPerms игрока.
     * Только чтение групп; ничего в LuckPerms не изменяется. Нет группы — 1.0.
     */
    public double donateMultiplier(Player p) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(p.getUniqueId());
            if (user == null) return 1.0;
            Set<String> groups = user.getInheritedGroups(QueryOptions.nonContextual()).stream()
                    .map(Group::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("donate-multipliers");
            if (sec == null) return 1.0;
            double best = 1.0;
            for (String key : sec.getKeys(false)) {
                if (groups.contains(key.toLowerCase())) {
                    best = Math.max(best, sec.getDouble(key, 1.0));
                }
            }
            return best;
        } catch (IllegalStateException e) {
            return 1.0;
        }
    }

    /** Ротация режима * донат * временные бусты. */
    public double totalMultiplier(Player p, String mode) {
        return rotation.getMultiplier(mode)
                * donateMultiplier(p)
                * echo.boostMultiplier(p.getUniqueId());
    }

    /** Убийство в BedWars/SkyWars: +1 Эхо * getMultiplier * getDonateMultiplier. */
    public void addKillEcho(Player killer, String mode) {
        echo.addEcho(killer, 1.0 * totalMultiplier(killer, mode));
    }

    /** Победа в BedWars: 100 Эхо с множителями. */
    public void addBedwarsWinEcho(Player winner) {
        echo.addEcho(winner, 100.0 * totalMultiplier(winner, Modes.BEDWARS));
    }
}
