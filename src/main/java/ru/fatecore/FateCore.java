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
import ru.fatecore.learn.*;
import ru.fatecore.menu.*;
import ru.fatecore.onboarding.OnboardingListener;
import ru.fatecore.perks.HomeBonus;
import ru.fatecore.perks.PerkListener;
import ru.fatecore.perks.TierRules;
import ru.fatecore.portal.PortalListener;
import ru.fatecore.portal.PortalManager;
import ru.fatecore.quest.*;
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

        EchoManager echoManager = new EchoManager(this, rsp.getProvider());
        EchoNotifier echoNotifier = new EchoNotifier();
        pm.registerEvents(echoNotifier, this);
        
        IEchoService echoService = new EchoServiceAdapter(echoManager, echoNotifier);
        dataStore = new PlayerDataStore(this);

        CombatManager combat = new CombatManager();
        combat.start(this);

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
        pm.registerEvents(new ChatAntiSpam(), this);
        pm.registerEvents(new TpaBypassListener(), this);

        MenuItems menuItems = new MenuItems(this);
        DonateGui donateGui = new DonateGui(this);
        HelpGui helpGui = new HelpGui(this, donateGui);
        MainMenuGui mainMenuGui = new MainMenuGui(this, classes, helpGui, donateGui, combat);
        PlayerItemListener itemListener = new PlayerItemListener(this, menuItems, classes, helpGui, donateGui, mainMenuGui, combat);

        pm.registerEvents(itemListener, this);
        pm.registerEvents(helpGui, this);
        pm.registerEvents(donateGui, this);
        pm.registerEvents(mainMenuGui, this);

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
        getCommand("bounty").setExecutor(new BountyCommand(echoService, bounties, dataStore));

        npc.spawnWhenReady();
        onboarding.placeSigns();
        new TipsTask(this).start();
        new DailyResetTask(this, quests).start();
        
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
