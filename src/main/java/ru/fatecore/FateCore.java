package ru.fatecore;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.fatecore.admin.FateCommand;
import ru.fatecore.api.EchoServiceAdapter;
import ru.fatecore.api.IDataStore;
import ru.fatecore.api.IEchoService;
import ru.fatecore.api.PlayerDataStore;
import ru.fatecore.classes.ClassListener;
import ru.fatecore.classes.ClassManager;
import ru.fatecore.combat.CombatListener;
import ru.fatecore.combat.CombatManager;
import ru.fatecore.earn.EarnListener;
import ru.fatecore.earn.EarnService;
import ru.fatecore.echo.EchoCommand;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.echo.EchoNotifier;
import ru.fatecore.gui.GuiListener;
import ru.fatecore.learn.BountyCommand;
import ru.fatecore.learn.BountyListener;
import ru.fatecore.learn.BountyManager;
import ru.fatecore.learn.DailyCommand;
import ru.fatecore.learn.DailyListener;
import ru.fatecore.learn.DailyService;
import ru.fatecore.learn.DonateCommand;
import ru.fatecore.learn.HelpCommand;
import ru.fatecore.learn.LobbyCommand;
import ru.fatecore.learn.PlaytimeCommand;
import ru.fatecore.learn.PlaytimeTask;
import ru.fatecore.learn.RulesCommand;
import ru.fatecore.learn.ShopCommand;
import ru.fatecore.learn.ShopGui;
import ru.fatecore.learn.TipsTask;
import ru.fatecore.learn.AuthMeLoginBridge;
import ru.fatecore.menu.DonateGui;
import ru.fatecore.menu.HelpGui;
import ru.fatecore.menu.MenuCommand;
import ru.fatecore.menu.MenuItems;
import ru.fatecore.menu.PlayerItemListener;
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
import ru.fatecore.util.VoidListener;
import ru.fatecore.util.YamlStore;

import java.util.ArrayList;
import java.util.List;

public final class FateCore extends JavaPlugin {
    private final List<YamlStore> stores = new ArrayList<>();
    private PortalManager portals;
    private QuestNpc npc;
    private FatePlaceholders placeholders;
    private IDataStore dataStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Vault-экономика не найдена (нужен EssentialsX). Плагин выключен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginManager pm = getServer().getPluginManager();

        // --- Эхо и Данные ---
        EchoManager echoManager = new EchoManager(this, rsp.getProvider());
        EchoNotifier echoNotifier = new EchoNotifier();
        pm.registerEvents(echoNotifier, this);
        
        IEchoService echoService = new EchoServiceAdapter(echoManager, echoNotifier);
        dataStore = new PlayerDataStore(this);

        // --- Боевая система ---
        CombatManager combat = new CombatManager();
        combat.start(this);

        // --- Остальные менеджеры ---
        RotationManager rotation = new RotationManager(this);
        EarnService earn = new EarnService(this, echoManager, rotation);
        ClassManager classes = new ClassManager(this);
        QuestManager quests = new QuestManager(this, echoManager);
        npc = new QuestNpc(this);
        QuestGui questGui = new QuestGui(quests);
        ShopGui shop = new ShopGui(this, echoManager);
        OnboardingListener onboarding = new OnboardingListener(this);
        HelpCommand help = new HelpCommand(this);
        portals = new PortalManager(this);
        BountyManager bounties = new BountyManager(this);
        DailyService daily = new DailyService(this, dataStore, echoService);

        rotation.start();

        // --- Регистрация событий ---
        pm.registerEvents(new GuiListener(classes, shop), this);
        pm.registerEvents(new ClassListener(this, classes), this);
        pm.registerEvents(new EarnListener(earn, echoManager), this);
        pm.registerEvents(onboarding, this);
        pm.registerEvents(new QuestListener(quests, npc, questGui), this);
        pm.registerEvents(new BlockBreakListener(quests), this);
        pm.registerEvents(new PlayerDeathListener(quests), this);
        pm.registerEvents(new PortalListener(), this);
        pm.registerEvents(portals, this);
        pm.registerEvents(new PerkListener(this), this);
        pm.registerEvents(new TierRules(this), this);
        pm.registerEvents(help, this);
        pm.registerEvents(new VoidListener(this), this);
        pm.registerEvents(new CombatListener(combat), this);

        // --- Предметы меню (Компас и Звезда) ---
        MenuItems menuItems = new MenuItems(this);
        DonateGui donateGui = new DonateGui(this);
        HelpGui helpGui = new HelpGui(this, donateGui);
        PlayerItemListener itemListener = new PlayerItemListener(this, menuItems, classes, helpGui, donateGui, false);

        pm.registerEvents(itemListener, this);
        pm.registerEvents(helpGui, this);
        pm.registerEvents(donateGui, this);

        // --- Уведомления (AuthMe) ---
        boolean authMe = pm.isPluginEnabled("AuthMe");
        DailyListener dailyListener = new DailyListener(this, daily, !authMe);
        BountyListener bountyListener = new BountyListener(this, bounties, echoService, !authMe);
        pm.registerEvents(dailyListener, this);
        pm.registerEvents(bountyListener, this);

        if (authMe) {
            pm.registerEvents(new AuthMeLoginBridge(p -> {
                dailyListener.notifyIfAvailable(p);
                bountyListener.notifyBounty(p);
                itemListener.giveMissingLater(p);
            }), this);
        }

        // --- Регистрация команд ---
        getCommand("echo").setExecutor(new EchoCommand(echoManager, rotation, earn));
        getCommand("help").setExecutor(help);
        getCommand("shop").setExecutor(new ShopCommand(shop));
        getCommand("quests").setExecutor(new QuestCommand(questGui));
        getCommand("donate").setExecutor(new DonateCommand(this));
        getCommand("rules").setExecutor(new RulesCommand(this));
        getCommand("fatecore").setExecutor(new FateCommand(this, classes, npc, quests, earn, echoManager, portals::reload));
        getCommand("menu").setExecutor(new MenuCommand(itemListener));
        getCommand("lobby").setExecutor(new LobbyCommand(this, combat));
        getCommand("daily").setExecutor(new DailyCommand(daily));
        getCommand("playtime").setExecutor(new PlaytimeCommand(dataStore));
        getCommand("bounty").setExecutor(new BountyCommand(echoService, bounties));

        // --- Задачи (Таски) ---
        npc.spawnWhenReady();
        onboarding.placeSigns();
        new TipsTask(this).start();
        new DailyResetTask(this, quests).start();
        
        // Награда за игру (раз в минуту)
        java.util.function.Predicate<Player> eligible = authMe
                ? p -> fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(p)
                : p -> true;
        getServer().getScheduler().runTaskTimer(this, new PlaytimeTask(dataStore, echoService, eligible), 1200L, 1200L);

        new BukkitRunnable() {
            @Override public void run() { quests.checkPrefixExpiryOnline(); }
        }.runTaskTimer(this, 1200L, 1200L);

        new BukkitRunnable() {
            @Override public void run() { flushAll(); }
        }.runTaskTimer(this, 1200L, 1200L);

        // --- Плейсхолдеры ---
        if (pm.getPlugin("PlaceholderAPI") != null) {
            placeholders = new FatePlaceholders(this, quests, echoManager, classes, rotation, new HomeBonus(this), dataStore, daily, bounties, combat);
            placeholders.register();
        }
        getLogger().info("FateCore включён.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (npc != null) npc.removeAll();
        HandlerList.unregisterAll(this);
        if (placeholders != null) {
            placeholders.unregister();
            placeholders = null;
        }
        if (dataStore != null) dataStore.save();
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
