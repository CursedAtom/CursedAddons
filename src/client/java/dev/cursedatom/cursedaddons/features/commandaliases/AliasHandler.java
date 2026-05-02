package dev.cursedatom.cursedaddons.features.commandaliases;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;

import java.util.List;

/**
 * Handles command alias expansion for both outgoing chat messages ({@link #processMessage})
 * and the command suggestions UI ({@link #findMatch}).
 *
 * <p>Aliases only match slash-prefixed input. Replacements do not need to start with {@code /};
 * a non-slash replacement causes the expanded text to be sent as a plain chat message.
 * Aliases with an empty {@code replacement} field are skipped to avoid sending blank input.
 */
public class AliasHandler {
    private AliasHandler() {}

    /** Returns the first enabled alias that matches {@code message}, or null. */
    public static SpecialUnits.AliasUnit findMatch(String message) {
        if (!message.startsWith("/") || !ConfigProvider.getBoolean(ConfigKeys.ALIASES_ENABLED, false)) {
            return null;
        }
        List<Object> aliasList = ConfigProvider.getList(ConfigKeys.ALIASES_LIST);
        if (aliasList.isEmpty()) return null;
        for (SpecialUnits.AliasUnit alias : SpecialUnits.AliasUnit.fromList(aliasList)) {
            if (alias.enabled && !alias.alias.isEmpty() && !alias.replacement.isEmpty()
                    && (message.startsWith(alias.alias + " ") || message.equals(alias.alias))) {
                return alias;
            }
        }
        return null;
    }

    public static String processMessage(String message) {
        SpecialUnits.AliasUnit match = findMatch(message);
        if (match == null) return message;
        return match.replacement + message.substring(match.alias.length());
    }
}
