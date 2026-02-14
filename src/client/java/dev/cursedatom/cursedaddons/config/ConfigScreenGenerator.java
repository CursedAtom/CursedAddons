package dev.cursedatom.cursedaddons.config;

import dev.cursedatom.cursedaddons.config.utils.ConfigGui;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cursedatom.cursedaddons.CursedAddons;
import net.minecraft.client.gui.screens.Screen;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Loads and caches the config GUI definition from {@code config_gui.json}, and provides
 * a factory method for creating a {@link ConfigScreen}.
 */
public class ConfigScreenGenerator {
    private static ConfigGui configGui;
    public static boolean configGuiInitialized = false;
    private static final Gson GSON = new GsonBuilder().create();

    public static void loadConfigGui() {
        try {
            InputStream inputStream = ConfigScreenGenerator.class.getClassLoader().getResourceAsStream("assets/cursedaddons/config_gui.json");
            if (inputStream == null) {
                CursedAddons.LOGGER.error("config_gui.json not found in assets/cursedaddons/");
                return;
            }
            Reader reader = new InputStreamReader(inputStream);
            configGui = GSON.fromJson(reader, ConfigGui.class);
            configGuiInitialized = true;
        } catch (Exception e) {
            CursedAddons.LOGGER.error("Failed to load config_gui.json: " + e.getMessage());
        }
    }

    public static ConfigGui getConfigGui() {
        if (!configGuiInitialized) {
            loadConfigGui();
        }
        return configGui;
    }

    public static Screen getConfigScreen(Screen parent) {
        if (!configGuiInitialized) {
            loadConfigGui();
        }

        return new ConfigScreen(parent);
    }
}
