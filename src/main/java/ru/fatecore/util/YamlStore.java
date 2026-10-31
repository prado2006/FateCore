package ru.fatecore.util;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.fatecore.FateCore;

import java.io.File;
import java.io.IOException;

/** Простое YAML-хранилище с отложенной записью (flush вызывается планировщиком и при выключении). */
public final class YamlStore {
    private final FateCore plugin;
    private final File file;
    private final YamlConfiguration cfg;
    private boolean dirty;

    public YamlStore(FateCore plugin, String name) {
        this.plugin = plugin;
        File dir = plugin.getDataFolder();
        dir.mkdirs();
        this.file = new File(dir, name);
        if (!file.exists()) {
            if (plugin.getResource(name) != null) {
                plugin.saveResource(name, false);
            } else {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().warning("Не удалось создать " + name + ": " + e.getMessage());
                }
            }
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
        plugin.stores().add(this);
    }

    public YamlConfiguration cfg() {
        return cfg;
    }

    public void dirty() {
        dirty = true;
    }

    public void flush() {
        if (!dirty) return;
        try {
            cfg.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить " + file.getName() + ": " + e.getMessage());
        }
    }
}
