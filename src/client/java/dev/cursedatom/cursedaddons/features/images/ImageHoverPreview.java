package dev.cursedatom.cursedaddons.features.images;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import dev.cursedatom.cursedaddons.CursedAddons;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for image hover preview functionality.
 * Handles URL validation, whitelist checking, and feature enablement.
 */
public final class ImageHoverPreview {
    public static final Pattern IMAGE_PATTERN = Pattern.compile(
        "(https?://\\S*\\.(?:png|jpg|jpeg|gif|webp)(?:[?#]\\S*)?)",
        Pattern.CASE_INSENSITIVE
    );

    private static long cachedVersion = -1;
    private static List<SpecialUnits.WhitelistUnit> cachedWhitelist = List.of();

    private static void refreshWhitelistCache() {
        long currentVersion = ConfigProvider.getVersion();
        if (currentVersion != cachedVersion) {
            cachedVersion = currentVersion;
            List<Object> rawList = ConfigProvider.getList(ConfigKeys.IMAGE_WHITELIST);
            cachedWhitelist = rawList.isEmpty() ? List.of() : SpecialUnits.WhitelistUnit.fromList(rawList);
        }
    }

    private ImageHoverPreview() {
        // Utility class
    }

    /**
     * Checks if the image hover preview feature is enabled.
     */
    public static boolean isEnabled() {
        return ConfigProvider.getBoolean(ConfigKeys.IMAGE_PREVIEW_ENABLED, false);
    }

    /**
     * Checks if embed URL resolution is enabled for the given URL's domain.
     */
    public static boolean isEmbedResolutionEnabledForUrl(String url) {
        SpecialUnits.WhitelistUnit entry = getWhitelistEntry(url);
        return entry != null && entry.resolveEmbed;
    }

    /**
     * Checks if a URL is a direct image URL or a resolvable embed URL.
     */
    public static boolean isImageUrl(String url) {
        if (url == null) return false;
        if (IMAGE_PATTERN.matcher(url).find()) return true;
        return isEmbedResolutionEnabledForUrl(url) && UrlResolver.isResolvable(url);
    }

    /**
     * Returns true if the URL is not a direct image URL but can be resolved
     * via embed URL resolution (e.g. imgur.com/abc, tenor.com/view/...).
     */
    public static boolean needsResolution(String url) {
        if (url == null) return false;
        return !IMAGE_PATTERN.matcher(url).find() && UrlResolver.isResolvable(url);
    }

    /**
     * Checks if an image URL is whitelisted.
     */
    public static boolean isWhitelisted(String imageUrl) {
        return getWhitelistEntry(imageUrl) != null;
    }

    /**
     * Returns the matching whitelist entry for the given URL, or null if not whitelisted.
     */
    public static SpecialUnits.WhitelistUnit getWhitelistEntry(String imageUrl) {
        refreshWhitelistCache();
        if (cachedWhitelist.isEmpty()) {
            return null;
        }

        try {
            java.net.URL url;
            try {
                url = new URI(imageUrl).toURL();
            } catch (URISyntaxException e) {
                return null;
            }

            String host = url.getHost();
            if (host == null) {
                return null;
            }
            host = host.toLowerCase();

            for (SpecialUnits.WhitelistUnit entry : cachedWhitelist) {
                if (!entry.enabled) {
                    continue;
                }

                String domain = entry.domain.toLowerCase();
                if (host.equals(domain) || host.endsWith("." + domain)) {
                    return entry;
                }
            }

            return null;
        } catch (Exception e) {
            CursedAddons.LOGGER.error("[ImageHoverPreview] Error checking whitelist: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clears the image cache and URL resolver cache.
     */
    public static void clearCache() {
        ImageCache.clear();
        ImageTextureManager.clear();
        UrlResolver.clearCache();
    }

    /**
     * Initializes the feature.
     */
    public static void init() {
        refreshWhitelistCache();
    }
}
