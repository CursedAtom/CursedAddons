package dev.cursedatom.cursedaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.cursedatom.cursedaddons.utils.ConfigUtils;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigStorage {
    public static final File FILE = new File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                                                                                 .toFile(), "cursedaddons.json");

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

    // combine two hashmaps
    public ConfigStorage withDefault(Map<String, Object> defaultMap) {
        for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
            configMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        if (defaultMap.containsKey("config.version")) {
            configMap.put("config.version", defaultMap.get("config.version"));
        }
        return this;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public void readConfigFile(boolean loadDefault) {
        try {
            Reader reader;
            if (loadDefault) {
                reader = new InputStreamReader(ConfigStorage.class.getClassLoader()
                                                        .getResourceAsStream("assets/cursedaddons/default_config.json"));
            } else {
                reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8);
            }
            configMap = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object get(String key) {
        if (this.hasKey(key)) {
            return configMap.get(key);
        } else if (ConfigUtils.DEFAULT_CONFIG != null && ConfigUtils.DEFAULT_CONFIG.hasKey(key)) {
            return ConfigUtils.DEFAULT_CONFIG.get(key);
        } else {
            LoggerUtils.error("[CursedAddons] Error occurred when getting variable \"" + key + "\", no such key!");
            return null;
        }
    }

    public boolean hasKey(String key) {
        return this.configMap != null && this.configMap.get(key) != null;
    }

    public void set(String variableName, Object value) {
        if (configMap != null) {
            configMap.put(variableName, value);
        }
    }

    public void save() {
        if (configMap == null) return;
        LoggerUtils.info("[CursedAddons] Saving configs.");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
            GSON.toJson(configMap, writer);
        } catch (Exception e) {
            LoggerUtils.error("[CursedAddons] Couldn't save config.");
            e.printStackTrace();
        }
    }
}
