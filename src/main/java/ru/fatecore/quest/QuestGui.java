package ru.fatecore.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.fatecore.gui.FateHolder;
import ru.fatecore.quest.QuestManager.Quest;
import ru.fatecore.util.Items;
import ru.fatecore.util.Msg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI Квестового Мастера (27 слотов): слоты 11 / 13 / 15 — квесты, 22 — «Плюшка», 26 — «Закрыть».
 * Открывается кликом по NPC и командой /quests. В каждой награде: «Привилегии не выдаются.»
 */
public final class QuestGui {
    private static final String NO_PRIV = "&7Привилегии не выдаются.";

    private final QuestManager quests;

    public QuestGui(QuestManager quests) {
        this.quests = quests;
    }

    public void open(Player p) {
        UUID id = p.getUniqueId();
        FateHolder holder = new FateHolder("quests");
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("&8Квестовый Мастер"));
        holder.bind(inv);
        Items.fill(inv);

        inv.setItem(11, questItem(id, Quest.SURVIVAL));
        inv.setItem(13, questItem(id, Quest.BEDWARS));
        inv.setItem(15, questItem(id, Quest.SKYWARS));
        inv.setItem(22, plushkaItem(id));
        inv.setItem(26, Items.make(Material.BARRIER, "&cЗакрыть"));
        p.openInventory(inv);
    }

    private ItemStack plushkaItem(UUID id) {
        int done = quests.dayCompleted(id);
        boolean given = quests.plushkaGiven(id);
        List<String> lore = new ArrayList<>();
        lore.add("&7Выполни все 3 квеста — получишь случайную плюшку:");
        for (String label : quests.plushkaLabels()) lore.add("&f • " + label);
        lore.add("&eВыполнено: " + done + "/3");
        lore.add(given ? "&aПлюшка получена" : "&cПлюшка ещё не получена");
        lore.add(NO_PRIV);
        // серая, пока не выполнены все 3 квеста
        return given
                ? Items.make(Material.BOOK, "&dПлюшка", lore.toArray(new String[0]))
                : Items.make(Material.GRAY_DYE, "&7Плюшка", lore.toArray(new String[0]));
    }

    private ItemStack questItem(UUID id, Quest q) {
        int cur = quests.progress(id, q);
        int target = quests.target(q);
        boolean done = cur >= target;
        return Items.make(q.icon, "&6" + q.mode + " — " + quests.title(q),
                "&7Прогресс: &f" + cur + "/" + target,
                done ? "&aВыполнено" : "&cВ процессе",
                "&6Награда: &f" + quests.rewardText(q),
                NO_PRIV,
                "&8Сброс в 00:00 МСК");
    }
}
