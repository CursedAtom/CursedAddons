package dev.cursedatom.cursedaddons.config;

import dev.cursedatom.cursedaddons.config.utils.AbstractConfigUnit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps config keys to their corresponding {@link AbstractConfigUnit} subclasses and display names.
 * Used by {@link dev.cursedatom.cursedaddons.config.ConfigScreen} and
 * {@link dev.cursedatom.cursedaddons.config.utils.GenericEditScreen} to look up unit types by key.
 */
public final class UnitTypeRegistry {

    public record Registration<T extends AbstractConfigUnit>(Class<T> unitClass, String displayName) {}

    private static final Map<String, Registration<?>> REGISTRY = new LinkedHashMap<>();

    static {
        register(ConfigKeys.MACRO_LIST, SpecialUnits.MacroUnit.class, "Macro");
        register(ConfigKeys.ALIASES_LIST, SpecialUnits.AliasUnit.class, "Alias");
        register(ConfigKeys.NOTIFICATIONS_LIST, SpecialUnits.NotificationUnit.class, "Notification");
        register(ConfigKeys.IMAGE_WHITELIST, SpecialUnits.WhitelistUnit.class, "Whitelist Domain");
    }

    private static <T extends AbstractConfigUnit> void register(String configKey, Class<T> unitClass, String displayName) {
        REGISTRY.put(configKey, new Registration<>(unitClass, displayName));
    }

    public static Registration<?> get(String configKey) {
        return REGISTRY.get(configKey);
    }

    public static Map<String, Registration<?>> all() {
        return REGISTRY;
    }

    public static String getDisplayName(Class<?> unitClass) {
        for (Registration<?> reg : REGISTRY.values()) {
            if (reg.unitClass() == unitClass) {
                return reg.displayName();
            }
        }
        return "Unit";
    }

    public static String getConfigKey(Class<?> unitClass) {
        for (Map.Entry<String, Registration<?>> entry : REGISTRY.entrySet()) {
            if (entry.getValue().unitClass() == unitClass) {
                return entry.getKey();
            }
        }
        return null;
    }

    private UnitTypeRegistry() {}
}
