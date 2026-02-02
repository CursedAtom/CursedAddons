package dev.cursedatom.cursedaddons.features.images;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;

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
    
    private ImageHoverPreview() {
        // Utility class
    }
    
    /**
     * Checks if the image hover preview feature is enabled.
     */
    public static boolean isEnabled() {
        Object enabled = ConfigProvider.get("general.ImageHoverPreview.Enabled");
        return enabled != null && (boolean) enabled;
    }
    
    /**
     * Checks if an image URL matches the image pattern.
     */
    public static boolean isImageUrl(String url) {
        return url != null && IMAGE_PATTERN.matcher(url).find();
    }
    
    /**
     * Checks if an image URL is whitelisted.
     */
    public static boolean isWhitelisted(String imageUrl) {
        Object whitelistObj = ConfigProvider.get("general.ImageHoverPreview.Whitelist");
        if (whitelistObj == null) {
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) whitelistObj;
            List<SpecialUnits.WhitelistUnit> whitelist = SpecialUnits.WhitelistUnit.fromList(rawList);
            
            if (whitelist.isEmpty()) {
                return false;
            }

            java.net.URL url;
            try {
                url = new URI(imageUrl).toURL();
            } catch (URISyntaxException e) {
                return false;
            }

            String host = url.getHost();
            if (host == null) {
                return false;
            }
            host = host.toLowerCase();
            
            for (SpecialUnits.WhitelistUnit entry : whitelist) {
                if (!entry.enabled) {
                    continue;
                }
                
                String domain = entry.domain.toLowerCase();
                if (host.equals(domain) || host.endsWith("." + domain)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            LoggerUtils.error("[ImageHoverPreview] Error checking whitelist: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Clears the image cache.
     */
    public static void clearCache() {
        ImageCache.clear();
        ImageTextureManager.clear();
    }
    
    /**
     * Initializes the feature.
     */
    public static void init() {
        // Feature initialization if needed
    }
}
