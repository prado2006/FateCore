package ru.fatecore.admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import ru.fatecore.FateCore;
import ru.fatecore.classes.ClassManager;
import ru.fatecore.earn.EarnService;
import ru.fatecore.echo.EchoManager;
import ru.fatecore.learn.CaseItems;
import ru.fatecore.quest.QuestManager;
import ru.fatecore.quest.QuestNpc;
import ru.fatecore.util.Items;
import ru.fatecore.util.Msg;

/**
 * МОДУЛЬ 8 — /fatecore.
 *   reload                          — fatecore.reload (admin, owner)
 *   class                           — fatecore.class.use
 *   npc, bwwin, boost, case         — fatecore.admin.* (только owner и консоль)
 * Консоль (ConsoleCommandSender) всегда проходит проверку прав для bwwin.
 * boost и case вызываются из shop.rewards (консоль). Привилегий и донат-групп эти команды не выдают.
 */
public final class FateCommand implements CommandExecutor {
    private static final String USAGE = "&7/fatecore <reload|class|npc|bwwin|boost|case>";
    private static final String SERVER_PREFIX = "§6[Сервер]§r ";

    private final FateCore plugin;
    private final ClassManager classes;
    private final QuestNpc npc;
    private final QuestManager quests;
    private final EarnService earn;
    private final EchoManager echo;
    private final Runnable reloadHook;

    public FateCommand(FateCore plugin, ClassManager classes, QuestNpc npc, QuestManager quests,
                       EarnService earn, EchoManager echo, Runnable reloadHook) {
        this.plugin = plugin;
        this.classes = classes;
        this.npc = npc;
        this.quests = quests;
        this.earn = earn;
        this.echo = echo;
        this.reloadHook = reloadHook;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, USAGE);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("fatecore.reload")) {
                    Msg.send(sender, "&cНет прав.");
                    return true;
                }
                plugin.reloadConfig();
                npc.respawn();
                reloadHook.run();
                Msg.send(sender, "&aКонфиг FateCore перезагружен.");
            }
            case "class" -> {
                if (!(sender instanceof Player p)) {
                    Msg.send(sender, "Только для игроков.");
                    return true;
                }
                if (!p.hasPermission("fatecore.class.use")) {
                    Msg.send(p, "&cНет прав.");
                    return true;
                }
                classes.openGui(p);
            }
            case "npc" -> {
                if (!sender.hasPermission("fatecore.admin.npc")) {
                    Msg.send(sender, "&cНет прав.");
                    return true;
                }
                boolean ok = npc.respawn();
                Msg.send(sender, ok ? "&aКвестовый Мастер пересоздан в quest.npc.location." : "&cМир из quest.npc.location не загружен.");
            }
            case "bwwin" -> bwWin(sender, args);
            case "boost" -> boost(sender, args);
            case "case" -> giveCase(sender, args);
            default -> Msg.send(sender, USAGE);
        }
        return true;
    }

    /** /fatecore bwwin <ник> — засчитать победу в BedWars (Эхо + квест). Консоль всегда допускается. */
    private void bwWin(CommandSender sender, String[] args) {
        boolean allowed = sender instanceof ConsoleCommandSender
                || sender.hasPermission("fatecore.admin.bwwin");
        if (!allowed) {
            serverMsg(sender, "§cУ вас нет прав на эту команду.");
            return;
        }
        if (args.length < 2) {
            serverMsg(sender, "§7Использование: /fatecore bwwin <ник>");
            return;
        }

        String name = args[1];
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            // Проверяем, знает ли сервер такого игрока (для понятного сообщения).
            OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(name);
            if (off == null) {
                serverMsg(sender, "§cИгрок не найден: §f" + name);
            } else {
                serverMsg(sender, "§cИгрок §f" + name + " §cсейчас не в сети — победа не засчитана.");
            }
            return;
        }

        earn.addBedwarsWinEcho(target);
        quests.addProgress(target, QuestManager.Quest.BEDWARS, 1);

        serverMsg(sender, "§aПобеда засчитана: §f" + target.getName());
        serverMsg(sender, "§aЭхо за победу начислено: §f" + target.getName() + "§a, квест BedWars засчитан.");
    }

    /**
     * Сообщение от имени Сервера: отправителю команды и в консоль (если отправитель — игрок).
     */
    private void serverMsg(CommandSender sender, String text) {
        String full = SERVER_PREFIX + text;
        sender.sendMessage(full);
        if (!(sender instanceof ConsoleCommandSender)) {
            Bukkit.getConsoleSender().sendMessage(full);
        }
    }

    /** /fatecore boost <игрок> <множитель 1.0–5.0> <секунды ≤ 604800> — временный множитель Эхо. */
    private void boost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fatecore.admin.boost")) {
            Msg.send(sender, "&cНет прав.");
            return;
        }
        if (args.length < 4) {
            Msg.send(sender, "&7/fatecore boost <игрок> <множитель> <секунды>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.send(sender, "&cИгрок не в сети.");
            return;
        }
        double mult;
        long seconds;
        try {
            mult = Double.parseDouble(args[2]);
            seconds = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            Msg.send(sender, "&cМножитель и секунды должны быть числами.");
            return;
        }
        if (mult < 1.0 || mult > 5.0 || seconds < 1 || seconds > 604800) {
            Msg.send(sender, "&cМножитель 1.0–5.0, секунды 1–604800.");
            return;
        }
        echo.addBoost(target.getUniqueId(), mult, seconds);
        Msg.send(sender, "&aБуст x" + mult + " на " + seconds + " с выдан: " + target.getName());
    }

    /** /fatecore case <игрок> <common|rare> — предмет-кейс. */
    private void giveCase(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fatecore.admin.case")) {
            Msg.send(sender, "&cНет прав.");
            return;
        }
        if (args.length < 3 || !(args[2].equalsIgnoreCase("common") || args[2].equalsIgnoreCase("rare"))) {
            Msg.send(sender, "&7/fatecore case <игрок> <common|rare>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.send(sender, "&cИгрок не в сети.");
            return;
        }
        Items.give(target, CaseItems.make(plugin, args[2].equalsIgnoreCase("rare")));
        Msg.send(sender, "&aКейс " + args[2].toLowerCase() + " выдан: " + target.getName());
    }
}
