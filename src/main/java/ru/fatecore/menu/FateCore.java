import ru.fatecore.menu.*;
import org.bukkit.plugin.PluginManager;

// ... в onEnable(), после создания ClassManager (здесь он назван classes):
PluginManager pm = getServer().getPluginManager();

MenuItems menuItems = new MenuItems(this);
DonateGui donateGui = new DonateGui(this);
HelpGui helpGui = new HelpGui(this, donateGui);

boolean authMe = pm.isPluginEnabled("AuthMe"); // если используете другой плагин авторизации — см. ниже
PlayerItemListener itemListener =
        new PlayerItemListener(this, menuItems, classes, helpGui, donateGui, !authMe);

pm.registerEvents(itemListener, this);
pm.registerEvents(helpGui, this);
pm.registerEvents(donateGui, this);
if (authMe) {
    pm.registerEvents(new AuthMeHook(itemListener), this);
}
getCommand("menu").setExecutor(new MenuCommand(itemListener));
