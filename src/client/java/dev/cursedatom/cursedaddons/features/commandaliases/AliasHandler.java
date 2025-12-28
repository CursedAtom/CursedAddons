package dev.cursedatom.cursedaddons.features.commandaliases;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.utils.ConfigUtils;

import java.util.List;

public class AliasHandler {
    public static String processMessage(String message) {
        if (!message.startsWith("/")) {
            return message;
        }

        Object enabledObj = ConfigUtils.get("commandaliases.Aliases.Enabled");
        if (enabledObj == null || !(boolean) enabledObj) {
            return message;
        }

        Object aliasListObj = ConfigUtils.get("commandaliases.Aliases.List");
        if (aliasListObj == null) {
            return message;
        }

        @SuppressWarnings("unchecked")
        List<Object> aliasList = (List<Object>) aliasListObj;
        List<SpecialUnits.AliasUnit> aliases = SpecialUnits.AliasUnit.fromList(aliasList);

        for (SpecialUnits.AliasUnit alias : aliases) {
            if (alias.enabled && !alias.alias.isEmpty() && message.startsWith(alias.alias + " ")) {
                String replaced = message.replaceFirst(alias.alias, alias.replacement);
                return replaced;
            } else if (alias.enabled && !alias.alias.isEmpty() && message.equals(alias.alias)) {
                // Exact match
                String replaced = alias.replacement;
                return replaced;
            }
        }

        return message;
    }
}
