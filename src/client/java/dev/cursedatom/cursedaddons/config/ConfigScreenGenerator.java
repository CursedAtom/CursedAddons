package dev.cursedatom.cursedaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.cursedatom.cursedaddons.utils.ConfigScreenUtils;
import dev.cursedatom.cursedaddons.utils.ConfigUtils;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import static dev.cursedatom.cursedaddons.utils.TextUtils.trans;

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

    public static ConfigBuilder getConfigBuilder() {
        if (!configGuiMapInitialized) {
            loadConfigGuiMap();
        }

        ResourceLocation backgroundTexture = ResourceLocation.parse("minecraft:textures/block/oak_planks.png");
        ConfigBuilder builder = ConfigBuilder.create().setTitle(trans("gui.title"))
                                             .setDefaultBackgroundTexture(backgroundTexture)
                                             .setTransparentBackground(true).setSavingRunnable(ConfigUtils::save);
        ConfigEntryBuilder eb = builder.entryBuilder();

        if (configGuiMap != null && configGuiMap.containsKey("categories")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) configGuiMap.get("categories");
            for (Map<String, Object> categoryMap : categories) {
                String categoryName = (String) categoryMap.get("name");
                ConfigCategory category = builder.getOrCreateCategory(trans("gui.category." + categoryName.toLowerCase().replace(" ", "")));

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> entries = (List<Map<String, Object>>) categoryMap.get("content");
                for (Map<String, Object> elementMap : entries) {
                    String type = (String) elementMap.get("type");
                    String key = (String) elementMap.get("key");
                    String errorSupplier = (String) elementMap.getOrDefault("errorSupplier", "null");

                    category.addEntry(ConfigScreenUtils.getEntryBuilder(eb, type, key, errorSupplier));
                }
            }
        } else {
            LoggerUtils.warn("configGuiMap is null or does not contain 'categories' key. Config screen will be empty.");
        }

        return builder;
    }
}
