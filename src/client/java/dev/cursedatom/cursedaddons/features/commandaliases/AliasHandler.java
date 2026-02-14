package dev.cursedatom.cursedaddons.features.commandaliases;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;

import java.util.List;

/**
 * Processes outgoing chat messages and rewrites commands that match a configured alias.
 */
public class AliasHandler {
    private AliasHandler() {}

    public static String processMessage(String message) {
        if (!message.startsWith("/")) {
            return message;
        }

        if (!ConfigProvider.getBoolean(ConfigKeys.ALIASES_ENABLED, false)) {
            return message;
        }

        List<Object> aliasList = ConfigProvider.getList(ConfigKeys.ALIASES_LIST);
        if (aliasList.isEmpty()) {
            return message;
        }
        List<SpecialUnits.AliasUnit> aliases = SpecialUnits.AliasUnit.fromList(aliasList);

        for (SpecialUnits.AliasUnit alias : aliases) {
            if (alias.enabled && !alias.alias.isEmpty() && message.startsWith(alias.alias + " ")) {
                // Use substring instead of replaceFirst to avoid regex metacharacter interpretation
                String replaced = alias.replacement + message.substring(alias.alias.length());
                return replaced;
            } else if (alias.enabled && !alias.alias.isEmpty() && message.equals(alias.alias)) {
                // Exact match
                return alias.replacement;
            }
        }

        return message;
    }
}
