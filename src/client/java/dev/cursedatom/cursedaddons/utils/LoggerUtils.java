package dev.cursedatom.cursedaddons.utils;

import dev.cursedatom.cursedaddons.CursedAddons;

public class LoggerUtils {
    public static void init() {
        // Initialized in the main mod class
    }

    public static void info(String s) {
        CursedAddons.LOGGER.info(s);
    }

    public static void warn(String s) {
        CursedAddons.LOGGER.warn(s);
    }

    public static void error(String s) {
        CursedAddons.LOGGER.error(s);
    }
}
