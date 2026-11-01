package ru.fatecore;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.fatecore.admin.FateCommand;
import ru.fatecore.classes.ClassListener;
import ru.fatecore.classes.ClassManager;
import ru.fatecore.earn.EarnListener;
import ru.fatecore.earn.EarnService;
import ru.fatecore.echo.EchoCommand;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.gui.GuiListener;
import ru.fatecore.learn.DonateCommand;
import ru.fatecore.learn.HelpCommand;
import ru.fatecore.learn.RulesCommand;
import ru.fatecore.learn.ShopCommand;
import ru.fatecore.learn.ShopGui;
import ru.fatecore.learn.TipsTask;
import ru.fatecore.menu.*; // Добавлен импорт меню
import ru.fatecore.onboarding.OnboardingListener;
import ru.fatecore.perks.HomeBonus;
import ru.fatecore.perks.PerkListener;
import ru.fatecore.perks.TierRules;
import ru.fatecore.portal.PortalListener;
import ru.fatecore.portal.PortalManager;
import ru.fatecore.quest.BlockBreakListener;
import ru.fatecore.quest.DailyResetTask;
import ru.fatecore.quest.FatePlaceholders;
import ru.fatecore.quest.PlayerDeathListener;
import ru.fatecore.quest.QuestCommand;
import ru.fatecore.quest.QuestGui;
import ru.fatecore.quest.QuestListener;
import ru.fatecore.quest.QuestManager;
import ru.fatecore.quest.QuestNpc;
import ru.fatecore.rotation.RotationManager;
import ru.fatecore.util.YamlStore;

import java.util.ArrayList;
import java.util.List;

public final class FateCore extends JavaPlugin {
    private final List<YamlStore> stores = new ArrayList<>();
    private PortalManager portals;
    private QuestNpc npc;
    private FatePlaceholders placeholders;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Vault-экономика не найдена (нужен EssentialsX). Плагин выключен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        EchoManager echo = new EchoManager(this, rsp.getProvider());
        RotationManager rotation = new RotationManager(this);
        EarnService earn = new EarnService(this, echo, rotation);
        ClassManager classes = new ClassManager(this);
        QuestManager quests = new QuestManager(this, echo);
        npc = new QuestNpc(this);
        QuestGui questGui = new QuestGui(quests);
        ShopGui shop = new ShopGui(this, echo);
        OnboardingListener onboarding = new OnboardingListener(this);
        HelpCommand help = new HelpCommand(this);
        portals = new PortalManager(this);

        rotation.start();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GuiListener(classes, shop), this);
        pm.registerEvents(new ClassListener(this, classes), this);
        pm.registerEvents(new EarnListener(earn, echo), this);
        pm.registerEvents(onboarding, this);
        pm.registerEvents(new QuestListener(quests, npc, questGui), this);
        pm.registerEvents(new BlockBreakListener(quests), this);
        pm.registerEvents(new PlayerDeathListener(quests), this);
        pm.registerEvents(new PortalListener(), this);
        pm.registerEvents(portals, this);
        pm.registerEvents(new PerkListener(this), this);
        pm.registerEvents(new TierRules(this), this); // строго после PerkListener
        pm.registerEvents(help, this);

        getCommand("echo").setExecutor(new EchoCommand(echo, rotation, earn));
        getCommand("help").setExecutor(help);
        getCommand("shop").setExecutor(new ShopCommand(shop));
        getCommand("quests").setExecutor(new QuestCommand(questGui));
        getCommand("donate").setExecutor(new DonateCommand(this));
        getCommand("rules").setExecutor(new RulesCommand(this));
        getCommand("fatecore").setExecutor(new FateCommand(this, classes, npc, quests, earn, echo, portals::reload));

        npc.spawnWhenReady();
        onboarding.placeSigns();
        new TipsTask(this).start();
        new DailyResetTask(this, quests).start(); // 00:00 МСК

        // раз в минуту: снятие группы quest по сроку (24 ч)
        new BukkitRunnable() {
            @Override
            public void run() {
                quests.checkPrefixExpiryOnline();
            }
        }.runTaskTimer(this, 1200L, 1200L);

        // автосохранение данных раз в минуту
        new BukkitRunnable() {
            @Override
            public void run() {
                flushAll();
            }
        }.runTaskTimer(this, 1200L, 1200L);

        if (pm.getPlugin("PlaceholderAPI") != null) {
            placeholders = new FatePlaceholders(this, quests, echo, classes, rotation, new HomeBonus(this));
            placeholders.register();
        }

        // --- Меню предметов (Компас и Звезда) ---
        MenuItems menuItems = new MenuItems(this);
        DonateGui donateGui = new DonateGui(this);
        HelpGui helpGui = new HelpGui(this, donateGui);

        boolean authMe = pm.isPluginEnabled("AuthMe");
        PlayerItemListener itemListener =
                new PlayerItemListener(this, menuItems, classes, helpGui, donateGui, !authMe);

        pm.registerEvents(itemListener, this);
        pm.registerEvents(helpGui, this);
        pm.registerEvents(donateGui, this);

        if (authMe) {
            pm.registerEvents(new AuthMeHook(itemListener), this);
        }

        getCommand("menu").setExecutor(new MenuCommand(itemListener));
        // ----------------------------------------

        getLogger().info("FateCore включён.");
    }

    @Override
    public void onDisable() {
        // отменяем все планировщики плагина: ротация, подсказки, квесты, автосохранение
        getServer().getScheduler().cancelTasks(this);
        if (npc != null) npc.removeAll();
        HandlerList.unregisterAll(this);
        if (placeholders != null) {
            placeholders.unregister();
            placeholders = null;
        }
        flushAll();
    }

    private void flushAll() {
        for (YamlStore s : stores) s.flush();
    }

    public List<YamlStore> stores() {
        return stores;
    }

    public NamespacedKey key(String name) {
        return new NamespacedKey(this, name);
    }
}
