package dev.cursedatom.chathotkey.features.chatkeybindings;

import dev.cursedatom.chathotkey.config.SpecialUnits;
import dev.cursedatom.chathotkey.utils.ConfigUtils;
import dev.cursedatom.chathotkey.utils.KeyboardUtils;
import dev.cursedatom.chathotkey.utils.LoggerUtils;
import dev.cursedatom.chathotkey.utils.MessageUtils;
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
        
        for (SpecialUnits.MacroUnit macro : SpecialUnits.MacroUnit.fromList((List) macroListObj)) {
            if (macro.enabled && KeyboardUtils.isKeyPressingWithModifier(macro.key, macro.modifier)) {
                if (!keyWasPressed.contains(macro)) {
                    keyWasPressed.add(macro);
                    LoggerUtils.info("[ChatHotkey] Triggered Macro: " + macro.command);
                    MessageUtils.sendToPublicChat(macro.command);
                }
            } else {
                keyWasPressed.remove(macro);
            }
        }
    }
}
