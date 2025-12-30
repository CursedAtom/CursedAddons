package dev.cursedatom.cursedaddons.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class MessageUtils {
    private static long lastCommandTime = 0;
    private static final long COMMAND_RATE_LIMIT_MS = 50;

    public static void sendToNonPublicChat(Component text) {
        if (Minecraft.getInstance().gui != null) {
            Minecraft.getInstance().gui.getChat().addMessage(text);
        }
    }

    public static void sendToPublicChat(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        String text2 = text.trim();
        if (!text2.isEmpty()) {
            // Rate limiting for commands (50ms minimum interval)
            long currentTime = System.currentTimeMillis();
            if (text2.startsWith("/") && currentTime - lastCommandTime < COMMAND_RATE_LIMIT_MS) {
                LoggerUtils.warn("Command \"" + text2 + "\" not executed due to ratelimit! (" + (currentTime-lastCommandTime)+"/50ms)");
                return; // Skip command if rate limited

            }

            Minecraft.getInstance().gui.getChat().addRecentChat(text);
            if (text2.startsWith("/")) {
                player.connection.sendCommand(text2.substring(1));
                lastCommandTime = currentTime;
            } else {
                player.connection.sendChat(text2);
            }
        }
    }
}
