package ru.fatecore.quest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.fatecore.FateCore;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Msg;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Ежедневный сброс квестов в 00:00 МСК: планирует себя на ближайшую полночь и перезапускается после срабатывания.
 * Игроки не в сети сбрасываются лениво при следующем обращении (resetQuestsIfNewDay).
 * Все задачи отменяются в FateCore.onDisable через cancelTasks.
 */
public final class DailyResetTask {
    private final FateCore plugin;
    private final QuestManager quests;

    public DailyResetTask(FateCore plugin, QuestManager quests) {
        this.plugin = plugin;
        this.quests = quests;
    }

    public void start() {
        schedule();
    }

    private void schedule() {
        ZonedDateTime now = ZonedDateTime.now(QuestManager.MSK);
        ZonedDateTime next = now.toLocalDate().plusDays(1).atStartOfDay(QuestManager.MSK);
        // минимум 1 секунда: если сработали чуть раньше полуночи, следующий запуск попадёт уже в новый день
        long ticks = Math.max(20L, Duration.between(now, next).toMillis() / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            quests.resetOnline();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (Auth.isAuthed(p)) Msg.send(p, "&aЕжедневные квесты обновились: /quests");
            }
            schedule();
        }, ticks);
    }
}
