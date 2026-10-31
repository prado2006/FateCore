package ru.fatecore.quest;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.fatecore.FateCore;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.util.Items;
import ru.fatecore.util.Msg;
import ru.fatecore.util.YamlStore;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * МОДУЛЬ 6 — ежедневные квесты. Определения и награды читаются из quests.yml (секции quests, plushka),
 * прогресс игроков хранится там же в секции progress. Сброс в 00:00 МСК.
 *
 * Разрешённые типы наград: echo, item, effect, key, tools, quest_prefix, echo_boost.
 * Любой другой тип игнорируется и пишется в лог. Донат-тиры выдать нельзя (§7.1).
 */
public final class QuestManager {
    public static final String QUEST_GROUP = "quest";
    public static final ZoneId MSK = ZoneId.of("Europe/Moscow");

    public enum Quest {
        SURVIVAL("survival", Material.PAPER, "Survival"),
        BEDWARS("bedwars", Material.RED_BED, "BedWars"),
        SKYWARS("skywars", Material.DIAMOND_SWORD, "SkyWars");

        public final String key;
        public final Material icon;
        public final String mode;

        Quest(String key, Material icon, String mode) {
            this.key = key;
            this.icon = icon;
            this.mode = mode;
        }
    }

    private final FateCore plugin;
    private final EchoManager echo;
    private final YamlStore store;

    public QuestManager(FateCore plugin, EchoManager echo) {
        this.plugin = plugin;
        this.echo = echo;
        this.store = new YamlStore(plugin, "quests.yml");
    }

    // ---------- определения из quests.yml ----------

    private ConfigurationSection def(Quest q) {
        return store.cfg().getConfigurationSection("quests." + q.key);
    }

    public int target(Quest q) {
        ConfigurationSection d = def(q);
        return d == null ? 1 : Math.max(1, d.getInt("target", 1));
    }

    public String title(Quest q) {
        ConfigurationSection d = def(q);
        return d == null ? q.mode : d.getString("title", q.mode);
    }

    public String rewardText(Quest q) {
        ConfigurationSection d = def(q);
        return d == null ? "" : d.getString("reward-text", "");
    }

    /** Подписи вариантов «Плюшки» из quests.yml (plushka[].label). */
    public List<String> plushkaLabels() {
        List<String> out = new java.util.ArrayList<>();
        for (Map<?, ?> v : store.cfg().getMapList("plushka")) out.add(String.valueOf(v.get("label")));
        return out;
    }

    // ---------- данные игрока ----------

    private ConfigurationSection sec(UUID id) {
        String path = "progress." + id;
        ConfigurationSection s = store.cfg().getConfigurationSection(path);
        return s != null ? s : store.cfg().createSection(path);
    }

    public void resetQuestsIfNewDay(UUID id) {
        String today = LocalDate.now(MSK).toString();
        ConfigurationSection s = sec(id);
        if (today.equals(s.getString("lastReset"))) return;
        for (Quest q : Quest.values()) s.set(q.key, 0);
        s.set("dayCompleted", 0);
        s.set("rewarded", null);
        s.set("plushka", false);
        s.set("lastReset", today);
        store.dirty();
    }

    public void resetOnline() {
        plugin.getServer().getOnlinePlayers().forEach(p -> resetQuestsIfNewDay(p.getUniqueId()));
    }

    public int progress(UUID id, Quest q) {
        resetQuestsIfNewDay(id);
        return sec(id).getInt(q.key);
    }

    public boolean isDone(UUID id, Quest q) {
        return progress(id, q) >= target(q);
    }

    public int dayCompleted(UUID id) {
        resetQuestsIfNewDay(id);
        return sec(id).getInt("dayCompleted");
    }

    public boolean plushkaGiven(UUID id) {
        resetQuestsIfNewDay(id);
        return sec(id).getBoolean("plushka");
    }

    public String progressString(UUID id) {
        StringBuilder sb = new StringBuilder();
        for (Quest q : Quest.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(q.mode).append(' ').append(progress(id, q)).append('/').append(target(q));
        }
        return sb.toString();
    }

    // ---------- прогресс ----------

    public void addProgress(Player p, Quest q, int amount) {
        UUID id = p.getUniqueId();
        resetQuestsIfNewDay(id);
        ConfigurationSection s = sec(id);
        int cur = s.getInt(q.key);
        int target = target(q);
        if (cur >= target) return;
        int now = Math.min(target, cur + amount);
        s.set(q.key, now);
        store.dirty();
        if (now >= target) complete(p, q, s);
    }

    private void complete(Player p, Quest q, ConfigurationSection s) {
        if (s.getBoolean("rewarded." + q.key)) return;
        s.set("rewarded." + q.key, true);
        int done = s.getInt("dayCompleted") + 1;
        s.set("dayCompleted", done);
        store.dirty();

        applyRewards(p, store.cfg().getMapList("quests." + q.key + ".rewards"));
        Msg.send(p, "&aКвест выполнен: &6" + q.mode + " — " + title(q) + "&a. Награда: " + rewardText(q));

        if (done >= Quest.values().length && !s.getBoolean("plushka")) {
            s.set("plushka", true);
            store.dirty();
            giveBonus(p);
        }
    }

    /** «Плюшка» за все 3 квеста: случайный вариант из quests.yml → plushka. */
    private void giveBonus(Player p) {
        List<Map<?, ?>> variants = store.cfg().getMapList("plushka");
        if (variants.isEmpty()) return;
        Map<?, ?> v = variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        Msg.send(p, "&d&lВсе квесты выполнены! &fТвоя плюшка: &e" + v.get("label"));
        Object rewards = v.get("rewards");
        if (rewards instanceof List<?> list) applyRewards(p, list);
    }

    // ---------- награды ----------

    private void applyRewards(Player p, List<?> rewards) {
        for (Object o : rewards) {
            if (o instanceof Map<?, ?> m) applyReward(p, m);
        }
    }

    private static double num(Map<?, ?> r, String k, double def) {
        Object o = r.get(k);
        return o instanceof Number n ? n.doubleValue() : def;
    }

    private static String str(Map<?, ?> r, String k) {
        Object o = r.get(k);
        return o == null ? "" : o.toString();
    }

    private void applyReward(Player p, Map<?, ?> r) {
        String type = str(r, "type").toLowerCase();
        switch (type) {
            case "echo" -> echo.addEcho(p, num(r, "amount", 0));
            case "item" -> {
                Material m = Material.matchMaterial(str(r, "material"));
                if (m == null) {
                    plugin.getLogger().warning("quests.yml: неизвестный материал " + str(r, "material"));
                    return;
                }
                Items.give(p, new ItemStack(m, (int) num(r, "amount", 1)));
            }
            case "effect" -> {
                PotionEffectType t = PotionEffectType.getByName(str(r, "effect"));
                if (t == null) {
                    plugin.getLogger().warning("quests.yml: неизвестный эффект " + str(r, "effect"));
                    return;
                }
                p.addPotionEffect(new PotionEffect(t, (int) num(r, "seconds", 60) * 20, (int) num(r, "amplifier", 0)));
            }
            case "key" -> {
                for (int i = 0; i < (int) num(r, "amount", 1); i++) {
                    ItemStack key = Items.make(Material.TRIPWIRE_HOOK, "&6Ключ", "&7Для открытия кейса");
                    ItemMeta meta = key.getItemMeta();
                    meta.getPersistentDataContainer().set(plugin.key("key"), PersistentDataType.BYTE, (byte) 1);
                    key.setItemMeta(meta);
                    Items.give(p, key);
                }
            }
            case "tools" -> giveTools(p, r);
            case "quest_prefix" -> grantQuestPrefix(p, (long) num(r, "hours", 24));
            case "echo_boost" -> echo.addBoost(p.getUniqueId(), num(r, "multiplier", 1.1), (long) num(r, "seconds", 3600));
            default -> plugin.getLogger().warning("quests.yml: тип награды «" + type + "» не разрешён и пропущен");
        }
    }

    private void giveTools(Player p, Map<?, ?> r) {
        Object items = r.get("items");
        Object enchants = r.get("enchants");
        if (!(items instanceof List<?> list)) return;
        for (Object name : list) {
            Material mat = Material.matchMaterial(String.valueOf(name));
            if (mat == null) continue;
            ItemStack it = new ItemStack(mat);
            if (enchants instanceof Map<?, ?> em) {
                for (Map.Entry<?, ?> en : em.entrySet()) {
                    // getByKey работает на 1.20.1 и новее (константы EFFICIENCY/UNBREAKING есть только в новых API)
                    Enchantment ench = Enchantment.getByKey(
                            NamespacedKey.minecraft(String.valueOf(en.getKey()).toLowerCase()));
                    if (ench != null && en.getValue() instanceof Number n) {
                        it.addUnsafeEnchantment(ench, n.intValue());
                    }
                }
            }
            Items.give(p, it);
        }
    }

    // ---------- префикс [Quest]: только группа "quest", на ограниченное время ----------

    public void grantQuestPrefix(Player p, long hours) {
        LuckPerms lp = LuckPermsProvider.get();
        Instant until = Instant.now().plus(Duration.ofHours(hours));
        lp.getUserManager().modifyUser(p.getUniqueId(), user -> {
            user.data().clear(NodeType.INHERITANCE.predicate(n -> n.getGroupName().equals(QUEST_GROUP)));
            user.data().add(InheritanceNode.builder(QUEST_GROUP).expiry(until).build());
        });
        sec(p.getUniqueId()).set("questPrefixUntil", until.toEpochMilli());
        store.dirty();
    }

    /** Планировщик: снимает группу quest, когда срок вышел. */
    public void checkPrefixExpiry(Player p) {
        ConfigurationSection s = sec(p.getUniqueId());
        long until = s.getLong("questPrefixUntil");
        if (until <= 0 || System.currentTimeMillis() < until) return;
        LuckPerms lp = LuckPermsProvider.get();
        lp.getUserManager().modifyUser(p.getUniqueId(), user ->
                user.data().clear(NodeType.INHERITANCE.predicate(n -> n.getGroupName().equals(QUEST_GROUP))));
        s.set("questPrefixUntil", 0);
        store.dirty();
    }

    public void checkPrefixExpiryOnline() {
        plugin.getServer().getOnlinePlayers().forEach(this::checkPrefixExpiry);
    }

    public void flush() {
        store.flush();
    }
}
