package dev.cursedatom.cursedaddons.config.utils;

import dev.cursedatom.cursedaddons.config.ConfigScreenGenerator;
import dev.cursedatom.cursedaddons.config.ConfigScreen;
import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.config.UnitTypeRegistry;
import dev.cursedatom.cursedaddons.CursedAddons;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * A generic edit screen for adding or modifying a single {@link AbstractConfigUnit} item.
 * Field definitions are loaded from {@code config_gui.json} and rendered as typed widgets
 * (text, toggle, keybind, cycle) via {@link FieldWidgetFactory}.
 */
public class GenericEditScreen extends Screen {
    private static final Map<String, Supplier<List<String>>> dropdownProviders = new HashMap<>();

    public static void registerDropdownProvider(String name, Supplier<List<String>> provider) {
        dropdownProviders.put(name, provider);
    }

    private final Screen parent;
    private final AbstractConfigUnit editingUnit;
    private final int editingIndex;
    private final Class<?> unitClass;
    private final List<FieldDefinition> fieldDefinitions;

    private final Map<String, AbstractWidget> widgetMap = new HashMap<>();
    private final List<DropdownSuggestor> dropdownSuggestors = new ArrayList<>();
    private FieldWidgetFactory widgetFactory;
    private KeybindButton waitingButton = null;
    private InputConstants.Key selectedKey = InputConstants.UNKNOWN;

    public GenericEditScreen(Screen parent, AbstractConfigUnit unit, int index, Class<?> unitClass) {
        super(Component.literal((unit == null ? "Add " : "Edit ") + getUnitTypeName(unitClass)));
        this.parent = parent;
        this.editingUnit = unit;
        this.editingIndex = index;
        this.unitClass = unitClass;
        this.fieldDefinitions = getFieldDefinitions(unitClass);
    }

    private static String getUnitTypeName(Class<?> unitClass) {
        return UnitTypeRegistry.getDisplayName(unitClass);
    }

    private static List<FieldDefinition> getFieldDefinitions(Class<?> unitClass) {
        ConfigGui configGui = ConfigScreenGenerator.getConfigGui();
        if (configGui != null && configGui.getCategories() != null) {
            for (Category category : configGui.getCategories()) {
                if (category.getContent() != null) {
                    for (ConfigItem item : category.getContent()) {
                        if ("list".equals(item.getType()) && item.getFields() != null) {
                            // Find the matching list item based on unit class
                            if (matchesUnitClass(item.getKey(), unitClass)) {
                                return item.getFields();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean matchesUnitClass(String configKey, Class<?> unitClass) {
        UnitTypeRegistry.Registration<?> reg = UnitTypeRegistry.get(configKey);
        return reg != null && reg.unitClass() == unitClass;
    }

    @Override
    protected void init() {
        super.init();
        widgetFactory = new FieldWidgetFactory();

        if (fieldDefinitions == null) return;

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = 20;
        int y = startY;

        // Load initial values if editing
        Map<String, Object> initialValues = new HashMap<>();
        if (editingUnit != null) {
            loadValuesFromUnit(initialValues);
        }

        // Create widgets based on field definitions
        for (FieldDefinition fieldDef : fieldDefinitions) {
            Object initialValue = initialValues.get(fieldDef.getName());
            AbstractWidget widget;

            // Handle keybind fields specially to wire up the waiting callback
            if ("keybind".equals(fieldDef.getType())) {
                widget = widgetFactory.createKeybindWidget(fieldDef, centerX, y, buttonWidth, buttonHeight, initialValue, (Button.OnPress) button -> {
                    if (this.waitingButton != null) {
                        this.waitingButton.stopWaiting();
                    }
                    KeybindButton keybindButton = (KeybindButton) button;
                    keybindButton.startWaiting();
                    this.waitingButton = keybindButton;
                });
            } else {
                widget = widgetFactory.createWidget(fieldDef, centerX, y, buttonWidth, buttonHeight, initialValue, this.font);
            }

            widgetMap.put(fieldDef.getName(), widget);
            this.addRenderableWidget(widget);
            y += ("text".equals(fieldDef.getType()) ? 30 : 25);
        }

        // Wire up dropdown suggestors for fields that have a dropdown provider
        dropdownSuggestors.clear();
        for (FieldDefinition fieldDef : fieldDefinitions) {
            if (fieldDef.getDropdown() != null) {
                Supplier<List<String>> provider = dropdownProviders.get(fieldDef.getDropdown());
                AbstractWidget widget = widgetMap.get(fieldDef.getName());
                if (provider != null && widget instanceof EditBox) {
                    EditBox box = (EditBox) widget;
                    DropdownSuggestor suggestor = new DropdownSuggestor(box, this.font, provider.get());
                    box.setResponder(suggestor::update);
                    dropdownSuggestors.add(suggestor);
                }
            }
        }

        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            saveUnit();
        }).bounds(centerX, y, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
            this.onClose();
        }).bounds(centerX, y + 25, buttonWidth, buttonHeight).build());
    }

    private void loadValuesFromUnit(Map<String, Object> initialValues) {
        try {
            for (Field field : editingUnit.getClass().getDeclaredFields()) {
                if (!field.canAccess(editingUnit)) {
                    field.setAccessible(true);
                }
                String fieldName = field.getName();
                Object value = field.get(editingUnit);
                initialValues.put(fieldName, value);

                // Set initial values for widgets that need them
                if (fieldName.equals("key") && value instanceof String) {
                    selectedKey = InputConstants.getKey((String) value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to load values from unit", e);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        for (DropdownSuggestor suggestor : dropdownSuggestors) {
            suggestor.render(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        // Let visible dropdown suggestors consume key events first
        for (DropdownSuggestor suggestor : dropdownSuggestors) {
            if (suggestor.isVisible() && suggestor.keyPressed(event.key())) {
                return true;
            }
        }

        if (this.waitingButton != null) {
            InputConstants.Key key;
            if (event.isEscape()) {
                key = InputConstants.UNKNOWN;
                this.waitingButton.setBoundKey(key);
            } else {
                key = InputConstants.getKey(event);
                this.waitingButton.setBoundKey(key);
            }
            this.waitingButton.stopWaiting();
            this.waitingButton = null;
            selectedKey = key;
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (DropdownSuggestor suggestor : dropdownSuggestors) {
            if (suggestor.mouseScrolled(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        // Let dropdown suggestors consume clicks first
        for (DropdownSuggestor suggestor : dropdownSuggestors) {
            if (suggestor.mouseClicked(event.x(), event.y())) {
                return true;
            }
        }

        if (this.waitingButton != null) {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.button());
            this.waitingButton.setBoundKey(key);
            this.waitingButton.stopWaiting();
            this.waitingButton = null;
            selectedKey = key;
            return true;
        } else {
            return super.mouseClicked(event, doubleClick);
        }
    }

    /**
     * Saves the current widget values back to a new unit instance using reflection.
     * For each field definition, retrieves the value from the corresponding widget,
     * converts enum fields from String to their enum type, applies domain sanitization
     * for whitelist entries, then notifies the parent {@link dev.cursedatom.cursedaddons.config.ConfigScreen}.
     */
    private void saveUnit() {
        try {
            AbstractConfigUnit newUnit = (AbstractConfigUnit) unitClass.getDeclaredConstructor().newInstance();

            for (FieldDefinition fieldDef : fieldDefinitions) {
                AbstractWidget widget = widgetMap.get(fieldDef.getName());
                Object value = widgetFactory.getValueFromWidget(fieldDef, widget);
                if (value != null && widgetFactory.isValidValue(fieldDef, value)) {
                    Field field = unitClass.getDeclaredField(fieldDef.getName());
                    if (!field.canAccess(newUnit)) {
                        field.setAccessible(true);
                    }

                    // Convert String to enum if field is an enum type
                    if (field.getType().isEnum() && value instanceof String) {
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        Enum enumValue = Enum.valueOf((Class<Enum>) field.getType(), (String) value);
                        value = enumValue;
                    }

                    if (unitClass == SpecialUnits.WhitelistUnit.class && "domain".equals(fieldDef.getName())) {
                        value = sanitizeDomain((String) value);
                    }
                    field.set(newUnit, value);
                }
            }

            // Special handling for key field
            if (selectedKey != null && selectedKey != InputConstants.UNKNOWN) {
                try {
                    Field keyField = unitClass.getDeclaredField("key");
                    if (!keyField.canAccess(newUnit)) {
                        keyField.setAccessible(true);
                    }
                    keyField.set(newUnit, selectedKey.getName());
                } catch (NoSuchFieldException ignored) {
                    // Unit doesn't have a key field (e.g. WhitelistUnit), ignore
                }
            }

            if (!isValidUnit(newUnit)) {
                return;
            }

            // Validate regex pattern for notifications
            if (newUnit instanceof SpecialUnits.NotificationUnit) {
                SpecialUnits.NotificationUnit notification = (SpecialUnits.NotificationUnit) newUnit;
                if (notification.regex && !notification.pattern.isEmpty()) {
                    try {
                        Pattern.compile(notification.pattern);
                    } catch (Exception e) {
                        CursedAddons.LOGGER.error("[CursedAddons] Invalid regex pattern: " + e.getMessage());
                        return;
                    }
                }
            }

            if (parent instanceof ConfigScreen) {
                ((ConfigScreen) parent).onUnitSaved(newUnit, editingIndex, unitClass);
            }
            this.onClose();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save unit", e);
        }
    }

    /**
     * Strips protocol ({@code http://}, {@code https://}), path, and port from user-entered domain input
     * so that only the bare hostname is stored in the whitelist.
     */
    private static String sanitizeDomain(String domain) {
        if (domain == null) return "";
        domain = domain.trim();
        if (domain.startsWith("http://")) domain = domain.substring(7);
        else if (domain.startsWith("https://")) domain = domain.substring(8);
        int slashIdx = domain.indexOf('/');
        if (slashIdx >= 0) domain = domain.substring(0, slashIdx);
        int colonIdx = domain.indexOf(':');
        if (colonIdx >= 0) domain = domain.substring(0, colonIdx);
        return domain.trim();
    }

    private boolean isValidUnit(AbstractConfigUnit unit) {
        try {
            for (FieldDefinition fieldDef : fieldDefinitions) {
                if ("text".equals(fieldDef.getType()) && Boolean.TRUE.equals(fieldDef.getRequired())) {
                    Field field = unitClass.getDeclaredField(fieldDef.getName());
                    if (!field.canAccess(unit)) {
                        field.setAccessible(true);
                    }
                    Object value = field.get(unit);
                    if (value instanceof String && ((String) value).trim().isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}