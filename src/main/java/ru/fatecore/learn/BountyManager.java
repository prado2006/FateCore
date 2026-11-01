package ru.fatecore.learn;

import ru.fatecore.FateCore;
import ru.fatecore.util.YamlStore;

import java.util.UUID;

public final class BountyManager {
    private final YamlStore store;

    public BountyManager(FateCore plugin) {
        // Создаем файл bounties.yml в папке плагина
        this.store = new YamlStore(plugin, "bounties.yml");
    }

    public long get(UUID id) {
        return store.cfg().getLong("bounties." + id + ".amount", 0);
    }

    /** Добавляет к существующей награде. Возвращает общую сумму. */
    public long add(UUID id, String name, long amount) {
        long total = get(id) + amount;
        store.cfg().set("bounties." + id + ".amount", total);
        store.cfg().set("bounties." + id + ".name", name);
        store.dirty();
        return total;
    }

    /** Снимает баунти и возвращает сумму (0, если не было). */
    public long remove(UUID id) {
        long a = get(id);
        if (a > 0) {
            store.cfg().set("bounties." + id, null);
            store.dirty();
        }
        return a;
    }
}
