package ru.fatecore.quest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import ru.fatecore.quest.QuestManager.Quest;
import ru.fatecore.util.Modes;

/** Квест SkyWars «Убей 3 игрока»: убийство ИГРОКА в world_skywars → +1 (мобы и самоубийства не считаются). */
public final class PlayerDeathListener implements Listener {
    private final QuestManager quests;

    public PlayerDeathListener(QuestManager quests) {
        this.quests = quests;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        if (!Modes.SKYWARS.equals(Modes.of(victim.getWorld()))) return;
        quests.addProgress(killer, Quest.SKYWARS, 1);
    }
}
