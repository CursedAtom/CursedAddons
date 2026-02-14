package dev.cursedatom.cursedaddons.config.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

import static dev.cursedatom.cursedaddons.utils.TextUtils.trans;

/**
 * Creates the appropriate widget for each field type defined in {@code config_gui.json}
 * and provides methods to read back the current widget value.
 */
public class FieldWidgetFactory {
    // cycleIndices and toggleStates track per-widget state between creation and save
    private final Map<String, Integer> cycleIndices = new HashMap<>();
    private final Map<AbstractWidget, Boolean> toggleStates = new HashMap<>();

    public AbstractWidget createWidget(FieldDefinition fieldDef, int x, int y, int width, int height, Object initialValue, Font font) {
        switch (fieldDef.getType()) {
            case "text":
                return createTextWidget(fieldDef, x, y, width, height, initialValue, font);
            case "toggle":
                return createToggleWidget(fieldDef, x, y, width, height, initialValue);
            case "keybind":
                return createKeybindWidget(fieldDef, x, y, width, height, initialValue);
            case "cycle":
                return createCycleWidget(fieldDef, x, y, width, height, initialValue);
            default:
                return createDefaultWidget(fieldDef, x, y, width, height);
        }
    }

    private AbstractWidget createTextWidget(FieldDefinition fieldDef, int x, int y, int width, int height, Object initialValue, Font font) {
        EditBox editBox = new EditBox(font, x, y, width, height, trans(fieldDef.getLabelKey()));
        if (fieldDef.getHintKey() != null) {
            editBox.setHint(Component.literal(trans(fieldDef.getHintKey()).getString()));
        }
        if (fieldDef.getMaxLength() != null) {
            editBox.setMaxLength(fieldDef.getMaxLength());
        }
        if (initialValue instanceof String) {
            editBox.setValue((String) initialValue);
        }
        return editBox;
    }

    private AbstractWidget createToggleWidget(FieldDefinition fieldDef, int x, int y, int width, int height, Object initialValue) {
        boolean currentValue = fieldDef.getDefaultValue() instanceof Boolean ? (Boolean) fieldDef.getDefaultValue() : false;
        if (initialValue instanceof Boolean) {
            currentValue = (Boolean) initialValue;
        }
        final boolean initialToggle = currentValue;
        Button toggleButton = Button.builder(
            Component.literal(trans(fieldDef.getLabelKey()).getString() + ": " + (initialToggle ? "ON" : "OFF")),
            button -> {
                boolean newValue = !toggleStates.get(button);
                toggleStates.put(button, newValue);
                button.setMessage(Component.literal(trans(fieldDef.getLabelKey()).getString() + ": " + (newValue ? "ON" : "OFF")));
            }
        ).bounds(x, y, width, height).build();
        toggleStates.put(toggleButton, initialToggle);
        return toggleButton;
    }

    private AbstractWidget createKeybindWidget(FieldDefinition fieldDef, int x, int y, int width, int height, Object initialValue) {
        return createKeybindWidget(fieldDef, x, y, width, height, initialValue, button -> {
            // Default empty callback - caller should provide their own
        });
    }

    public KeybindButton createKeybindWidget(FieldDefinition fieldDef, int x, int y, int width, int height, Object initialValue, Button.OnPress onPress) {
        InputConstants.Key initialKey = InputConstants.UNKNOWN;
        if (initialValue instanceof String) {
            initialKey = InputConstants.getKey((String) initialValue);
        }
        return new KeybindButton(x, y, width, height, initialKey, onPress);
    }

    private AbstractWidget createCycleWidget(FieldDefinition fieldDef, int x, int y, int width, int height, Object initialValue) {
        String fieldKey = fieldDef.getName();
        int initialIndex = 0;
        if (initialValue != null && fieldDef.getOptions() != null) {
            String valueStr = initialValue.toString();
            initialIndex = fieldDef.getOptions().indexOf(valueStr);
            if (initialIndex == -1) initialIndex = 0;
        }
        cycleIndices.put(fieldKey, initialIndex);

        String displayText = getCycleDisplayText(fieldDef, initialIndex);
        Button cycleButton = Button.builder(
            Component.literal(trans(fieldDef.getLabelKey()).getString() + ": " + displayText),
            button -> {
                int currentIndex = cycleIndices.get(fieldKey);
                currentIndex = (currentIndex + 1) % fieldDef.getOptions().size();
                cycleIndices.put(fieldKey, currentIndex);
                String newDisplayText = getCycleDisplayText(fieldDef, currentIndex);
                button.setMessage(Component.literal(trans(fieldDef.getLabelKey()).getString() + ": " + newDisplayText));
            }
        ).bounds(x, y, width, height).build();
        return cycleButton;
    }

    private static String getCycleDisplayText(FieldDefinition fieldDef, int index) {
        if (fieldDef.getOptionKeys() != null && index < fieldDef.getOptionKeys().size()) {
            return trans(fieldDef.getOptionKeys().get(index)).getString();
        } else if (fieldDef.getOptions() != null && index < fieldDef.getOptions().size()) {
            return fieldDef.getOptions().get(index);
        }
        return "Unknown";
    }

    private static AbstractWidget createDefaultWidget(FieldDefinition fieldDef, int x, int y, int width, int height) {
        return Button.builder(Component.literal("Unknown field type: " + fieldDef.getType()), button -> {}).bounds(x, y, width, height).build();
    }

    public Object getValueFromWidget(FieldDefinition fieldDef, AbstractWidget widget) {
        switch (fieldDef.getType()) {
            case "text":
                if (widget instanceof EditBox) {
                    return ((EditBox) widget).getValue().trim();
                }
                break;
            case "toggle":
                Boolean state = toggleStates.get(widget);
                if (state != null) {
                    return state;
                }
                break;
            case "keybind":
                if (widget instanceof KeybindButton) {
                    return ((KeybindButton) widget).getBoundKey().getName();
                }
                break;
            case "cycle":
                Integer currentIndex = cycleIndices.get(fieldDef.getName());
                if (currentIndex != null && fieldDef.getOptions() != null && currentIndex < fieldDef.getOptions().size()) {
                    return fieldDef.getOptions().get(currentIndex);
                }
                break;
        }
        return null;
    }

    public boolean isValidValue(FieldDefinition fieldDef, Object value) {
        if (fieldDef.getRequired() != null && fieldDef.getRequired()) {
            if (value == null) return false;
            if (value instanceof String && ((String) value).trim().isEmpty()) return false;
        }
        return true;
    }
}
