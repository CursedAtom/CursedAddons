package dev.cursedatom.cursedaddons.features.doublechatfix;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFWCharModsCallbackI;

/**
 * Callback for typing a character in a GUI.
 * Called before the character is processed.
 *
 * <p>Upon return:
 * <ul><li>SUCCESS/CONSUME cancels further listener processing and forwards the character to be processed by the client.
 * <li>PASS falls back to further processing.
 * <li>FAIL cancels further processing and does not forward the character.</ul>
 */
public interface KeyboardCharTypedCallback {
    Event<KeyboardCharTypedCallback> EVENT = EventFactory.createArrayBacked(KeyboardCharTypedCallback.class,
            listeners -> (window, codepoint, modifiers) -> {
                for (KeyboardCharTypedCallback listener : listeners) {
                    InteractionResult result = listener.onCharTyped(window, codepoint, modifiers);

                    if (result != InteractionResult.PASS)
                        return result;
                }

                return InteractionResult.PASS;
            });

    /**
     * Will be called when a Unicode character is input regardless of what modifier keys are used.
     *
     * @param window the window that received the event
     * @param codepoint the Unicode code point of the character
     * @param modifiers bitfield describing which modifier keys were held down
     * @see GLFWCharModsCallbackI#invoke(long, int, int)
     */
    InteractionResult onCharTyped(long window, int codepoint, int modifiers);
}
