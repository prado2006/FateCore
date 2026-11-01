package ru.fatecore.quest;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import ru.fatecore.FateCore;
import ru.fatecore.api.IDataStore;
import ru.fatecore.classes.ClassManager;
import ru.fatecore.combat.CombatManager;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.learn.BountyManager;
import ru.fatecore.learn.DailyService;
import ru.fatecore.learn.PlaytimeCommand;
import ru.fatecore.learn.PlaytimeTask;
import ru.fatecore.perks.HomeBonus;
import ru.fatecore.rotation.RotationManager;

import java.util.UUID;

public final class FatePlaceholders extends PlaceholderExpansion {

    private final FateCore plugin;
    private final QuestManager quests;
    private final EchoManager echo;
    private final ClassManager classes;
    private final RotationManager rotation;
    private final HomeBonus homeBonus;
    private final IDataStore dataStore;
    private final DailyService daily;
    private final BountyManager bounties;
    private final CombatManager combat;

    public FatePlaceholders(FateCore plugin, QuestManager quests, EchoManager echo, ClassManager classes,
                            RotationManager rotation, HomeBonus homeBonus, IDataStore dataStore,
                            DailyService daily, BountyManager bounties, CombatManager combat) {
        this.plugin = plugin;
        this.quests = quests;
        this.echo = echo;
        this.classes = classes;
        this.rotation = rotation;
        this.homeBonus = homeBonus;
        this.dataStore = dataStore;
        this.daily = daily;
        this.bounties = bounties;
        this.combat = combat;
    }

    @Override
    public String getIdentifier() {
        return "fatecore";
    }

    @Override
    public String getAuthor() {
        return "FateCore";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        UUID id = player.getUniqueId();

        switch (params.toLowerCase()) {
            case "echo":
                return echo.format(echo.getEcho(player));
            case "playtime":
                return PlaytimeCommand.format(dataStore.getLong(id, PlaytimeTask.KEY, 0));
            case "streak":
                return String.valueOf(daily.effectiveStreak(id));
            case "bounty":
                return String.valueOf(bounties.get(id));
            case "combat":
                return player.isOnline() && combat.isTagged(id) ? "Да" : "Нет";
                
            // --- СТАРЫЕ ПЛЕЙСХОЛДЕРЫ ВРЕМЕННО ОТКЛЮЧЕНЫ ---
            // Если вы захотите их вернуть, вам нужно узнать точные названия методов 
            // в файлах ClassManager, QuestManager, RotationManager и HomeBonus.
            /*
            case "class": return classes.getClassName(id);
            case "quest_progress": return String.valueOf(quests.getProgress(id));
            case "quest_done": return String.valueOf(quests.getDoneCount(id));
            case "featured": return rotation.getMultiplierLabel();
            case "home": return homeBonus.getHomeStatus(player);
            */
            
            default:
                return null;
        }
    }
}
