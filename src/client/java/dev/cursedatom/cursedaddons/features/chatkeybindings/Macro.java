package dev.cursedatom.cursedaddons.features.chatkeybindings;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import dev.cursedatom.cursedaddons.utils.KeyboardUtils;
import dev.cursedatom.cursedaddons.utils.MessageUtils;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles chat keybinding macros — sends configured commands when bound keys are pressed.
 * Config values are cached and invalidated by {@link dev.cursedatom.cursedaddons.utils.ConfigProvider#getVersion()}.
 */
public class Macro {
    private Macro() {}

    static Set<SpecialUnits.MacroUnit> keyWasPressed = new HashSet<>();

    private static long cachedVersion = -1;
    private static boolean cachedEnabled = false;
    private static List<SpecialUnits.MacroUnit> cachedMacros = List.of();

    private static void refreshCache() {
        long currentVersion = ConfigProvider.getVersion();
        if (currentVersion != cachedVersion) {
            cachedVersion = currentVersion;
            cachedEnabled = ConfigProvider.getBoolean(ConfigKeys.MACRO_ENABLED, false);
            List<Object> rawList = ConfigProvider.getList(ConfigKeys.MACRO_LIST);
            cachedMacros = rawList.isEmpty() ? List.of() : SpecialUnits.MacroUnit.fromList(rawList);
        }
    }

    public static boolean isEnabled() {
        refreshCache();
        return cachedEnabled;
    }

    public static void tick() {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        refreshCache();
        if (cachedMacros.isEmpty()) return;
        for (SpecialUnits.MacroUnit macro : cachedMacros) {
            if (macro.enabled && KeyboardUtils.isKeyPressingWithModifier(macro.key, macro.modifier)) {
                if (!keyWasPressed.contains(macro)) {
                    keyWasPressed.add(macro);
                    MessageUtils.sendToPublicChat(macro.command);
                }
            } else {
                keyWasPressed.remove(macro);
            }
        }
    }
}
