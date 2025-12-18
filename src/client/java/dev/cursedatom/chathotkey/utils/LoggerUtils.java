package dev.cursedatom.chathotkey.utils;

import dev.cursedatom.chathotkey.ChatHotkey;

public class LoggerUtils {
    public static void init() {
        // No-op as ChatHotkey initializes it
    }

    public static void info(String s) {
        ChatHotkey.LOGGER.info(s);
    }

    public static void warn(String s) {
        ChatHotkey.LOGGER.warn(s);
    }

    public static void error(String s) {
        ChatHotkey.LOGGER.error(s);
    }
}
