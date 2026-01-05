package dev.cursedatom.cursedaddons.config;

import dev.cursedatom.cursedaddons.config.FieldDescriptor.FieldType;
import dev.cursedatom.cursedaddons.config.SpecialUnits.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnitType {
    private static final Map<Class<?>, List<FieldDescriptor>> descriptors = new HashMap<>();

    static {
        // MacroUnit fields
        descriptors.put(MacroUnit.class, Arrays.asList(
            new FieldDescriptor("key", FieldType.KEYBIND),
            new FieldDescriptor("modifier", FieldType.CYCLE, Arrays.asList("NONE", "SHIFT", "ALT", "CTRL")),
            new FieldDescriptor("command", FieldType.TEXT, "Command to execute", 1000),
            new FieldDescriptor("enabled", FieldType.TOGGLE)
        ));

        // AliasUnit fields
        descriptors.put(AliasUnit.class, Arrays.asList(
            new FieldDescriptor("alias", FieldType.TEXT, "Command alias", 1000),
            new FieldDescriptor("replacement", FieldType.TEXT, "Replacement command", 1000),
            new FieldDescriptor("enabled", FieldType.TOGGLE)
        ));

        // NotificationUnit fields
        descriptors.put(NotificationUnit.class, Arrays.asList(
            new FieldDescriptor("pattern", FieldType.TEXT, "Text or regex pattern to match", 2000),
            new FieldDescriptor("regex", FieldType.TOGGLE),
            new FieldDescriptor("sound", FieldType.TEXT, "Sound identifier", 1000),
            new FieldDescriptor("soundEnabled", FieldType.TOGGLE),
            new FieldDescriptor("title", FieldType.TEXT, "Title to display", 1000),
            new FieldDescriptor("titleEnabled", FieldType.TOGGLE),
            new FieldDescriptor("command", FieldType.TEXT, "Command to execute", 1000),
            new FieldDescriptor("commandEnabled", FieldType.TOGGLE),
            new FieldDescriptor("enabled", FieldType.TOGGLE)
        ));
    }

    public static List<FieldDescriptor> getDescriptors(Class<?> unitClass) {
        return descriptors.get(unitClass);
    }

    public static String getTypeName(Class<?> unitClass) {
        if (unitClass == MacroUnit.class) return "Macro";
        if (unitClass == AliasUnit.class) return "Alias";
        if (unitClass == NotificationUnit.class) return "Notification";
        return "Unit";
    }

    public static String getListName(Class<?> unitClass) {
        if (unitClass == MacroUnit.class) return "Macros";
        if (unitClass == AliasUnit.class) return "Aliases";
        if (unitClass == NotificationUnit.class) return "Notifications";
        return "List";
    }
}
