package dev.cursedatom.cursedaddons.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class KeybindButton extends Button {
    private InputConstants.Key boundKey = InputConstants.UNKNOWN;
    private boolean waiting = false;

    public KeybindButton(int x, int y, int width, int height, InputConstants.Key initialKey, OnPress onPress) {
        super(x, y, width, height, getDisplayMessage(initialKey), onPress, DEFAULT_NARRATION);
        this.boundKey = initialKey;
    }

    private static Component getDisplayMessage(InputConstants.Key key) {
        return key == InputConstants.UNKNOWN ? Component.translatable("gui.none") : key.getDisplayName();
    }

    public void updateDisplay() {
        if (waiting) {
            this.setMessage(Component.translatable("key.cursedaddons.chatkeybindings.controls.keybind.waiting"));
        } else {
            this.setMessage(getDisplayMessage(boundKey));
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
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderDefaultSprite(guiGraphics);
        this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
    }
}
