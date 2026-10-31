package ru.fatecore.perks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.fatecore.FateCore;

import java.time.Duration;
import java.time.Instant;

/**
 * «+1 дом на 7 дней» из /shop: игроку на 7 дней выдаётся ВРЕМЕННОЕ ПРАВО (не группа)
 * essentials.sethome.multiple.<ключ>, где ключ в config home-limits равен текущему лимиту + 1.
 * Essentials берёт наибольший лимит из прав. Донат-группы не затрагиваются.
 */
public final class HomeBonus {
    private final FateCore plugin;

    public HomeBonus(FateCore plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection limits() {
        return plugin.getConfig().getConfigurationSection("home-limits");
    }

    /** Текущий лимит домов игрока по правам essentials.sethome.multiple.<ключ> и home-limits. */
    public int limit(Player p) {
        return current(p);
    }

    private int current(Player p) {
        ConfigurationSection s = limits();
        if (s == null) return 0;
        int best = 0;
        for (String key : s.getKeys(false)) {
            if (p.hasPermission("essentials.sethome.multiple." + key)) best = Math.max(best, s.getInt(key));
        }
        return best;
    }

    private String keyFor(int value) {
        ConfigurationSection s = limits();
        if (s == null) return null;
        for (String key : s.getKeys(false)) {
            if (s.getInt(key) == value) return key;
        }
        return null;
    }

    public boolean canGrant(Player p) {
        int cur = current(p);
        return cur > 0 && keyFor(cur + 1) != null;
    }

    public boolean grant(Player p) {
        int cur = current(p);
        String key = cur > 0 ? keyFor(cur + 1) : null;
        if (key == null) return false;
        long days = plugin.getConfig().getLong("home-bonus-days", 7);
        LuckPerms lp = LuckPermsProvider.get();
        // Только права, не группы. Если в вашей версии API нет expiry(Instant) — замените на expiry(days, TimeUnit.DAYS).
        Node node = Node.builder("essentials.sethome.multiple." + key)
                .expiry(Instant.now().plus(Duration.ofDays(days)))
                .build();
        lp.getUserManager().modifyUser(p.getUniqueId(), user -> user.data().add(node));
        return true;
    }
}
