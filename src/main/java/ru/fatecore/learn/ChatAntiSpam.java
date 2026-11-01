package ru.fatecore.learn;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ChatAntiSpam implements Listener {
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, String> lastMessage = new HashMap<>();
    
    private static final long COOLDOWN_MS = 1500; 

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?:\\/\\/|www\\.)\\S+|(?i)\\b\\S+\\.(?:com|ru|net|org|io|me|tv|gg|xyz|link|site)\\b"
    );

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        String message = PlainTextComponentSerializer.plainText().serialize(e.message()).toLowerCase().trim();
        
        long now = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(id);
        String lastMsg = lastMessage.get(id);

        if (!p.hasPermission("fatecore.chat.links") && URL_PATTERN.matcher(message).find()) {
            e.setCancelled(true);
            p.sendMessage("§cСсылки в чате запрещены! Реклама и фишинг будут наказаны баном.");
            return;
        }

        if (lastTime != null && now - lastTime < COOLDOWN_MS) {
            e.setCancelled(true);
            p.sendMessage("§cНе так быстро! Подождите немного перед отправкой сообщения.");
            return;
        }

        if (lastMsg != null && lastMsg.equals(message)) {
            e.setCancelled(true);
            p.sendMessage("§cНе повторяйте одно и то же сообщение!");
            return;
        }

        lastMessageTime.put(id, now);
        lastMessage.put(id, message);
    }
}
