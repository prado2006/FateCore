package ru.fatecore.api;
import java.util.UUID;

public interface IDataStore {
    long getLong(UUID id, String key, long def);
    void setLong(UUID id, String key, long value);
    default void save() {}
}
