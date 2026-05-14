package dev.cursedatom.cursedaddons.config.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import static dev.cursedatom.cursedaddons.utils.TextUtils.trans;

/**
 * A {@link Button} that displays a bound key and enters a "waiting" state to capture a new key press.
 * Used in {@link dev.cursedatom.cursedaddons.config.utils.GenericEditScreen} for keybind fields.
 */
public class KeybindButton extends Button {
    private InputConstants.Key boundKey = InputConstants.UNKNOWN;
    private boolean waiting = false;
    private final String label;

    public KeybindButton(int x, int y, int width, int height, String label, InputConstants.Key initialKey, OnPress onPress) {
        super(x, y, width, height, getDisplayMessage(label, initialKey), onPress, DEFAULT_NARRATION);
        this.boundKey = initialKey;
        this.label = label;
    }

    private static Component getDisplayMessage(String label, InputConstants.Key key) {
        Component keyComp = key == InputConstants.UNKNOWN ? Component.translatable("gui.none") : key.getDisplayName();
        return Component.literal(label + ": ").append(keyComp);
    }

    public void updateDisplay() {
        if (waiting) {
            this.setMessage(Component.literal(label + ": ").append(trans("chatkeybindings.controls.keybind.waiting")));
        } else {
            this.setMessage(getDisplayMessage(label, boundKey));
        }
    }

    public void startWaiting() {
        this.waiting = true;
        updateDisplay();
    }

    public void stopWaiting() {
        this.waiting = false;
        updateDisplay();
    }

    public void setBoundKey(InputConstants.Key key) {
        this.boundKey = key;
        updateDisplay();
    }

    public InputConstants.Key getBoundKey() {
        return boundKey;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(guiGraphics);
        extractDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
