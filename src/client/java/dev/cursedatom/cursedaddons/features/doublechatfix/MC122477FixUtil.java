package dev.cursedatom.cursedaddons.features.doublechatfix;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class MC122477FixUtil {

    private static int pollsSinceKeyPress = -1;

    public static void onKeyPress(long window, int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            Minecraft client = Minecraft.getInstance();
            if (client.screen == null) {
                KeyMapping chatKey = client.options.keyChat;
                KeyMapping commandKey = client.options.keyCommand;
                if (matches(key, chatKey) || matches(key, commandKey)) {
                    pollsSinceKeyPress = 0;
                }
            }
        }
    }

    private static boolean matches(int key, KeyMapping keyMapping) {
        InputConstants.Key boundKey = InputConstants.getKey(keyMapping.saveString());
        return boundKey.getValue() == key && boundKey.getType() == InputConstants.Type.KEYSYM;
    }

    public static boolean shouldCancelChar() {
        if (pollsSinceKeyPress >= 0 && pollsSinceKeyPress < 6) {
            pollsSinceKeyPress = -1;
            return true;
        }
        return false;
    }

    public static void incrementPollCount() {
        if (pollsSinceKeyPress >= 0) {
            pollsSinceKeyPress++;
            if (pollsSinceKeyPress > 10) {
                pollsSinceKeyPress = -1;
            }
        }
    }
}