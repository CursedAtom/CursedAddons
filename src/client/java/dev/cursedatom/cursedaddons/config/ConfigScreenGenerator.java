package dev.cursedatom.cursedaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import net.minecraft.client.gui.screens.Screen;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;


public class ConfigScreenGenerator {
    private static Map<String, Object> configGuiMap;
    public static boolean configGuiMapInitialized = false;
    private static final Gson GSON = new GsonBuilder().create();

    public static void loadConfigGuiMap() {
        try {
            InputStream inputStream = ConfigScreenGenerator.class.getClassLoader()
                                                     .getResourceAsStream("assets/cursedaddons/config_gui.json");
            if (inputStream == null) {
                LoggerUtils.error("config_gui.json not found in assets/cursedaddons/");
                return;
            }
            Reader reader = new InputStreamReader(inputStream);
            configGuiMap = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
            configGuiMapInitialized = true;
        } catch (Exception e) {
            LoggerUtils.error("Failed to load config_gui.json: " + e.getMessage());
        }
    }

    public static Map<String, Object> getConfigGuiMap() {
        if (!configGuiMapInitialized) {
            loadConfigGuiMap();
        }
        return configGuiMap;
    }

    public static Screen getConfigScreen(Screen parent) {
        if (!configGuiMapInitialized) {
            loadConfigGuiMap();
        }

        return new CustomConfigScreen(parent);
    }
}
