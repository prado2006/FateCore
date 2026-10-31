package ru.fatecore.onboarding;

import fr.xephi.authme.events.LoginEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import ru.fatecore.FateCore;
import ru.fatecore.util.ConfigUtil;
import ru.fatecore.util.Items;
import ru.fatecore.util.Msg;
import ru.fatecore.util.Retry;

import java.util.List;

/**
 * МОДУЛЬ 5 — Онбординг. Всё содержимое берётся из config.yml (onboarding.*):
 *  - book    — книга-гайд, выдаётся при первом входе;
 *  - welcome — приветствие в чат после /login;
 *  - signs   — таблички хаба (включаются enabled: true после замены координат).
 */
public final class OnboardingListener implements Listener {
    private final FateCore plugin;

    public OnboardingListener(FateCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.hasPlayedBefore()) return;
        ItemStack book = guideBook();
        if (book != null) Items.give(p, book);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Location spawn = ConfigUtil.location(plugin.getConfig().getConfigurationSection("onboarding.spawn"));
            if (spawn != null) p.teleport(spawn);
        }, 5L);
    }

    /** Приветствие в чате после входа (/login или авто-логин после /register). */
    @EventHandler
    public void onLogin(LoginEvent e) {
        Player p = e.getPlayer();
        for (String line : plugin.getConfig().getStringList("onboarding.welcome")) {
            Msg.raw(p, line.replace("%player%", p.getName()));
        }
    }

    /** Книга из onboarding.book; null, если страницы не заданы. */
    public ItemStack guideBook() {
        ConfigurationSection b = plugin.getConfig().getConfigurationSection("onboarding.book");
        if (b == null) return null;
        List<String> pages = b.getStringList("pages");
        if (pages.isEmpty()) return null;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(b.getString("title", "Гид новичка"));
        meta.setAuthor(b.getString("author", "FateCore"));
        meta.setPages(pages.stream().map(Msg::c).toList());
        book.setItemMeta(meta);
        return book;
    }

    /** Ставит таблички из onboarding.signs.* с enabled: true; ждёт загрузки мира хаба. */
    public void placeSigns() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("onboarding.signs");
        if (root == null) return;
        Retry.run(plugin, 100L, 10, () -> {
            boolean allDone = true;
            for (String key : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null || !s.getBoolean("enabled", false)) continue;
                Location loc = ConfigUtil.location(s.getConfigurationSection("location"));
                if (loc == null) {
                    allDone = false;
                    continue;
                }
                placeSign(loc, s.getStringList("lines"));
            }
            return allDone;
        });
    }

    private void placeSign(Location loc, List<String> lines) {
        Block b = loc.getBlock();
        if (!(b.getState() instanceof Sign)) b.setType(Material.OAK_SIGN, false);
        if (b.getState() instanceof Sign sign) {
            SignSide side = sign.getSide(Side.FRONT);
            for (int i = 0; i < 4; i++) {
                side.setLine(i, i < lines.size() ? Msg.c(lines.get(i)) : "");
            }
            sign.update();
        }
    }
}
