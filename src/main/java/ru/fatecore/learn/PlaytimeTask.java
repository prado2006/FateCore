package ru.fatecore.learn;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.fatecore.api.IDataStore;
import ru.fatecore.api.IEchoService;

import java.util.UUID;
import java.util.function.Predicate;

public final class PlaytimeTask implements Runnable {
    public static final String KEY = "playtime.minutes";
    public static final long REWARD_ECHO = 100;

    private final IDataStore store;
    private final IEchoService echo;
    private final Predicate<Player> eligible;

    public PlaytimeTask(IDataStore store, IEchoService echo, Predicate<Player> eligible) {
        this.store = store;
        this.echo = echo;
        this.eligible = eligible;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Проверяем, подходит ли игрок (например, авторизован ли он)
            if (!eligible.test(p)) continue;
            
            UUID id = p.getUniqueId();
            long minutes = store.getLong(id, KEY, 0) + 1;
            store.setLong(id, KEY, minutes);
            
            // Каждый 60 минут (1 час)
            if (minutes % 60 == 0) {
                echo.give(id, REWARD_ECHO);
                p.sendMessage("§a§l[★] §eВы отыграли ещё час! Награда: §a+" + REWARD_ECHO + " Эхо");
            }
        }
    }
}
