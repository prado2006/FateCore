package ru.fatecore.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CombatManager {
    public static final int DURATION_SEC = 15;
    private final Map<UUID, Long> expires = new HashMap<>();

    public void tag(Player p) {
        boolean first = !isTagged(p.getUniqueId());
        expires.put(p.getUniqueId(), System.currentTimeMillis() + DURATION_SEC * 1000L);
        if (first) {
            p.sendMessage("§cВы вступили в бой! Не выходите из игры §e" + DURATION_SEC + " сек§c.");
        }
    }

    public boolean isTagged(UUID id) {
        Long t = expires.get(id);
        return t != null && t > System.currentTimeMillis();
    }

    public void untag(UUID id) {
        expires.remove(id);
    }

    public void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, Long>> it = expires.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> en = it.next();
                Player p = Bukkit.getPlayer(en.getKey());
                if (p == null) {
                    it.remove();
                    continue;
                }
                int left = (int) Math.ceil((en.getValue() - System.currentTimeMillis()) / 1000.0);
                if (left > 0) {
                    p.sendActionBar(net.kyori.adventure.text.Component.text("§cВ бою: " + left + " сек."));
                } else {
                    it.remove();
                    p.sendActionBar(net.kyori.adventure.text.Component.text("§aВы вышли из боя"));
                }
            }
        }, 20L, 20L);
    }
}
