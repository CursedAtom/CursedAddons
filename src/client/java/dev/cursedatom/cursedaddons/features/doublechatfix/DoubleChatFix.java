package dev.cursedatom.cursedaddons.features.doublechatfix;

import dev.cursedatom.cursedaddons.features.doublechatfix.callback.GLFWPollCallback;
import dev.cursedatom.cursedaddons.features.doublechatfix.callback.KeyboardCharTypedCallback;
import dev.cursedatom.cursedaddons.features.doublechatfix.callback.KeyboardKeyPressedCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class DoubleChatFix {
    private static long pollCount;
    private static long prevKeyPoll;

    public static void init() {
        GLFWPollCallback.EVENT.register(() -> pollCount++);

        // Key press events are always processed before char type events
        KeyboardKeyPressedCallback.EVENT.register((window, keyCode, keyEvent) -> {
            Minecraft client = Minecraft.getInstance();
            // If this is a key release/repeat OR we're already in a screen (including chat screen), skip
            if (keyEvent.isUp() || client.screen != null || keyCode == GLFW.GLFW_KEY_ENTER)
                return InteractionResult.PASS;

            // If the chat or command key was pressed, store what poll count it happened on. Same when opening inventory in creative mode ( Inv. open key could be typed into the creative search field )
            if (client.options.keyChat.matches(keyEvent) ||
                client.options.keyCommand.matches(keyEvent) ||
                client.options.keyInventory.matches(keyEvent)) {
                prevKeyPoll = pollCount;
            } else {
                // Otherwise, set to -1
                prevKeyPoll = -1L;
            }

            return InteractionResult.PASS;
        });

        KeyboardCharTypedCallback.EVENT.register((window, keyChar) -> {
            // If the previous key poll is -1 or the poll count doesn't match up closely, skip
            if (prevKeyPoll == -1 || pollCount - prevKeyPoll > 5)
                return InteractionResult.PASS;

            // If we are on a close poll count to when the key press event was polled,
            // then we should cancel the char type event to ensure it is not passed to the chat field.
            prevKeyPoll = -1;
            return InteractionResult.FAIL;
        });
    }


}
