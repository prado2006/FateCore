package ru.fatecore.quest;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import ru.fatecore.quest.QuestManager.Quest;
import ru.fatecore.util.Modes;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Квест Survival «Собери 32 дерева»: ломание бревна в world_survival → +1 (максимум по quests.yml).
 * Бревно, поставленное игроком, не засчитывается (защита от «поставил-сломал»; память до перезапуска).
 */
public final class BlockBreakListener implements Listener {
    private static final int MAX_TRACKED = 5000;

    private final QuestManager quests;
    private final Set<Long> placed = new LinkedHashSet<>();

    public BlockBreakListener(QuestManager quests) {
        this.quests = quests;
    }

    private static long key(Block b) {
        return (((long) b.getX() & 0x3FFFFFFL) << 38) | (((long) b.getZ() & 0x3FFFFFFL) << 12) | ((long) b.getY() & 0xFFFL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (!Modes.SURVIVAL.equals(Modes.of(b.getWorld())) || !Tag.LOGS.isTagged(b.getType())) return;
        placed.add(key(b));
        if (placed.size() > MAX_TRACKED) {
            Iterator<Long> it = placed.iterator();
            it.next();
            it.remove();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!Modes.SURVIVAL.equals(Modes.of(b.getWorld())) || !Tag.LOGS.isTagged(b.getType())) return;
        if (placed.remove(key(b))) return;
        quests.addProgress(e.getPlayer(), Quest.SURVIVAL, 1);
    }
}
