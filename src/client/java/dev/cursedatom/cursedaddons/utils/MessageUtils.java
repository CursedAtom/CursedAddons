package dev.cursedatom.cursedaddons.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class MessageUtils {

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
            Minecraft.getInstance().gui.getChat().addRecentChat(text);
            if (text2.startsWith("/")) {
                player.connection.sendCommand(text2.substring(1));
            } else {
                player.connection.sendChat(text2);
            }
        }
    }
}
