package ru.fatecore.echo;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.fatecore.FateCore;
import ru.fatecore.util.YamlStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** МОДУЛЬ 2 — Эхо: баланс живёт в Vault-экономике (EssentialsX), своей экономики нет. */
public final class EchoManager {
    private final Economy eco;
    private final YamlStore boosts;

    public EchoManager(FateCore plugin, Economy eco) {
        this.eco = eco;
        this.boosts = new YamlStore(plugin, "boosts.yml");
    }

    public double getEcho(OfflinePlayer p) {
        return eco.getBalance(p);
    }

    public void addEcho(Player p, int amount) {
        addEcho(p, (double) amount);
    }

    public void addEcho(Player p, double amount) {
        if (amount <= 0) return;
        eco.depositPlayer(p, amount);
    }

    /** Списывает Эхо; false, если не хватает. */
    public boolean take(Player p, double amount) {
        if (!eco.has(p, amount)) return false;
        return eco.withdrawPlayer(p, amount).transactionSuccess();
    }

    public String format(double v) {
        return String.format(Locale.US, v == Math.floor(v) ? "%.0f" : "%.1f", v);
    }

    // ---- временные множители Эхо (формат записи "множитель:до_мс") ----

    public void addBoost(UUID id, double multiplier, long seconds) {
        List<String> list = active(id);
        list.add(multiplier + ":" + (System.currentTimeMillis() + seconds * 1000L));
        boosts.cfg().set(id.toString(), list);
        boosts.dirty();
    }

    public double boostMultiplier(UUID id) {
        double total = 1.0;
        for (String entry : active(id)) {
            total *= Double.parseDouble(entry.split(":")[0]);
        }
        return total;
    }

    private List<String> active(UUID id) {
        long now = System.currentTimeMillis();
        List<String> all = boosts.cfg().getStringList(id.toString());
        List<String> alive = new ArrayList<>();
        for (String entry : all) {
            String[] parts = entry.split(":");
            if (parts.length == 2 && Long.parseLong(parts[1]) > now) alive.add(entry);
        }
        if (alive.size() != all.size()) {
            boosts.cfg().set(id.toString(), alive.isEmpty() ? null : alive);
            boosts.dirty();
        }
        return alive;
    }

    public void flush() {
        boosts.flush();
    }
}
