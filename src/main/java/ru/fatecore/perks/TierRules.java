package ru.fatecore.perks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.fatecore.FateCore;
import ru.fatecore.util.Cmd;
import ru.fatecore.util.Msg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Правила по тирам, которых нет в EssentialsX:
 *  - КД /sethome по тирам (config: sethome-tiers);
 *  - КД /back 1/час, без КД у групп из back-no-cooldown;
 *  - потолок /speed по тирам (config: speed-tiers).
 * Регистрируется ПОСЛЕ PerkListener, чтобы не ставить КД на уже отменённую команду.
 */
public final class TierRules implements Listener {
    private static final Set<String> SETHOME = Set.of("sethome", "esethome", "createhome", "ecreatehome");
    private static final Set<String> BACK = Set.of("back", "eback", "return", "ereturn");
    private static final Set<String> SPEED = Set.of("speed", "espeed", "walkspeed", "ewalkspeed", "flyspeed", "eflyspeed");

    private final FateCore plugin;
    private final Map<UUID, Long> sethomeLast = new HashMap<>();
    private final Map<UUID, Long> backLast = new HashMap<>();

    public TierRules(FateCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String cmd = Cmd.base(e.getMessage());
        if (cmd.isEmpty()) return;
        Player p = e.getPlayer();

        if (SETHOME.contains(cmd)) {
            if (!p.hasPermission("essentials.sethome")) return;
            int seconds = Tiers.min(Tiers.groups(p), plugin.getConfig().getConfigurationSection("sethome-tiers"), 30);
            gate(e, p, sethomeLast, seconds, "/sethome");
        } else if (BACK.contains(cmd)) {
            if (!p.hasPermission("essentials.back")) return;
            Set<String> groups = Tiers.groups(p);
            List<String> free = plugin.getConfig().getStringList("back-no-cooldown");
            for (String g : free) {
                if (groups.contains(g.toLowerCase())) return;
            }
            gate(e, p, backLast, plugin.getConfig().getInt("back-cooldown-seconds", 3600), "/back");
        } else if (SPEED.contains(cmd)) {
            if (!p.hasPermission("essentials.speed")) return;
            Double level = firstNumber(Cmd.args(e.getMessage()));
            if (level == null) return;
            int max = Tiers.max(Tiers.groups(p), plugin.getConfig().getConfigurationSection("speed-tiers"), 0);
            if (level > max) {
                e.setCancelled(true);
                Msg.send(p, max <= 0
                        ? "&cТебе недоступна команда /speed."
                        : "&cТвой максимум /speed: " + max);
            }
        }
    }

    private void gate(PlayerCommandPreprocessEvent e, Player p, Map<UUID, Long> last, int seconds, String name) {
        if (seconds <= 0) return;
        long now = System.currentTimeMillis();
        Long t = last.get(p.getUniqueId());
        if (t != null && now - t < seconds * 1000L) {
            long left = (seconds * 1000L - (now - t) + 999) / 1000;
            e.setCancelled(true);
            Msg.send(p, "&c" + name + " будет доступна через " + left + " с.");
            return;
        }
        last.put(p.getUniqueId(), now);
    }

    private static Double firstNumber(String[] args) {
        for (String a : args) {
            try {
                return Double.parseDouble(a.replace(',', '.'));
            } catch (NumberFormatException ignored) {
                // не число — идём дальше (walk / fly / ник)
            }
        }
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        sethomeLast.remove(e.getPlayer().getUniqueId());
        backLast.remove(e.getPlayer().getUniqueId());
    }
}
