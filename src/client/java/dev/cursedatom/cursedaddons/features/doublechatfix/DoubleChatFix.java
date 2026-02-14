package dev.cursedatom.cursedaddons.features.doublechatfix;

import dev.cursedatom.cursedaddons.features.doublechatfix.callback.KeyboardCharTypedCallback;
import dev.cursedatom.cursedaddons.features.doublechatfix.callback.KeyboardKeyPressedCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

/**
 * Fixes the "double character" bug where opening a chat-related screen (chat, command, inventory search)
 * causes the activating key character to be typed into the input field. Intercepts the char-typed event
 * immediately following a chat-open key press and suppresses it.
 */
public class DoubleChatFix {
    private DoubleChatFix() {}

    private static boolean cancelNextChar = false;

    public static void init() {
        KeyboardKeyPressedCallback.EVENT.register((window, keyCode, keyEvent) -> {
            if (keyEvent.isUp() || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                return InteractionResult.PASS;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.screen != null) {
                return InteractionResult.PASS;
            }

            if (client.options.keyChat.matches(keyEvent) ||
                client.options.keyCommand.matches(keyEvent) ||
                client.options.keyInventory.matches(keyEvent)) { // this one is for the creative search bar
                cancelNextChar = true;
            }

            return InteractionResult.PASS;
        });

        KeyboardCharTypedCallback.EVENT.register((window, characterEvent) -> {
            if (cancelNextChar) {
                cancelNextChar = false;
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}