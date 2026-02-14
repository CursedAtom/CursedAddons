package dev.cursedatom.cursedaddons.features.chatnotifications;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import dev.cursedatom.cursedaddons.CursedAddons;
import dev.cursedatom.cursedaddons.utils.MessageUtils;
import dev.cursedatom.cursedaddons.utils.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks incoming chat messages against configured notification patterns and triggers
 * the associated actions (sound, title display, command) when a pattern matches.
 * Config is lazily refreshed via {@link dev.cursedatom.cursedaddons.utils.ConfigProvider#getVersion()}.
 */
public class ChatNotifications {
    private ChatNotifications() {}

    private static final int MAX_PATTERN_LENGTH = 500;

    private record CachedNotification(SpecialUnits.NotificationUnit unit, Pattern compiledPattern) {}

    private static long cachedVersion = -1;
    private static List<CachedNotification> cachedNotifications = List.of();

    /**
     * Validates regex pattern to prevent ReDoS attacks.
     * Rejects patterns with nested quantifiers and excessive length.
     */
    private static boolean isPatternSafe(String pattern) {
        if (pattern == null || pattern.length() > MAX_PATTERN_LENGTH) {
            return false;
        }

        // Check for nested quantifiers that can cause catastrophic backtracking
        // Patterns like (a+)+, (a*)*, (a+)*, (a{1,5})+, etc.
        if (pattern.matches(".*\\([^)]*[+*{][^)]*\\)[+*{].*")) {
            return false;
        }

        // Check for alternation with repetition: (a|b)+
        if (pattern.matches(".*\\([^)]*\\|[^)]*\\)[+*{].*")) {
            return false;
        }

        return true;
    }

    /**
     * Substitutes regex capture group references in {@code template} with the corresponding
     * groups from {@code matcher}. Supports {@code $1}, {@code $2}, ... group references,
     * and {@code \$} as an escape to produce a literal {@code $}.
     */
    private static String replaceCaptureGroups(String template, Matcher matcher) {
        StringBuilder sb = new StringBuilder(template.length());
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '\\' && i + 1 < template.length() && template.charAt(i + 1) == '$') {
                sb.append('$');
                i += 2;
            } else if (c == '$' && i + 1 < template.length() && Character.isDigit(template.charAt(i + 1))) {
                int start = i + 1;
                int end = start;
                while (end < template.length() && Character.isDigit(template.charAt(end))) {
                    end++;
                }
                int groupIndex = Integer.parseInt(template.substring(start, end));
                if (groupIndex <= matcher.groupCount()) {
                    String group = matcher.group(groupIndex);
                    if (group != null) {
                        sb.append(group);
                    }
                } else {
                    sb.append(template, i, end);
                }
                i = end;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static void refreshCache() {
        long currentVersion = ConfigProvider.getVersion();
        if (currentVersion != cachedVersion) {
            cachedVersion = currentVersion;
            List<Object> rawList = ConfigProvider.getList(ConfigKeys.NOTIFICATIONS_LIST);
            if (rawList.isEmpty()) {
                cachedNotifications = List.of();
                return;
            }
            List<CachedNotification> built = new ArrayList<>();
            for (SpecialUnits.NotificationUnit unit : SpecialUnits.NotificationUnit.fromList(rawList)) {
                if (!unit.enabled || unit.pattern == null || unit.pattern.isEmpty()) continue;
                Pattern compiled = null;
                if (unit.regex) {
                    String pat = unit.pattern.replace("&r", "").replace("\\&", "&").replace("&", "§");
                    if (!isPatternSafe(pat)) {
                        CursedAddons.LOGGER.warn("[CursedAddons] Unsafe regex pattern detected (potential ReDoS): " + unit.pattern);
                        continue;
                    }
                    try {
                        compiled = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
                    } catch (Exception e) {
                        CursedAddons.LOGGER.warn("[CursedAddons] Invalid regex pattern: " + unit.pattern + " - " + e.getMessage());
                        continue;
                    }
                }
                built.add(new CachedNotification(unit, compiled));
            }
            cachedNotifications = List.copyOf(built);
        }
    }

    public static void checkMessage(Component message) {
        if (message == null) return;
        if (Minecraft.getInstance().player == null) return;

        if (!ConfigProvider.getBoolean(ConfigKeys.NOTIFICATIONS_ENABLED, false)) return;

        refreshCache();
        if (cachedNotifications.isEmpty()) return;

        String plainText = message.getString();
        String legacyText = null; // lazy init — only computed if a regex notification exists

        for (CachedNotification cached : cachedNotifications) {
            SpecialUnits.NotificationUnit notification = cached.unit();

            boolean matches = false;
            Matcher matcher = null;

            if (notification.regex) {
                if (legacyText == null) legacyText = TextUtils.toLegacyString(message);
                matcher = cached.compiledPattern().matcher(legacyText);
                matches = matcher.find();
            } else {
                matches = plainText.contains(notification.pattern);
            }

            if (matches) {
                // Play sound
                if (notification.soundEnabled && !notification.sound.isEmpty()) {
                    try {
                        String soundToPlay = notification.sound;
                        if (matcher != null && notification.regex) {
                            soundToPlay = replaceCaptureGroups(soundToPlay, matcher);
                        }
                        ResourceLocation soundLocation = ResourceLocation.parse(soundToPlay);
                        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.get(soundLocation).orElseThrow();
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(holder, 1.0F));
                    } catch (Exception e) {
                        CursedAddons.LOGGER.warn("[CursedAddons] Invalid sound: " + notification.sound);
                    }
                }

                // Set title
                if (notification.titleEnabled && !notification.title.isEmpty()) {
                    String titleToSet = notification.title;
                    if (matcher != null && notification.regex) {
                        titleToSet = replaceCaptureGroups(titleToSet, matcher);
                    }
                    titleToSet = titleToSet.replace("\\&", "&").replace("&", "§");
                    Minecraft.getInstance().gui.setTitle(Component.literal(titleToSet));
                }

                // Send command/message
                if (notification.commandEnabled && !notification.command.isEmpty()) {
                    String commandToSend = notification.command;
                    if (matcher != null && notification.regex) {
                        commandToSend = replaceCaptureGroups(commandToSend, matcher);
                    }
                    MessageUtils.sendToPublicChat(commandToSend);
                }
            }
        }
    }
}
