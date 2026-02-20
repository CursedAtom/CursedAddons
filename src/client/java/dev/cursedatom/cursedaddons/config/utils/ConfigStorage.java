package dev.cursedatom.cursedaddons.config.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.cursedatom.cursedaddons.CursedAddons;
import dev.cursedatom.cursedaddons.config.ConfigKeys;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Reads and writes the mod's JSON config file, and provides key-value access to config entries.
 * Can load either the bundled default config or the user's config file on disk.
 */
public class ConfigStorage {
    public static final File FILE = new File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile(), "cursedaddons.json");

    private Map<String, Object> configMap;

    public static boolean configFileExists() {
        return FILE.exists();
    }

    public ConfigStorage(boolean useDefault) {
        readConfigFile(useDefault);
    }

    public Map<String, Object> getHashmap() {
        return configMap;
    }

    public ConfigStorage withDefault(Map<String, Object> defaultMap) {
        for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
            configMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        if (defaultMap.containsKey(ConfigKeys.CONFIG_VERSION)) {
            configMap.put(ConfigKeys.CONFIG_VERSION, defaultMap.get(ConfigKeys.CONFIG_VERSION));
        }
        return this;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public void readConfigFile(boolean loadDefault) {
        try {
            if (loadDefault) {
                try (Reader reader = new InputStreamReader(ConfigStorage.class.getClassLoader()
                                                        .getResourceAsStream("assets/cursedaddons/default_config.json"))) {
                    configMap = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
                }
            } else {
                try (Reader reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
                    configMap = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
                }
            }
            if (configMap == null) {
                CursedAddons.LOGGER.error("[CursedAddons] Config file was empty or null — resetting to defaults");
                configMap = new java.util.HashMap<>();
            }
        } catch (Exception e) {
            CursedAddons.LOGGER.warn("[CursedAddons] Failed to load config file: " + e.getMessage());
            configMap = new java.util.HashMap<>();
        }
    }

    public Object get(String key) {
        return configMap != null ? configMap.get(key) : null;
    }

    public boolean hasKey(String key) {
        return this.configMap != null && this.configMap.containsKey(key);
    }

    public void set(String key, Object value) {
        if (configMap != null) {
            configMap.put(key, value);
        }
    }

    public void save() {
        if (configMap == null) return;
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
            GSON.toJson(configMap, writer);
        } catch (Exception e) {
            CursedAddons.LOGGER.error("[CursedAddons] Couldn't save config: " + e.getMessage());
        }
    }
}
