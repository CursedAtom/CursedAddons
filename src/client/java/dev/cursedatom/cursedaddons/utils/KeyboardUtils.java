package dev.cursedatom.cursedaddons.utils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.cursedatom.cursedaddons.config.SpecialUnits;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Utility class for checking key and mouse button state with optional modifier key requirements.
 */
public class KeyboardUtils {
    private KeyboardUtils() {}

    public static boolean isKeyPressingWithModifier(String translationKey, SpecialUnits.KeyModifiers modifier) {
        if (InputConstants.UNKNOWN.getName().equals(translationKey)) {
            return false;
        }
        
        Window window = Minecraft.getInstance().getWindow();
        InputConstants.Key key = InputConstants.getKey(translationKey);
        int keyCode = key.getValue();

        // Require the specified modifier key to be held (returns false if it's not)
        if ((modifier.equals(SpecialUnits.KeyModifiers.ALT) &&
                !(InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT) ||
                        InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)))
        || (modifier.equals(SpecialUnits.KeyModifiers.SHIFT) &&
                !(InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                        InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)))
        || (modifier.equals(SpecialUnits.KeyModifiers.CTRL) &&
                !(InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)))
        ) {
            return false;
        }
        
        if (key.getType().equals(InputConstants.Type.KEYSYM)) {
            return InputConstants.isKeyDown(window, keyCode);
        } else if (key.getType().equals(InputConstants.Type.MOUSE)) {
            return GLFW.glfwGetMouseButton(window.handle(), keyCode) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
