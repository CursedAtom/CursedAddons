package dev.cursedatom.cursedaddons.utils;

import dev.cursedatom.cursedaddons.config.utils.ConfigStorage;

import java.util.Collections;
import java.util.List;

/**
 * Static facade for accessing and modifying mod config values at runtime.
 * Wraps a {@link ConfigStorage} loaded from disk and one loaded from the bundled defaults.
 * A monotonically increasing {@code version} counter allows caches to detect staleness.
 */
public class ConfigProvider {
    public static ConfigStorage DEFAULT_CONFIG;
    public static ConfigStorage CONFIG;
    private static volatile long version = 0;

    public static long getVersion() {
        return version;
    }

    public static void init() {
        if (DEFAULT_CONFIG == null) {
            DEFAULT_CONFIG = new ConfigStorage(true);
        }

        if (!ConfigStorage.configFileExists()) {
            // if the config file doesn't exist, create a new one with the default settings.
            DEFAULT_CONFIG.save();
        }

        CONFIG = new ConfigStorage(false).withDefault(DEFAULT_CONFIG.getHashmap());
    }

    public static void save() {
        if (CONFIG != null) {
            CONFIG.save();
            version++;
        }
    }

    public static Object get(String key) {
        return CONFIG != null ? CONFIG.get(key) : (DEFAULT_CONFIG != null ? DEFAULT_CONFIG.get(key) : null);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(String key) {
        Object value = get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return Collections.emptyList();
    }

    public static void set(String key, Object value) {
        if (CONFIG != null) {
            CONFIG.set(key, value);
            version++;
        }
    }
}
