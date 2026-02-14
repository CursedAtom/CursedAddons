package dev.cursedatom.cursedaddons.config;

/**
 * Central registry of all config key strings used to access values in {@link dev.cursedatom.cursedaddons.utils.ConfigStorage}.
 */
public final class ConfigKeys {
    public static final String MACRO_ENABLED = "chatkeybindings.Macro.Enabled";
    public static final String MACRO_LIST = "chatkeybindings.Macro.List";
    public static final String ALIASES_ENABLED = "commandaliases.Aliases.Enabled";
    public static final String ALIASES_LIST = "commandaliases.Aliases.List";
    public static final String NOTIFICATIONS_ENABLED = "chatnotifications.Notifications.Enabled";
    public static final String NOTIFICATIONS_LIST = "chatnotifications.Notifications.List";
    public static final String CLICK_EVENTS_ENABLED = "general.PreviewClickEvents.Enabled";
    public static final String IMAGE_PREVIEW_ENABLED = "imagepreview.Preview.Enabled";
    public static final String IMAGE_DETECT_PLAIN_URLS = "imagepreview.Preview.DetectPlainTextUrls";
    public static final String IMAGE_MAX_FILE_SIZE_MB = "imagepreview.Preview.MaxFileSizeMB";
    public static final String IMAGE_WHITELIST = "imagepreview.Preview.Whitelist";
    public static final String CONFIG_VERSION = "config.version";

    private ConfigKeys() {}
}
