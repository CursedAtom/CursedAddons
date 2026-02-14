package dev.cursedatom.cursedaddons.features.images;

import dev.cursedatom.cursedaddons.CursedAddons;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves platform embed URLs (e.g. imgur.com/abc, tenor.com/view/...)
 * to direct image URLs that can be loaded by ImageCache.
 */
public final class UrlResolver {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;
    private static final int MAX_HTML_BYTES = 32 * 1024; // 32 KB
    private static final long FAILURE_COOLDOWN_MS = 60_000;

    private static final ConcurrentHashMap<String, String> resolvedCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> failedCache = new ConcurrentHashMap<>();

    private static final Pattern OG_IMAGE_PATTERN = Pattern.compile(
        "<meta[^>]+property\\s*=\\s*[\"']og:image[\"'][^>]+content\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern OG_IMAGE_PATTERN_ALT = Pattern.compile(
        "<meta[^>]+content\\s*=\\s*[\"']([^\"']+)[\"'][^>]+property\\s*=\\s*[\"']og:image[\"']",
        Pattern.CASE_INSENSITIVE
    );

    private static final List<PlatformResolver> RESOLVERS = List.of(
        new ImgurResolver(),
        new GiphyResolver(),
        new TenorResolver(),
        new GenericOgImageResolver()
    );

    private UrlResolver() {}

    /**
     * Returns true if any resolver can handle this URL.
     */
    public static boolean isResolvable(String url) {
        if (url == null) return false;
        for (PlatformResolver resolver : RESOLVERS) {
            if (resolver.matches(url)) return true;
        }
        return false;
    }

    /**
     * Resolves a platform URL to a direct image URL.
     * May make HTTP requests — call from async thread only.
     *
     * @return direct image URL, or null if resolution failed
     */
    public static String resolve(String url) {
        if (url == null) return null;

        String cached = resolvedCache.get(url);
        if (cached != null) return cached;

        Long failedAt = failedCache.get(url);
        if (failedAt != null && System.currentTimeMillis() - failedAt < FAILURE_COOLDOWN_MS) {
            return null;
        }

        for (PlatformResolver resolver : RESOLVERS) {
            if (resolver.matches(url)) {
                try {
                    String resolved = resolver.resolve(url);
                    if (resolved != null) {
                        resolvedCache.put(url, resolved);
                        failedCache.remove(url);
                        return resolved;
                    }
                } catch (Exception e) {
                    CursedAddons.LOGGER.error("[UrlResolver] Failed to resolve " + url + ": " + e.getMessage());
                }
                break; // Only try the first matching resolver (GenericOg is last and matches anything)
            }
        }

        failedCache.put(url, System.currentTimeMillis());
        return null;
    }

    public static void clearCache() {
        resolvedCache.clear();
        failedCache.clear();
    }

    private static String fetchOgImage(String url) throws Exception {
        URI uri = new URI(url);
        InetAddress address = InetAddress.getByName(uri.toURL().getHost());
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
            throw new Exception("Connection to private/loopback addresses is not allowed");
        }

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "text/html");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; ImagePreview/1.0)");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("HTTP " + responseCode);
            }

            // Post-redirect SSRF check
            String finalHost = connection.getURL().getHost();
            if (!finalHost.equals(uri.toURL().getHost())) {
                InetAddress finalAddress = InetAddress.getByName(finalHost);
                if (finalAddress.isLoopbackAddress() || finalAddress.isSiteLocalAddress() || finalAddress.isLinkLocalAddress()) {
                    throw new Exception("Redirect to private/loopback address blocked");
                }
            }

            try (InputStream stream = connection.getInputStream()) {
                byte[] buffer = new byte[MAX_HTML_BYTES];
                int totalRead = 0;
                int bytesRead;
                while (totalRead < MAX_HTML_BYTES && (bytesRead = stream.read(buffer, totalRead, MAX_HTML_BYTES - totalRead)) != -1) {
                    totalRead += bytesRead;
                }

                String html = new String(buffer, 0, totalRead, StandardCharsets.UTF_8);

                Matcher matcher = OG_IMAGE_PATTERN.matcher(html);
                if (matcher.find()) {
                    return matcher.group(1);
                }

                matcher = OG_IMAGE_PATTERN_ALT.matcher(html);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }

            return null;
        } finally {
            connection.disconnect();
        }
    }

    private interface PlatformResolver {
        boolean matches(String url);
        String resolve(String url) throws Exception;
    }

    /**
     * Imgur: rewrites imgur.com/{id} to i.imgur.com/{id}.jpg
     * No HTTP request needed.
     */
    private static class ImgurResolver implements PlatformResolver {
        private static final Pattern IMGUR_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?imgur\\.com/([a-zA-Z0-9]+)$"
        );

        @Override
        public boolean matches(String url) {
            return IMGUR_PATTERN.matcher(url).find();
        }

        @Override
        public String resolve(String url) {
            Matcher matcher = IMGUR_PATTERN.matcher(url);
            if (matcher.find()) {
                String id = matcher.group(1);
                // "a" and "gallery" are album path segments, not image IDs — skip them
                if ("a".equals(id) || "gallery".equals(id)) return null;
                return "https://i.imgur.com/" + id + ".jpg";
            }
            return null;
        }
    }

    /**
     * Giphy: rewrites giphy.com/gifs/{optional-slug-}{id} to media.giphy.com/media/{id}/giphy.gif
     * No HTTP request needed.
     */
    private static class GiphyResolver implements PlatformResolver {
        private static final Pattern GIPHY_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?giphy\\.com/gifs/(?:.*-)?([a-zA-Z0-9]+)$"
        );

        @Override
        public boolean matches(String url) {
            return GIPHY_PATTERN.matcher(url).find();
        }

        @Override
        public String resolve(String url) {
            Matcher matcher = GIPHY_PATTERN.matcher(url);
            if (matcher.find()) {
                String id = matcher.group(1);
                return "https://media.giphy.com/media/" + id + "/giphy.gif";
            }
            return null;
        }
    }

    /**
     * Tenor: fetches page HTML and extracts og:image.
     */
    private static class TenorResolver implements PlatformResolver {
        private static final Pattern TENOR_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?tenor\\.com/view/.+"
        );

        @Override
        public boolean matches(String url) {
            return TENOR_PATTERN.matcher(url).find();
        }

        @Override
        public String resolve(String url) throws Exception {
            return fetchOgImage(url);
        }
    }

    /**
     * Generic fallback: for any URL without an image extension, tries og:image scraping.
     * Only tried after all specific resolvers fail to match.
     */
    private static class GenericOgImageResolver implements PlatformResolver {
        @Override
        public boolean matches(String url) {
            // Matches any URL that isn't already a direct image URL
            return url != null && url.startsWith("http");
        }

        @Override
        public String resolve(String url) throws Exception {
            return fetchOgImage(url);
        }
    }
}
