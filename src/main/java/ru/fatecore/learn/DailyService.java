package ru.fatecore.learn;

import org.bukkit.entity.Player;
import ru.fatecore.FateCore;
import ru.fatecore.api.IDataStore;
import ru.fatecore.api.IEchoService;
import ru.fatecore.util.Items;

import java.util.UUID;

public final class DailyService {
    public static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final String K_LAST = "daily.last";
    private static final String K_STREAK = "daily.streak";

    public enum CaseType { NONE, COMMON, RARE }
    public record Reward(long echo, CaseType caseType) {}
    public record Claim(long streak, Reward reward) {}

    private final FateCore plugin;
    private final IDataStore store;
    private final IEchoService echo;

    public DailyService(FateCore plugin, IDataStore store, IEchoService echo) {
        this.plugin = plugin;
        this.store = store;
        this.echo = echo;
    }

    public boolean isAvailable(UUID id) {
        long last = store.getLong(id, K_LAST, 0);
        return last == 0 || System.currentTimeMillis() - last >= DAY_MS;
    }

    public long remainingMs(UUID id) {
        long last = store.getLong(id, K_LAST, 0);
        if (last == 0) return 0;
        return Math.max(0, DAY_MS - (System.currentTimeMillis() - last));
    }

    public long effectiveStreak(UUID id) {
        long last = store.getLong(id, K_LAST, 0);
        if (last == 0 || System.currentTimeMillis() - last > 2 * DAY_MS) return 0;
        return store.getLong(id, K_STREAK, 0);
    }

    public static Reward rewardFor(long streak) {
        return switch ((int) Math.min(Math.max(streak, 1), 5)) {
            case 1 -> new Reward(50, CaseType.NONE);
            case 2 -> new Reward(100, CaseType.NONE);
            case 3 -> new Reward(150, CaseType.COMMON);
            case 4 -> new Reward(200, CaseType.RARE);
            default -> new Reward(250, CaseType.RARE);
        };
    }

    public Claim claim(Player p) {
        UUID id = p.getUniqueId();
        if (!isAvailable(id)) return null;

        long now = System.currentTimeMillis();
        long last = store.getLong(id, K_LAST, 0);
        long streak = (last == 0 || now - last > 2 * DAY_MS)
                ? 1
                : store.getLong(id, K_STREAK, 0) + 1;

        store.setLong(id, K_LAST, now);
        store.setLong(id, K_STREAK, streak);

        Reward r = rewardFor(streak);
        echo.give(id, r.echo());
        
        if (r.caseType() != CaseType.NONE) {
            // ВАЖНО: Здесь используются ваши классы Items и CaseItems из плагина. 
            // Если их нет, сервер выдаст ошибку. Скажите мне, если это так.
            Items.give(p, CaseItems.make(plugin, r.caseType() == CaseType.RARE));
        }
        return new Claim(streak, r);
    }
}
