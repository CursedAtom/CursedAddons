package dev.cursedatom.cursedaddons.config;

import dev.cursedatom.cursedaddons.config.utils.AbstractConfigUnit;
import dev.cursedatom.cursedaddons.config.utils.ConfigGui;
import dev.cursedatom.cursedaddons.config.utils.ListManager;
import dev.cursedatom.cursedaddons.config.utils.GenericEditScreen;
import dev.cursedatom.cursedaddons.config.utils.Category;
import dev.cursedatom.cursedaddons.config.utils.ConfigItem;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

import static dev.cursedatom.cursedaddons.utils.TextUtils.trans;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final ConfigGui configGui;
    private String selectedCategory;

    // List managers for different unit types
    private ListManager<SpecialUnits.MacroUnit> macroManager;
    private ListManager<SpecialUnits.AliasUnit> aliasManager;
    private ListManager<SpecialUnits.NotificationUnit> notificationManager;
    private ListManager<SpecialUnits.WhitelistUnit> whitelistManager;
    private Button openWhitelistButton;

    public ConfigScreen(Screen parent) {
        super(trans("gui.title"));
        this.parent = parent;
        this.configGui = ConfigScreenGenerator.getConfigGui();
        initializeSelectedCategory();
        initializeListManagers();
    }

    private void initializeListManagers() {
        macroManager = new ListManager<>(
            "chatkeybindings.Macro.List",
            SpecialUnits.MacroUnit.class,
            this::formatMacroDisplay,
            this::formatMacroTooltip,
            item -> this.minecraft.setScreen(new GenericEditScreen(this, item, macroManager.getSelectedIndex(), SpecialUnits.MacroUnit.class)),
            (item, index) -> onUnitSaved(item, index, SpecialUnits.MacroUnit.class)
        );

        aliasManager = new ListManager<>(
            "commandaliases.Aliases.List",
            SpecialUnits.AliasUnit.class,
            this::formatAliasDisplay,
            this::formatAliasTooltip,
            item -> this.minecraft.setScreen(new GenericEditScreen(this, item, aliasManager.getSelectedIndex(), SpecialUnits.AliasUnit.class)),
            (item, index) -> onUnitSaved(item, index, SpecialUnits.AliasUnit.class)
        );

        notificationManager = new ListManager<>(
            "chatnotifications.Notifications.List",
            SpecialUnits.NotificationUnit.class,
            this::formatNotificationDisplay,
            this::formatNotificationTooltip,
            item -> this.minecraft.setScreen(new GenericEditScreen(this, item, notificationManager.getSelectedIndex(), SpecialUnits.NotificationUnit.class)),
            (item, index) -> onUnitSaved(item, index, SpecialUnits.NotificationUnit.class)
        );

        whitelistManager = new ListManager<>(
            "general.ImageHoverPreview.Whitelist",
            SpecialUnits.WhitelistUnit.class,
            this::formatWhitelistDisplay,
            this::formatWhitelistTooltip,
            item -> this.minecraft.setScreen(new GenericEditScreen(this, item, whitelistManager.getSelectedIndex(), SpecialUnits.WhitelistUnit.class)),
            (item, index) -> onWhitelistSaved(item, index)
        );
    }

    private String formatMacroDisplay(SpecialUnits.MacroUnit macro) {
        InputConstants.Key key = InputConstants.getKey(macro.key);
        String keyName = key.getDisplayName().getString();
        return keyName + (macro.modifier != SpecialUnits.KeyModifiers.NONE ? " + " + macro.modifier : "") + " -> " + macro.command;
    }

    private String formatMacroTooltip(SpecialUnits.MacroUnit macro) {
        InputConstants.Key key = InputConstants.getKey(macro.key);
        String keyName = key.getDisplayName().getString();
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("§bKey: §f").append(keyName);
        if (macro.modifier != SpecialUnits.KeyModifiers.NONE) {
            tooltip.append("\n§bModifier: §f").append(macro.modifier);
        }
        tooltip.append("\n§dCommand: §f").append(macro.command);
        return tooltip.toString();
    }

    private String formatAliasDisplay(SpecialUnits.AliasUnit alias) {
        return alias.alias + " -> " + alias.replacement;
    }

    private String formatAliasTooltip(SpecialUnits.AliasUnit alias) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("§bAlias: §f").append(alias.alias);
        tooltip.append("\n§dReplacement: §f").append(alias.replacement);
        return tooltip.toString();
    }

    private String formatNotificationDisplay(SpecialUnits.NotificationUnit notification) {
        String actions = (notification.soundEnabled ? "§e[SOUND]§r " : "") +
                         (notification.titleEnabled ? "§9[TITLE]§r " : "") +
                         (notification.commandEnabled ? "§d[CMD]§r " : "");
        return (notification.regex ? "REGEX: " : "TEXT: ") + notification.pattern + " " + actions.trim();
    }

    private String formatNotificationTooltip(SpecialUnits.NotificationUnit notification) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("§bPattern: §f").append(notification.pattern);
        tooltip.append("\n§bType: §f").append(notification.regex ? "Regex" : "Text");

        if (notification.soundEnabled && !notification.sound.isEmpty()) {
            tooltip.append("\n§eSound: §f").append(notification.sound);
        }
        if (notification.titleEnabled && !notification.title.isEmpty()) {
            tooltip.append("\n§9Title: §f").append(notification.title);
        }
        if (notification.commandEnabled && !notification.command.isEmpty()) {
            tooltip.append("\n§dCommand: §f").append(notification.command);
        }
        return tooltip.toString();
    }

    private void initializeSelectedCategory() {
        List<Category> categories = configGui.getCategories();
        if (categories != null && !categories.isEmpty()) {
            selectedCategory = trans(categories.get(0).getNameKey()).getString();
        }
    }

    private Class<?> getUnitClassForList(String key) {
        if ("chatkeybindings.Macro.List".equals(key)) return SpecialUnits.MacroUnit.class;
        if ("commandaliases.Aliases.List".equals(key)) return SpecialUnits.AliasUnit.class;
        if ("chatnotifications.Notifications.List".equals(key)) return SpecialUnits.NotificationUnit.class;
        if ("general.ImageHoverPreview.Whitelist".equals(key)) return SpecialUnits.WhitelistUnit.class;
        return null;
    }

    private String formatWhitelistDisplay(SpecialUnits.WhitelistUnit whitelist) {
        return whitelist.domain;
    }

    private String formatWhitelistTooltip(SpecialUnits.WhitelistUnit whitelist) {
        return "§7Domain: §f" + whitelist.domain;
    }

    private void onWhitelistSaved(SpecialUnits.WhitelistUnit unit, int index) {
        // Whitelist items are saved directly through the manager
    }



    @Override
    protected void init() {
        super.init();

        List<Category> categories = configGui.getCategories();
        if (categories == null) return;

        // Tab buttons at the top
        int tabWidth = 120;
        int tabHeight = 20;
        int startX = this.width / 2 - (categories.size() * tabWidth) / 2;
        int tabY = 30;

        for (int i = 0; i < categories.size(); i++) {
            final Category category = categories.get(i);
            final String categoryName = trans(category.getNameKey()).getString();
            boolean isSelected = categoryName.equals(selectedCategory);
            Button tabButton = Button.builder(trans(category.getNameKey()), button -> {
                selectedCategory = categoryName;
                this.clearWidgets();
                this.init();
            }).bounds(startX + i * tabWidth, tabY, tabWidth, tabHeight).build();

            if (isSelected) {
                tabButton.setMessage(Component.literal("§6§l[§r " + tabButton.getMessage().getString() + " §6§l]§r"));
            }

            this.addRenderableWidget(tabButton);
        }

        // Content area below tabs
        int contentY = tabY + tabHeight + 20;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;

        // Find selected category and add content
        for (Category category : categories) {
            String categoryName = trans(category.getNameKey()).getString();
            if (!categoryName.equals(selectedCategory)) continue;

            List<ConfigItem> content = category.getContent();
            if (content == null) continue;

            for (ConfigItem item : content) {
                String type = item.getType();
                String key = item.getKey();

                if ("boolean".equals(type)) {
                    boolean value = (boolean) ConfigProvider.get(key);
                    String label = item.getLabelKey() != null ? trans(item.getLabelKey()).getString() : trans(key).getString();
                    this.addRenderableWidget(Button.builder(
                        Component.literal(label + ": " + (value ? "§aON" : "§cOFF")),
                        button -> {
                            ConfigProvider.set(key, !value);
                            this.clearWidgets();
                            this.init();
                        }).bounds(centerX, contentY, buttonWidth, buttonHeight).build());
                    contentY += 25;
                } else if ("button".equals(type)) {
                    // Handle button type
                    String buttonLabel = item.getLabelKey() != null ? trans(item.getLabelKey()).getString() : trans(key).getString();
                    openWhitelistButton = Button.builder(Component.literal(buttonLabel), button -> {
                        // Open the whitelist editor
                        this.minecraft.setScreen(new WhitelistScreen(this));
                    }).bounds(centerX, contentY, buttonWidth, buttonHeight).build();
                    this.addRenderableWidget(openWhitelistButton);
                    contentY += 25;
                } else if ("list".equals(type) && !item.isHidden()) {
                    Class<?> unitClass = getUnitClassForList(key);
                    if (unitClass != null) {
                        addListManagement(contentY, centerX, buttonWidth, buttonHeight, key, unitClass);
                        contentY += 200; // Estimate for list height
                    }
                }
            }
            break;
        }

        // Done button at the bottom
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            ConfigProvider.save();
            this.onClose();
        }).bounds(centerX, this.height - 40, buttonWidth, buttonHeight).build());
    }

    private void addListManagement(int startY, int centerX, int buttonWidth, int buttonHeight, String configKey, Class<?> unitClass) {
        Runnable refreshScreen = () -> {
            this.clearWidgets();
            this.init();
        };

        List<AbstractWidget> widgets;
        if (unitClass == SpecialUnits.MacroUnit.class) {
            widgets = macroManager.getScrollableListWidgets(this.minecraft, startY, centerX, buttonWidth, buttonHeight, refreshScreen);
        } else if (unitClass == SpecialUnits.AliasUnit.class) {
            widgets = aliasManager.getScrollableListWidgets(this.minecraft, startY, centerX, buttonWidth, buttonHeight, refreshScreen);
        } else if (unitClass == SpecialUnits.NotificationUnit.class) {
            widgets = notificationManager.getScrollableListWidgets(this.minecraft, startY, centerX, buttonWidth, buttonHeight, refreshScreen);
        } else if (unitClass == SpecialUnits.WhitelistUnit.class) {
            widgets = whitelistManager.getScrollableListWidgets(this.minecraft, startY, centerX, buttonWidth, buttonHeight, refreshScreen);
        } else {
            return;
        }

        for (AbstractWidget widget : widgets) {
            this.addRenderableWidget(widget);
        }
    }

    public void onUnitSaved(AbstractConfigUnit unit, int index, Class<?> unitClass) {
        if (unitClass == SpecialUnits.MacroUnit.class) {
            macroManager.saveItem((SpecialUnits.MacroUnit) unit, index);
        } else if (unitClass == SpecialUnits.AliasUnit.class) {
            aliasManager.saveItem((SpecialUnits.AliasUnit) unit, index);
        } else if (unitClass == SpecialUnits.NotificationUnit.class) {
            notificationManager.saveItem((SpecialUnits.NotificationUnit) unit, index);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw semi-transparent black background (darkens the screen)
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000);

        // Draw overlayed slightly darker background for the config area (shows bounds of the config window)
        int bgX = this.width / 2 - 250;
        int bgY = 20;
        int bgWidth = 500;
        int bgHeight = this.height - 40;
        guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xAA000000);
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }
}
