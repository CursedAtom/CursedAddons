package dev.cursedatom.cursedaddons.config;

import dev.cursedatom.cursedaddons.config.utils.AbstractConfigUnit;
import dev.cursedatom.cursedaddons.config.utils.GenericEditScreen;
import dev.cursedatom.cursedaddons.config.utils.ListManager;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class WhitelistScreen extends Screen {
    private final Screen parent;
    private ListManager<SpecialUnits.WhitelistUnit> whitelistManager;

    public WhitelistScreen(Screen parent) {
        super(Component.translatable("general.ImageHoverPreview.Whitelist"));
        this.parent = parent;
        initializeWhitelistManager();
    }

    private void initializeWhitelistManager() {
        whitelistManager = new ListManager<>(
            "general.ImageHoverPreview.Whitelist",
            SpecialUnits.WhitelistUnit.class,
            this::formatWhitelistDisplay,
            this::formatWhitelistTooltip,
            // Edit action: Open GenericEditScreen
            item -> this.minecraft.setScreen(new GenericEditScreen(this, item, whitelistManager.getSelectedIndex(), SpecialUnits.WhitelistUnit.class)),
            // Save action: Handled by onUnitSaved
            (item, index) -> {}
        );
    }

    private String formatWhitelistDisplay(SpecialUnits.WhitelistUnit whitelist) {
        return whitelist.domain;
    }

    private String formatWhitelistTooltip(SpecialUnits.WhitelistUnit whitelist) {
        return "§7Domain: §f" + whitelist.domain;
    }

    /**
     * Called by GenericEditScreen when a unit is saved.
     */
    public void onUnitSaved(AbstractConfigUnit unit, int index, Class<?> unitClass) {
        if (unitClass == SpecialUnits.WhitelistUnit.class) {
            whitelistManager.saveItem((SpecialUnits.WhitelistUnit) unit, index);
        }
    }

    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = 40; // Position below the title

        // Logic to refresh the list (called by ListManager buttons)
        Runnable refreshScreen = () -> {
            this.clearWidgets();
            this.init();
        };

        // Get the standard list widgets (Edit/Add/Delete buttons + Scrollable List)
        List<AbstractWidget> widgets = whitelistManager.getScrollableListWidgets(
            this.minecraft, 
            startY, 
            centerX, 
            buttonWidth, 
            buttonHeight, 
            refreshScreen
        );

        for (AbstractWidget widget : widgets) {
            this.addRenderableWidget(widget);
        }

        // Add "Done" button at the bottom
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            ConfigProvider.save();
            this.onClose();
        }).bounds(centerX, this.height - 30, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw semi-transparent black background (darkens the screen)
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000);

        // Draw overlayed slightly darker background for the config area (consistent with ConfigScreen)
        int bgX = this.width / 2 - 250;
        int bgY = 20;
        int bgWidth = 500;
        int bgHeight = this.height - 40;
        guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xAA000000);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}