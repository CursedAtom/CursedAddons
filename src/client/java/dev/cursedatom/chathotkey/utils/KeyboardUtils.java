package dev.cursedatom.chathotkey.utils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.cursedatom.chathotkey.config.SpecialUnits;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyboardUtils {
    public static boolean isKeyPressingWithModifier(String translationKey, SpecialUnits.KeyModifiers modifier) {
        if (InputConstants.UNKNOWN.getName().equals(translationKey)) {
            return false;
        }
        
        Window window = Minecraft.getInstance().getWindow();
        InputConstants.Key key = InputConstants.getKey(translationKey);
        int keyCode = key.getValue();

        // This check is GREEDY
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
