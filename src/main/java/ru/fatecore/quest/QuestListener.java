package ru.fatecore.quest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.fatecore.util.Auth;
import ru.fatecore.util.Msg;

/** Квестовый Мастер: правый клик открывает GUI (как /quests), NPC неуязвим; сброс и снятие [Quest] при входе. */
public final class QuestListener implements Listener {
    private final QuestManager quests;
    private final QuestNpc npc;
    private final QuestGui gui;

    public QuestListener(QuestManager quests, QuestNpc npc, QuestGui gui) {
        this.quests = quests;
        this.npc = npc;
        this.gui = gui;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        quests.resetQuestsIfNewDay(e.getPlayer().getUniqueId());
        quests.checkPrefixExpiry(e.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!npc.isNpc(e.getRightClicked())) return;
        e.setCancelled(true); // без окна торговли жителя
        // клиент шлёт INTERACT_AT и INTERACT: GUI открываем один раз
        if (e instanceof PlayerInteractAtEntityEvent) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!Auth.isAuthed(p)) {
            Msg.send(p, "&cСначала /login");
            return;
        }
        gui.open(p);
    }

    @EventHandler
    public void onNpcDamage(EntityDamageEvent e) {
        if (npc.isNpc(e.getEntity())) e.setCancelled(true);
    }
}
