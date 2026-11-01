package ru.fatecore.api;

import ru.fatecore.FateCore;
import ru.fatecore.util.YamlStore;

import java.util.UUID;

public class PlayerDataStore implements IDataStore {
    private final YamlStore store;

    public PlayerDataStore(FateCore plugin) {
        this.store = new YamlStore(plugin, "players.yml");
    }

    @Override
    public long getLong(UUID id, String key, long def) {
        return store.cfg().getLong("players." + id + "." + key, def);
    }

    @Override
    public void setLong(UUID id, String key, long value) {
        store.cfg().set("players." + id + "." + key, value);
        store.dirty();
    }

    @Override
    public void save() {
        store.flush();
    }
}
