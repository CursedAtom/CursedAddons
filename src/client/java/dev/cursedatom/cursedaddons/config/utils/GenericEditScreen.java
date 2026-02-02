package dev.cursedatom.cursedaddons.config.utils;

import dev.cursedatom.cursedaddons.config.ConfigScreenGenerator;
import dev.cursedatom.cursedaddons.config.ConfigScreen;
import dev.cursedatom.cursedaddons.config.WhitelistScreen;
import dev.cursedatom.cursedaddons.config.SpecialUnits;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericEditScreen extends Screen {
    private final Screen parent;
    private final AbstractConfigUnit editingUnit;
    private final int editingIndex;
    private final Class<?> unitClass;
    private final List<FieldDefinition> fieldDefinitions;

    private final Map<String, AbstractWidget> widgetMap = new HashMap<>();
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
        if (unitClass == SpecialUnits.MacroUnit.class) return "Macro";
        if (unitClass == SpecialUnits.AliasUnit.class) return "Alias";
        if (unitClass == SpecialUnits.NotificationUnit.class) return "Notification";
        if (unitClass == SpecialUnits.WhitelistUnit.class) return "Whitelist Domain";
        return "Unit";
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
        if ("chatkeybindings.Macro.List".equals(configKey) && unitClass == SpecialUnits.MacroUnit.class) return true;
        if ("commandaliases.Aliases.List".equals(configKey) && unitClass == SpecialUnits.AliasUnit.class) return true;
        if ("chatnotifications.Notifications.List".equals(configKey) && unitClass == SpecialUnits.NotificationUnit.class) return true;
        if ("general.ImageHoverPreview.Whitelist".equals(configKey) && unitClass == SpecialUnits.WhitelistUnit.class) return true;
        return false;
    }

    @Override
    protected void init() {
        super.init();

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
                widget = FieldWidgetFactory.createKeybindWidget(fieldDef, centerX, y, buttonWidth, buttonHeight, initialValue, (Button.OnPress) button -> {
                    if (this.waitingButton != null) {
                        this.waitingButton.stopWaiting();
                    }
                    KeybindButton keybindButton = (KeybindButton) button;
                    keybindButton.startWaiting();
                    this.waitingButton = keybindButton;
                });
            } else {
                widget = FieldWidgetFactory.createWidget(fieldDef, centerX, y, buttonWidth, buttonHeight, initialValue, this.font);
            }

            widgetMap.put(fieldDef.getName(), widget);
            this.addRenderableWidget(widget);
            y += ("text".equals(fieldDef.getType()) ? 30 : 25);
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
    public boolean keyPressed(final KeyEvent event) {
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
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
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

    private void saveUnit() {
        try {
            AbstractConfigUnit newUnit = (AbstractConfigUnit) unitClass.getDeclaredConstructor().newInstance();

            for (FieldDefinition fieldDef : fieldDefinitions) {
                AbstractWidget widget = widgetMap.get(fieldDef.getName());
                Object value = FieldWidgetFactory.getValueFromWidget(fieldDef, widget);
                if (value != null && FieldWidgetFactory.isValidValue(fieldDef, value)) {
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

            if (parent instanceof ConfigScreen) {
                ((ConfigScreen) parent).onUnitSaved(newUnit, editingIndex, unitClass);
            } else if (parent instanceof WhitelistScreen) {
                ((WhitelistScreen) parent).onUnitSaved(newUnit, editingIndex, unitClass);
            }
            this.onClose();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save unit", e);
        }
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