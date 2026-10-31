package ru.fatecore.quest;

import com.earth2me.essentials.IEssentials;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.fatecore.FateCore;
import ru.fatecore.classes.ClassManager;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.perks.HomeBonus;
import ru.fatecore.rotation.RotationManager;
import ru.fatecore.util.Modes;

/**
 * PlaceholderAPI, идентификатор fatecore:
 *   %fatecore_class%           — класс игрока
 *   %fatecore_echo%            — баланс Эхо
 *   %fatecore_quest_progress%  — прогресс квестов («Survival 0/32, BedWars 0/1, SkyWars 0/3»)
 *   %fatecore_quest_done%      — выполнено квестов за день («1/3»)
 *   %fatecore_featured%        — режим дня с Эхо x2
 *   %fatecore_home%            — домов установлено / лимит
 */
public final class FatePlaceholders extends PlaceholderExpansion {
    private final FateCore plugin;
    private final QuestManager quests;
    private final EchoManager echo;
    private final ClassManager classes;
    private final RotationManager rotation;
    private final HomeBonus homes;

    public FatePlaceholders(FateCore plugin, QuestManager quests, EchoManager echo, ClassManager classes,
                            RotationManager rotation, HomeBonus homes) {
        this.plugin = plugin;
        this.quests = quests;
        this.echo = echo;
        this.classes = classes;
        this.rotation = rotation;
        this.homes = homes;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "fatecore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "FateCore";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        switch (params.toLowerCase()) {
            case "quest_progress":
                return quests.progressString(player.getUniqueId());
            case "quest_done":
                return quests.dayCompleted(player.getUniqueId()) + "/3";
            case "echo":
                return echo.format(echo.getEcho(player));
            case "class":
                return classes.getTitle(player.getUniqueId());
            case "featured":
                return Modes.display(rotation.getBoostedMode()) + " x2";
            case "home":
                return homeInfo(player.getPlayer());
            default:
                return null;
        }
    }

    private String homeInfo(Player p) {
        if (p == null) return "-";
        int count = 0;
        try {
            if (Bukkit.getPluginManager().getPlugin("Essentials") instanceof IEssentials ess) {
                var user = ess.getUser(p);
                if (user != null) count = user.getHomes().size();
            }
        } catch (Exception ignored) {
            // Essentials недоступен — показываем 0
        }
        return count + "/" + homes.limit(p);
    }
}
