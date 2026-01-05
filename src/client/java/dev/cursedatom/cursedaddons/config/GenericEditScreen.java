package dev.cursedatom.cursedaddons.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cursedatom.cursedaddons.utils.TextUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private final List<FieldDescriptor> descriptors;

    private final Map<String, AbstractWidget> widgetMap = new HashMap<>();
    private final Map<String, Integer> cycleIndices = new HashMap<>();
    private KeybindButton waitingButton = null;
    private InputConstants.Key selectedKey = InputConstants.UNKNOWN;

    public GenericEditScreen(Screen parent, AbstractConfigUnit unit, int index, Class<?> unitClass) {
        super(Component.literal((unit == null ? "Add " : "Edit ") + UnitType.getTypeName(unitClass)));
        this.parent = parent;
        this.editingUnit = unit;
        this.editingIndex = index;
        this.unitClass = unitClass;
        this.descriptors = UnitType.getDescriptors(unitClass);
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = 20;
        int y = startY;

        // Load initial values if editing
        if (editingUnit != null) {
            loadValuesFromUnit();
        }

        // Create widgets based on descriptors
        for (FieldDescriptor desc : descriptors) {
            AbstractWidget widget = createWidgetForDescriptor(desc, centerX, y, buttonWidth, buttonHeight);
            widgetMap.put(desc.fieldName, widget);
            this.addRenderableWidget(widget);
            y += (desc.type == FieldDescriptor.FieldType.TEXT ? 30 : 25);
        }

        // Save button
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            saveUnit();
        }).bounds(centerX, y, buttonWidth, buttonHeight).build());

        // Cancel button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
            this.onClose();
        }).bounds(centerX, y + 25, buttonWidth, buttonHeight).build());
    }

    private void loadValuesFromUnit() {
        try {
            for (Field field : editingUnit.getClass().getDeclaredFields()) {
                if (!field.canAccess(editingUnit)) {
                    field.setAccessible(true);
                }
                String fieldName = field.getName();
                Object value = field.get(editingUnit);

                // Set initial values for widgets that need them
                if (fieldName.equals("key") && value instanceof String) {
                    selectedKey = InputConstants.getKey((String) value);
                }
                // Other fields will be loaded in createWidgetForDescriptor
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to load values from unit", e);
        }
    }

    private AbstractWidget createWidgetForDescriptor(FieldDescriptor desc, int x, int y, int width, int height) {
        switch (desc.type) {
            case TEXT:
                EditBox editBox = new EditBox(this.font, x, y, width, height, TextUtils.trans("config.field." + desc.fieldName));
                if (desc.hint != null) {
                    editBox.setHint(Component.literal(desc.hint));
                }
                editBox.setMaxLength(desc.maxLength);

                // Load value if editing
                if (editingUnit != null) {
                    try {
                        Field field = editingUnit.getClass().getDeclaredField(desc.fieldName);
                        if (!field.canAccess(editingUnit)) {
                            field.setAccessible(true);
                        }
                        Object value = field.get(editingUnit);
                        if (value instanceof String) {
                            editBox.setValue((String) value);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                return editBox;

            case TOGGLE:
                boolean initialValue = false;
                if (editingUnit != null) {
                    try {
                        Field field = editingUnit.getClass().getDeclaredField(desc.fieldName);
                        if (!field.canAccess(editingUnit)) {
                            field.setAccessible(true);
                        }
                        Object value = field.get(editingUnit);
                        if (value instanceof Boolean) {
                            initialValue = (Boolean) value;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                final boolean[] currentValue = {initialValue};
                Button toggleButton = Button.builder(Component.literal(TextUtils.trans("config.field." + desc.fieldName).getString() + ": " + (currentValue[0] ? "ON" : "OFF")), button -> {
                    currentValue[0] = !currentValue[0];
                    button.setMessage(Component.literal(TextUtils.trans("config.field." + desc.fieldName).getString() + ": " + (currentValue[0] ? "ON" : "OFF")));
                }).bounds(x, y, width, height).build();
                return toggleButton;

            case KEYBIND:
                KeybindButton keyButton = new KeybindButton(x, y, width, height, selectedKey, button -> {
                    if (this.waitingButton != null) {
                        this.waitingButton.stopWaiting();
                    }
                    this.waitingButton = (KeybindButton) button;
                    ((KeybindButton) button).startWaiting();
                    this.setFocused(button);
                });
                return keyButton;

            case CYCLE:
                int initialIndex = 0;
                if (editingUnit != null) {
                    try {
                        Field field = editingUnit.getClass().getDeclaredField(desc.fieldName);
                        if (!field.canAccess(editingUnit)) {
                            field.setAccessible(true);
                        }
                        Object value = field.get(editingUnit);
                        if (value != null) {
                            String valueStr = value.toString();
                            initialIndex = desc.cycleOptions.indexOf(valueStr);
                            if (initialIndex == -1) initialIndex = 0;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                cycleIndices.put(desc.fieldName, initialIndex);
                Button cycleButton = Button.builder(Component.literal(TextUtils.trans("config.field." + desc.fieldName).getString() + ": " + TextUtils.trans("config.modifier." + desc.cycleOptions.get(initialIndex)).getString()), button -> {
                    int currentIndex = cycleIndices.get(desc.fieldName);
                    currentIndex = (currentIndex + 1) % desc.cycleOptions.size();
                    cycleIndices.put(desc.fieldName, currentIndex);
                    button.setMessage(Component.literal(TextUtils.trans("config.field." + desc.fieldName).getString() + ": " + TextUtils.trans("config.modifier." + desc.cycleOptions.get(currentIndex)).getString()));
                }).bounds(x, y, width, height).build();
                return cycleButton;

            default:
                return Button.builder(Component.literal("Unknown field type"), button -> {}).bounds(x, y, width, height).build();
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

            for (FieldDescriptor desc : descriptors) {
                AbstractWidget widget = widgetMap.get(desc.fieldName);
                Object value = getValueFromWidget(desc, widget);
                if (value != null) {
                    Field field = unitClass.getDeclaredField(desc.fieldName);
                    if (!field.canAccess(newUnit)) {
                        field.setAccessible(true);
                    }
                    field.set(newUnit, value);
                }
            }

            // Special handling for key field
            if (selectedKey != null && selectedKey != InputConstants.UNKNOWN) {
                Field keyField = unitClass.getDeclaredField("key");
                if (!keyField.canAccess(newUnit)) {
                    keyField.setAccessible(true);
                }
                keyField.set(newUnit, selectedKey.getName());
            }

            // Basic validation
            if (!isValidUnit(newUnit)) {
                return;
            }

            if (parent instanceof CustomConfigScreen) {
                ((CustomConfigScreen) parent).onUnitSaved(newUnit, editingIndex, unitClass);
            }
            this.onClose();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save unit", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getValueFromWidget(FieldDescriptor desc, AbstractWidget widget) {
        switch (desc.type) {
            case TEXT:
                if (widget instanceof EditBox) {
                    return ((EditBox) widget).getValue();
                }
                break;
            case TOGGLE:
                if (widget instanceof Button) {
                    String message = ((Button) widget).getMessage().getString();
                    return message.endsWith("ON");
                }
                break;
            case KEYBIND:
                // Handled separately
                break;
            case CYCLE:
                Integer currentIndex = cycleIndices.get(desc.fieldName);
                if (currentIndex != null && currentIndex < desc.cycleOptions.size()) {
                    String enumName = desc.cycleOptions.get(currentIndex);
                    try {
                        Field field = unitClass.getDeclaredField(desc.fieldName);
                        Class<?> fieldType = field.getType();
                        if (fieldType.isEnum()) {
                            @SuppressWarnings({ "rawtypes" })
                            Class<? extends Enum> enumClass = (Class<? extends Enum>) fieldType;
                            @SuppressWarnings("rawtypes")
                            Enum value = Enum.valueOf(enumClass, enumName);
                            return value;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                break;
        }
        return null;
    }

    private boolean isValidUnit(AbstractConfigUnit unit) {
        try {
            // Basic validation: check required fields are not empty
            for (FieldDescriptor desc : descriptors) {
                if (desc.type == FieldDescriptor.FieldType.TEXT) {
                    Field field = unitClass.getDeclaredField(desc.fieldName);
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
