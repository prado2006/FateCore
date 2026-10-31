package ru.fatecore.quest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import ru.fatecore.FateCore;
import ru.fatecore.util.ConfigUtil;
import ru.fatecore.util.Retry;

import java.util.ArrayList;
import java.util.List;

/**
 * Квестовый Мастер: Villager без AI внутри FateCore (Citizens не нужен).
 * Спавнится в onEnable в quest.npc.location, удаляется в onDisable, помечен PDC-ключом quest_npc.
 */
public final class QuestNpc {
    public static final String NAME = "Квестовый Мастер";

    private final FateCore plugin;
    private final NamespacedKey key;

    public QuestNpc(FateCore plugin) {
        this.plugin = plugin;
        this.key = plugin.key("quest_npc");
    }

    public boolean isNpc(Entity e) {
        return e instanceof Villager v && v.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Ждёт загрузки мира и создаёт NPC, если рядом с точкой его ещё нет. */
    public void spawnWhenReady() {
        Retry.run(plugin, 100L, 10, this::spawnIfMissing);
    }

    /** true — NPC на месте (был или создан), false — мир ещё не загружен. Лишние копии рядом удаляются. */
    public boolean spawnIfMissing() {
        Location loc = ConfigUtil.location(plugin.getConfig().getConfigurationSection("quest.npc.location"));
        if (loc == null) return false;
        loc.getChunk().load();
        List<Villager> found = new ArrayList<>();
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (isNpc(e)) found.add((Villager) e);
        }
        if (!found.isEmpty()) {
            for (int i = 1; i < found.size(); i++) found.get(i).remove();
            return true;
        }
        loc.getWorld().spawn(loc, Villager.class, v -> {
            v.setAI(false);
            v.setCustomName(NAME);
            v.setCustomNameVisible(true);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setProfession(Villager.Profession.LIBRARIAN);
            v.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        });
        return true;
    }

    /** Удаляет всех Квестовых Мастеров в загруженных чанках (вызывается в onDisable и перед респавном). */
    public int removeAll() {
        int n = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Villager v : w.getEntitiesByClass(Villager.class)) {
                if (isNpc(v)) {
                    v.remove();
                    n++;
                }
            }
        }
        return n;
    }

    /** Пересоздаёт NPC в актуальной точке из конфига (после правки координат). */
    public boolean respawn() {
        removeAll();
        return spawnIfMissing();
    }
}
