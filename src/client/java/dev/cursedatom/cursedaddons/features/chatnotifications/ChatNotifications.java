package dev.cursedatom.cursedaddons.features.chatnotifications;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.utils.ConfigUtils;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import dev.cursedatom.cursedaddons.utils.MessageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatNotifications {

    private static String replaceCaptureGroups(String template, Matcher matcher) {
        String result = template;
        // Replace $0, $1, $2, etc. with capture groups
        for (int i = 0; i <= matcher.groupCount(); i++) {
            result = result.replace("$" + i, matcher.group(i));
        }
        return result;
    }

    public static void checkMessage(Component message) {
        if (Minecraft.getInstance().player == null) return;

        Object enabledObj = ConfigUtils.get("chatnotifications.Notifications.Enabled");
        if (enabledObj == null || !(boolean) enabledObj) return;

        Object notificationListObj = ConfigUtils.get("chatnotifications.Notifications.List");
        if (notificationListObj == null) return;

        String messageText = message.getString();

        @SuppressWarnings("unchecked")
        List<Object> notificationList = (List<Object>) notificationListObj;
        for (SpecialUnits.NotificationUnit notification : SpecialUnits.NotificationUnit.fromList(notificationList)) {
            if (!notification.enabled || notification.pattern.isEmpty()) continue;

            boolean matches = false;
            Matcher matcher = null;

            if (notification.regex) {
                try {
                    Pattern pattern = Pattern.compile(notification.pattern);
                    matcher = pattern.matcher(messageText);
                    matches = matcher.find();
                } catch (Exception e) {
                    LoggerUtils.warn("[CursedAddons] Invalid regex pattern: " + notification.pattern);
                    continue;
                }
            } else {
                matches = messageText.contains(notification.pattern);
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
                    Minecraft.getInstance().gui.setTitle(Component.literal(titleToSet));
                }

                // Send command
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
