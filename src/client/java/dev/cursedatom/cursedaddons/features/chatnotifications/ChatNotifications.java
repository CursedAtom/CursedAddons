package dev.cursedatom.cursedaddons.features.chatnotifications;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import dev.cursedatom.cursedaddons.utils.MessageUtils;
import dev.cursedatom.cursedaddons.utils.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatNotifications {

    private static final int MAX_PATTERN_LENGTH = 500;

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

    private static String replaceCaptureGroups(String template, Matcher matcher) {
        String result = template;

        // First, replace escaped dollars with placeholders to avoid conflicts
        result = result.replace("\\$", "{{ESCAPED_DOLLAR}}");

        // Then replace $0, $1, $2, etc. with capture groups
        for (int i = 0; i <= matcher.groupCount(); i++) {
            result = result.replace("$" + i, matcher.group(i));
        }

        // Finally, restore escaped dollars
        result = result.replace("{{ESCAPED_DOLLAR}}", "$");

        return result;
    }

    public static void checkMessage(Component message) {
        if (message == null) return;
        if (Minecraft.getInstance().player == null) return;

        Object enabledObj = ConfigProvider.get("chatnotifications.Notifications.Enabled");
        if (enabledObj == null || !(boolean) enabledObj) return;

        Object notificationListObj = ConfigProvider.get("chatnotifications.Notifications.List");
        if (notificationListObj == null) return;

        String plainText = message.getString();
        String legacyText = TextUtils.toLegacyString(message);

        @SuppressWarnings("unchecked")
        List<Object> notificationList = (List<Object>) notificationListObj;
        for (SpecialUnits.NotificationUnit notification : SpecialUnits.NotificationUnit.fromList(notificationList)) {
            if (!notification.enabled || notification.pattern.isEmpty()) continue;

            boolean matches = false;
            Matcher matcher = null;
            String textToMatch = notification.regex ? legacyText : plainText;

            if (notification.regex) {
                if (notification.pattern == null || notification.pattern.trim().isEmpty()) {
                    LoggerUtils.warn("[CursedAddons] Empty regex pattern in notification rule");
                    continue;
                }
                try { // this allows color codes to be used in the regex
                    String pattern = notification.pattern.replace("&r", "").replace("\\&", "&").replace("&", "§");

                    // Validate pattern safety to prevent ReDoS attacks
                    if (!isPatternSafe(pattern)) {
                        LoggerUtils.warn("[CursedAddons] Unsafe regex pattern detected (potential ReDoS): " + notification.pattern);
                        continue;
                    }

                    Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                    matcher = p.matcher(textToMatch);
                    matches = matcher.find();
                } catch (Exception e) {
                    LoggerUtils.warn("[CursedAddons] Invalid regex pattern: " + notification.pattern + " - " + e.getMessage());
                    continue;
                }
            } else {
                if (notification.pattern == null) {
                    LoggerUtils.warn("[CursedAddons] Null pattern in notification rule");
                    continue;
                }
                matches = textToMatch.contains(notification.pattern);
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
                        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLocation);
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0F));
                    } catch (Exception e) {
                        LoggerUtils.warn("[CursedAddons] Invalid sound: " + notification.sound);
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
