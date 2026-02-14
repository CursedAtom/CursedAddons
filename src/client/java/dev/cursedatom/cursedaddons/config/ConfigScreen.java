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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static dev.cursedatom.cursedaddons.utils.TextUtils.trans;

/**
 * Main config screen for CursedAddons, rendered as a tabbed UI with scrollable list managers
 * for each feature's configurable item lists.
 *
 * <p>Minecraft section-sign (§) color codes used in format methods:
 * §b = aqua (label), §f = white (value), §d = light purple (command/accent),
 * §e = yellow (sound), §9 = blue (title), §7 = gray (subdued label).
 */
public class ConfigScreen extends Screen {
    private static final int TAB_WIDTH = 120;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_Y = 30;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BG_COLOR = 0xAA000000;
    private static final int BG_WIDTH = 640;

    private final Screen parent;
    private final ConfigGui configGui;
    private String selectedCategory;

    private final Map<String, ListManager<?>> managers = new LinkedHashMap<>();

    public ConfigScreen(Screen parent) {
        super(trans("gui.title"));
        this.parent = parent;
        this.configGui = ConfigScreenGenerator.getConfigGui();
        initializeSelectedCategory();
        initializeListManagers();
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractConfigUnit> void registerManager(String configKey, Class<T> unitClass,
            Function<T, String> displayFmt, Function<T, String> tooltipFmt) {
        ListManager<T>[] managerRef = new ListManager[1]; // capture for lambda
        managerRef[0] = new ListManager<>(
            configKey, unitClass, displayFmt, tooltipFmt,
            item -> this.minecraft.setScreen(new GenericEditScreen(this, item, managerRef[0].getSelectedIndex(), unitClass))
        );
        managers.put(configKey, managerRef[0]);
    }

    private void initializeListManagers() {
        registerManager(ConfigKeys.MACRO_LIST, SpecialUnits.MacroUnit.class,
            this::formatMacroDisplay, this::formatMacroTooltip);
        registerManager(ConfigKeys.ALIASES_LIST, SpecialUnits.AliasUnit.class,
            this::formatAliasDisplay, this::formatAliasTooltip);
        registerManager(ConfigKeys.NOTIFICATIONS_LIST, SpecialUnits.NotificationUnit.class,
            this::formatNotificationDisplay, this::formatNotificationTooltip);
        registerManager(ConfigKeys.IMAGE_WHITELIST, SpecialUnits.WhitelistUnit.class,
            this::formatWhitelistDisplay, this::formatWhitelistTooltip);
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

    private String formatWhitelistDisplay(SpecialUnits.WhitelistUnit whitelist) {
        String embedTag = whitelist.resolveEmbed ? " §b[EMBED]§r" : "";
        return whitelist.domain + embedTag;
    }

    private String formatWhitelistTooltip(SpecialUnits.WhitelistUnit whitelist) {
        return "§7Domain: §f" + whitelist.domain + "\n§bResolve Embed: §f" + (whitelist.resolveEmbed ? "Yes" : "No");
    }

    private void initializeSelectedCategory() {
        List<Category> categories = configGui.getCategories();
        if (categories != null && !categories.isEmpty()) {
            selectedCategory = trans(categories.get(0).getNameKey()).getString();
        }
    }

    @Override
    protected void init() {
        super.init();

        List<Category> categories = configGui.getCategories();
        if (categories == null) return;

        // Tab buttons at the top
        int startX = this.width / 2 - (categories.size() * TAB_WIDTH) / 2;

        for (int i = 0; i < categories.size(); i++) {
            final Category category = categories.get(i);
            final String categoryName = trans(category.getNameKey()).getString();
            boolean isSelected = categoryName.equals(selectedCategory);
            Button tabButton = Button.builder(trans(category.getNameKey()), button -> {
                selectedCategory = categoryName;
                this.clearWidgets();
                this.init();
            }).bounds(startX + i * TAB_WIDTH, TAB_Y, TAB_WIDTH, TAB_HEIGHT).build();

            if (isSelected) {
                tabButton.setMessage(Component.literal("§6§l[§r " + tabButton.getMessage().getString() + " §6§l]§r"));
            }

            this.addRenderableWidget(tabButton);
        }

        // Content area below tabs
        int contentY = TAB_Y + TAB_HEIGHT + 20;
        int centerX = this.width / 2 - BUTTON_WIDTH / 2;

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
                    boolean value = ConfigProvider.getBoolean(key, false);
                    String label = item.getLabelKey() != null ? trans(item.getLabelKey()).getString() : trans(key).getString();
                    this.addRenderableWidget(Button.builder(
                        Component.literal(label + ": " + (value ? "§aON" : "§cOFF")),
                        button -> {
                            ConfigProvider.set(key, !value);
                            this.clearWidgets();
                            this.init();
                        }).bounds(centerX, contentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
                    contentY += 25;
                } else if ("list".equals(type) && !item.isHidden()) {
                    ListManager<?> manager = managers.get(key);
                    if (manager != null) {
                        addListManagement(contentY, centerX, manager);
                        contentY += 200;
                    }
                }
            }
            break;
        }

        // Done button at the bottom
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            ConfigProvider.save();
            this.onClose();
        }).bounds(centerX, this.height - 40, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void addListManagement(int startY, int centerX, ListManager<?> manager) {
        Runnable refreshScreen = () -> {
            this.clearWidgets();
            this.init();
        };

        List<AbstractWidget> widgets = manager.getScrollableListWidgets(this.minecraft, startY, centerX, BUTTON_WIDTH, BUTTON_HEIGHT, refreshScreen);
        for (AbstractWidget widget : widgets) {
            this.addRenderableWidget(widget);
        }
    }

    @SuppressWarnings("unchecked")
    public void onUnitSaved(AbstractConfigUnit unit, int index, Class<?> unitClass) {
        String configKey = UnitTypeRegistry.getConfigKey(unitClass);
        if (configKey != null) {
            ListManager<AbstractConfigUnit> manager = (ListManager<AbstractConfigUnit>) managers.get(configKey);
            if (manager != null) {
                manager.saveItem(unit, index);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Two overlapping fills: first darkens the entire screen, second marks the config panel bounds.
        guiGraphics.fill(0, 0, this.width, this.height, BG_COLOR);

        int bgX = this.width / 2 - BG_WIDTH / 2;
        int bgY = 20;
        int bgHeight = this.height - 40;
        guiGraphics.fill(bgX, bgY, bgX + BG_WIDTH, bgY + bgHeight, BG_COLOR);
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
