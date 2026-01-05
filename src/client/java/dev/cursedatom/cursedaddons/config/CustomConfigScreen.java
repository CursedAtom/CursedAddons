package dev.cursedatom.cursedaddons.config;

import dev.cursedatom.cursedaddons.utils.ConfigUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;
import java.util.Map;

import static dev.cursedatom.cursedaddons.utils.TextUtils.trans;

public class CustomConfigScreen extends Screen {
    private final Screen parent;
    private final Map<String, Object> configGuiMap;
    private String selectedCategory;

    // List managers for different unit types
    private ListManager<SpecialUnits.MacroUnit> macroManager;
    private ListManager<SpecialUnits.AliasUnit> aliasManager;
    private ListManager<SpecialUnits.NotificationUnit> notificationManager;

    public CustomConfigScreen(Screen parent) {
        super(trans("gui.title"));
        this.parent = parent;
        this.configGuiMap = ConfigScreenGenerator.getConfigGuiMap();
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
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categories = (List<Map<String, Object>>) configGuiMap.get("categories");
        if (categories != null && !categories.isEmpty()) {
            selectedCategory = (String) categories.get(0).get("name");
        }
    }

    private Class<?> getUnitClass(String type) {
        switch (type) {
            case "MacroList": return SpecialUnits.MacroUnit.class;
            case "AliasList": return SpecialUnits.AliasUnit.class;
            case "NotificationList": return SpecialUnits.NotificationUnit.class;
            default: return null;
        }
    }



    @Override
    protected void init() {
        super.init();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categories = (List<Map<String, Object>>) configGuiMap.get("categories");
        if (categories == null) return;

        // Tab buttons at the top
        int tabWidth = 120;
        int tabHeight = 20;
        int startX = this.width / 2 - (categories.size() * tabWidth) / 2;
        int tabY = 30;

        for (int i = 0; i < categories.size(); i++) {
            final String categoryName = (String) categories.get(i).get("name");
            boolean isSelected = categoryName.equals(selectedCategory);
            Button tabButton = Button.builder(trans("gui.category." + categoryName.toLowerCase().replace(" ", "")), button -> {
                selectedCategory = categoryName;
                this.clearWidgets();
                this.init();
            }).bounds(startX + i * tabWidth, tabY, tabWidth, tabHeight).build();

            if (isSelected) {
                tabButton.setMessage(Component.literal("[ " + tabButton.getMessage().getString() + " ]"));
            }

            this.addRenderableWidget(tabButton);
        }

        // Content area below tabs
        int contentY = tabY + tabHeight + 20;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;

        // Find selected category and add content
        for (Map<String, Object> category : categories) {
            String categoryName = (String) category.get("name");
            if (!categoryName.equals(selectedCategory)) continue;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) category.get("content");
            if (content == null) continue;

            for (Map<String, Object> item : content) {
                String type = (String) item.get("type");
                String key = (String) item.get("key");

                if ("boolean".equals(type)) {
                    boolean value = (boolean) ConfigUtils.get(key);
                    this.addRenderableWidget(Button.builder(
                        Component.literal(trans(key).getString() + ": " + (value ? "§aON" : "§cOFF")),
                        button -> {
                            ConfigUtils.set(key, !value);
                            this.clearWidgets();
                            this.init();
                        }).bounds(centerX, contentY, buttonWidth, buttonHeight).build());
                    contentY += 25;
                } else if (type.endsWith("List")) {
                    Class<?> unitClass = getUnitClass(type);
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
            ConfigUtils.save();
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
        // Draw semi-transparent black background
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000); // Semi-transparent black

        // Draw slightly larger dark gray background for the config area
        int bgX = this.width / 2 - 250;
        int bgY = 20;
        int bgWidth = 500;
        int bgHeight = this.height - 40;
        guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xAA030303); // Transparent dark gray... #030303
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
