package dev.cursedatom.cursedaddons.features.chatkeybindings;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.utils.ConfigUtils;
import dev.cursedatom.cursedaddons.utils.KeyboardUtils;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import dev.cursedatom.cursedaddons.utils.MessageUtils;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Macro {
    static Set<SpecialUnits.MacroUnit> keyWasPressed = new HashSet<>();

    public static void tick() {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        
        Object macroListObj = ConfigUtils.get("chatkeybindings.Macro.List");
        if (macroListObj == null) return;

        @SuppressWarnings("unchecked")
        List<Object> macroList = (List<Object>) macroListObj;
        for (SpecialUnits.MacroUnit macro : SpecialUnits.MacroUnit.fromList(macroList)) {
            if (macro.enabled && KeyboardUtils.isKeyPressingWithModifier(macro.key, macro.modifier)) {
                if (!keyWasPressed.contains(macro)) {
                    keyWasPressed.add(macro);
                    LoggerUtils.info("[CursedAddons] Triggered Macro: " + macro.command);
                    MessageUtils.sendToPublicChat(macro.command);
                }
            } else {
                keyWasPressed.remove(macro);
            }
        }
    }
}
