package dev.cursedatom.cursedaddons.utils;

import dev.cursedatom.cursedaddons.CursedAddons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Utility class for sending messages and commands to the Minecraft chat system.
 * Provides both server-visible chat and client-only (non-public) message injection,
 * with rate limiting for command sends.
 */
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

        String trimmedMessage = text.trim();
        if (!trimmedMessage.isEmpty()) {
            // Rate limiting for commands (50ms minimum interval)
            long currentTime = System.currentTimeMillis();
            if (trimmedMessage.startsWith("/") && currentTime - lastCommandTime < COMMAND_RATE_LIMIT_MS) {
                CursedAddons.LOGGER.warn("[CursedAddons] Command \"{}\" not executed due to ratelimit! ({}/{}ms)",
                    trimmedMessage, currentTime - lastCommandTime, COMMAND_RATE_LIMIT_MS);
                return;
            }

            Minecraft.getInstance().gui.getChat().addRecentChat(text);
            if (trimmedMessage.startsWith("/")) {
                player.connection.sendCommand(trimmedMessage.substring(1));
                lastCommandTime = currentTime;
            } else {
                player.connection.sendChat(trimmedMessage);
            }
        }
    }
}
