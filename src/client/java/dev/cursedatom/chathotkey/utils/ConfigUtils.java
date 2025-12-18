package dev.cursedatom.chathotkey.utils;

import dev.cursedatom.chathotkey.config.ConfigStorage;

public class ConfigUtils {
    public static ConfigStorage DEFAULT_CONFIG;
    public static ConfigStorage CONFIG;

    public static void init(){
        // Initialize DEFAULT_CONFIG here to avoid static initialization issues if resources aren't ready
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
        if (CONFIG != null) CONFIG.save();
    }

    public static Object get(String var){
        return CONFIG != null ? CONFIG.get(var) : (DEFAULT_CONFIG != null ? DEFAULT_CONFIG.get(var) : null);
    }

    public static void set(String key, Object value) {
        if (CONFIG != null) CONFIG.set(key, value);
    }
}
