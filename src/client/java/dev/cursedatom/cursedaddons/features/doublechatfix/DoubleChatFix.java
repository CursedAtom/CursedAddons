package dev.cursedatom.cursedaddons.features.doublechatfix;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class DoubleChatFix {
    private static long pollCount;
    private static long prevKeyPoll = -1L;

    public static void init() {
        GLFWPollCallback.EVENT.register(() -> pollCount++);

        // Key press events are always processed before char type events
        KeyboardKeyPressedCallback.EVENT.register((window, key, scancode, action, modifiers) -> {
            Minecraft client = Minecraft.getInstance();
            // If this is a key release/repeat OR we're already in a screen (including chat screen), skip
            if (action != GLFW.GLFW_PRESS || client.screen != null)
                return InteractionResult.PASS;

            // If the chat or command key was pressed, store what poll count it happened on.
            if (matchesKey(client.options.keyChat, key, scancode) || matchesKey(client.options.keyCommand, key, scancode)) {
                prevKeyPoll = pollCount;
            } else {
                // Otherwise, set to -1
                prevKeyPoll = -1L;
            }

            return InteractionResult.PASS;
        });
        KeyboardCharTypedCallback.EVENT.register((window, codepoint, modifiers) -> {
            // If the previous key poll is -1 or the poll count doesn't match up closely, skip
            if (prevKeyPoll == -1 || pollCount - prevKeyPoll > 5)
                return InteractionResult.PASS;

            // If we are on a close poll count to when the key press event was polled,
            // then we should cancel the char type event to ensure it is not passed to the chat field.
            prevKeyPoll = -1;
            return InteractionResult.FAIL;
        });
    }

    private static boolean matchesKey(KeyMapping keyMapping, int key, int scancode) {
        InputConstants.Key boundKey = InputConstants.getKey(keyMapping.saveString());
        if (key == InputConstants.UNKNOWN.getValue()) {
            return boundKey.getType() == InputConstants.Type.SCANCODE && boundKey.getValue() == scancode;
        }
        return boundKey.getType() == InputConstants.Type.KEYSYM && boundKey.getValue() == key;
    }
}
