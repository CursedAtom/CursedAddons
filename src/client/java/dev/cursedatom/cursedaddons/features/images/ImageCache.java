package dev.cursedatom.cursedaddons.features.images;

import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import dev.cursedatom.cursedaddons.CursedAddons;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronously fetches and caches images (static and GIF) from whitelisted URLs.
 * Applies SSRF protection, file-size limiting, and LRU eviction up to {@code MAX_CACHE_SIZE} entries.
 */
public class ImageCache {
    private ImageCache() {}
    private static final int MAX_CACHE_SIZE = 10;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_MAX_FILE_SIZE_MB = 30;

    private static int getMaxContentLength() {
        Object value = ConfigProvider.get(ConfigKeys.IMAGE_MAX_FILE_SIZE_MB);
        int mb = (value instanceof Number) ? ((Number) value).intValue() : DEFAULT_MAX_FILE_SIZE_MB;
        return Math.max(mb, 1) * 1024 * 1024;
    }

    private static final Object cacheLock = new Object();
    private static final Map<String, CacheEntry> cache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            if (size() > MAX_CACHE_SIZE) {
                String evictedKey = eldest.getKey();
                ImageTextureManager.scheduleTextureRemoval(evictedKey);
                return true;
            }
            return false;
        }
    };

    private static final Map<String, CompletableFuture<ImageResult>> loadingTasks = new ConcurrentHashMap<>();
    private static final Map<String, FailureEntry> failedUrls = new ConcurrentHashMap<>();
    private static final long RETRY_DELAY_MS = 30_000;

    public static CompletableFuture<ImageResult> loadImage(String url, int maxWidth, int maxHeight) {
        String cacheKey = getCacheKey(url, maxWidth, maxHeight);

        FailureEntry failure = failedUrls.get(cacheKey);
        if (failure != null) {
            if (System.currentTimeMillis() - failure.timestamp < RETRY_DELAY_MS) {
                return CompletableFuture.completedFuture(null);
            }
            failedUrls.remove(cacheKey);
        }

        synchronized (cacheLock) {
            CacheEntry entry = cache.get(cacheKey);
            if (entry != null) {
                return CompletableFuture.completedFuture(entry.result);
            }
        }

        return loadingTasks.computeIfAbsent(cacheKey, key -> {
            CompletableFuture<ImageResult> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> {
                try {
                    // Resolve embed URLs (e.g. imgur.com/abc -> i.imgur.com/abc.jpg)
                    String loadUrl = url;
                    if (ImageHoverPreview.isEmbedResolutionEnabledForUrl(url) && ImageHoverPreview.needsResolution(url)) {
                        String resolved = UrlResolver.resolve(url);
                        if (resolved == null) {
                            throw new Exception("Failed to resolve embed URL");
                        }
                        if (!ImageHoverPreview.isWhitelisted(resolved)) {
                            throw new Exception("Domain not whitelisted");
                        }
                        loadUrl = resolved;
                    }

                    ImageResult result;
                    if (isGifUrl(loadUrl)) {
                        result = loadGif(loadUrl, maxWidth, maxHeight);
                    } else {
                        result = loadStatic(loadUrl, maxWidth, maxHeight);
                    }
                    synchronized (cacheLock) {
                        cache.put(cacheKey, new CacheEntry(result));
                    }
                    future.complete(result);
                } catch (Exception e) {
                    CursedAddons.LOGGER.error("[ImageHoverPreview] Failed to load image from " + url + ": " + e.getMessage());
                    failedUrls.put(cacheKey, new FailureEntry(System.currentTimeMillis(), e.getMessage()));
                    future.completeExceptionally(e);
                } finally {
                    loadingTasks.remove(cacheKey);
                }
            });
            return future;
        });
    }

    /**
     * Returns the failure reason for a cached failed URL, or null if not failed.
     */
    public static String getFailureReason(String url, int maxWidth, int maxHeight) {
        String cacheKey = getCacheKey(url, maxWidth, maxHeight);
        FailureEntry failure = failedUrls.get(cacheKey);
        if (failure != null && System.currentTimeMillis() - failure.timestamp < RETRY_DELAY_MS) {
            return failure.reason;
        }
        return null;
    }

    // Cache keys follow the pattern "url@WxH" to separate entries for different display sizes.
    static String getCacheKey(String url, int maxWidth, int maxHeight) {
        return url + "@" + maxWidth + "x" + maxHeight;
    }

    private static InputStream openConnectionWithTimeouts(String url) throws Exception {
        URI uri = new URI(url);
        // SSRF check BEFORE connecting
        InetAddress address = InetAddress.getByName(uri.toURL().getHost());
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
            throw new Exception("Blocked address");
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "image/*");
            connection.connect();

            int responseCode = connection.getResponseCode();

            // Reject redirects — the whitelist validated the original host, not the redirect target
            if (responseCode >= 300 && responseCode < 400) {
                throw new Exception("HTTP " + responseCode);
            }

            if (responseCode != 200) {
                throw new Exception("HTTP " + responseCode);
            }

            int contentLength = connection.getContentLength();
            if (contentLength > getMaxContentLength()) {
                throw new Exception("File too large (" + (contentLength / 1024 / 1024) + "MB / " + (getMaxContentLength() / 1024 / 1024) + "MB max)");
            }

            return new BoundedInputStream(connection.getInputStream(), getMaxContentLength());
        } catch (Exception e) {
            if (connection != null) connection.disconnect();
            throw e;
        }
    }

    private static ImageResult loadStatic(String url, int maxWidth, int maxHeight) throws Exception {
        BufferedImage image;
        try (InputStream stream = openConnectionWithTimeouts(url)) {
            image = ImageIO.read(stream);
        }
        if (image == null) {
            throw new Exception("Failed to read image");
        }

        BufferedImage processedImage = scaleIfNeeded(image, maxWidth, maxHeight);
        int width = processedImage.getWidth();
        int height = processedImage.getHeight();

        int[] argbPixels = processedImage.getRGB(0, 0, width, height, null, 0, width);

        if (processedImage != image) {
            processedImage.flush();
        }
        image.flush();

        return ImageResult.ofStaticRaw(argbPixels, width, height);
    }

    private static BufferedImage scaleIfNeeded(BufferedImage image, int maxWidth, int maxHeight) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (maxWidth <= 0 || maxHeight <= 0) {
            return image;
        }

        float scaleX = (float) maxWidth / width;
        float scaleY = (float) maxHeight / height;
        float scale = Math.min(Math.min(scaleX, scaleY), 1.0f);

        if (scale >= 1.0f) {
            return image;
        }

        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();

        return scaled;
    }

    private static ImageResult loadGif(String url, int maxWidth, int maxHeight) throws Exception {
        try (InputStream stream = openConnectionWithTimeouts(url)) {
            GifDecoder.GifData gifData = GifDecoder.decode(stream, maxWidth, maxHeight);

            List<byte[]> encodedFrames = gifData.getEncodedFrames();
            if (encodedFrames.isEmpty()) {
                throw new Exception("GIF has no frames");
            }

            if (encodedFrames.size() == 1) {
                return ImageResult.ofStatic(encodedFrames.get(0), gifData.getWidth(), gifData.getHeight());
            }

            byte[][] framePngData = encodedFrames.toArray(new byte[0][]);

            return ImageResult.ofGif(framePngData, gifData.getDelays(),
                                     gifData.getWidth(), gifData.getHeight());
        }
    }

    /**
     * Encodes a BufferedImage to PNG byte array. CPU-intensive — call from async thread.
     */
    static byte[] bufferedToPng(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static boolean isGifUrl(String url) {
        String lower = url.toLowerCase();
        int queryIndex = lower.indexOf('?');
        String path = queryIndex >= 0 ? lower.substring(0, queryIndex) : lower;
        return path.endsWith(".gif");
    }

    public static void clear() {
        // Cancel all in-flight loading tasks
        List<CompletableFuture<ImageResult>> futures = new ArrayList<>(loadingTasks.values());
        loadingTasks.clear();
        for (CompletableFuture<ImageResult> future : futures) {
            future.cancel(true);
        }

        synchronized (cacheLock) {
            cache.clear();
        }
        failedUrls.clear();
    }

    private static class CacheEntry {
        final ImageResult result;

        CacheEntry(ImageResult result) {
            this.result = result;
        }
    }

    private static class FailureEntry {
        final long timestamp;
        final String reason;

        FailureEntry(long timestamp, String reason) {
            this.timestamp = timestamp;
            this.reason = reason;
        }
    }

    /**
     * InputStream wrapper that enforces a maximum number of bytes read.
     */
    private static class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private final long maxBytes;
        private long bytesRead;

        BoundedInputStream(InputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws java.io.IOException {
            if (bytesRead >= maxBytes) {
                throw new java.io.IOException("File too large (exceeded " + (maxBytes / 1024 / 1024) + "MB limit)");
            }
            int b = delegate.read();
            if (b >= 0) bytesRead++;
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws java.io.IOException {
            if (bytesRead >= maxBytes) {
                throw new java.io.IOException("File too large (exceeded " + (maxBytes / 1024 / 1024) + "MB limit)");
            }
            long remaining = maxBytes - bytesRead;
            int toRead = (int) Math.min(len, remaining);
            int n = delegate.read(buf, off, toRead);
            if (n > 0) bytesRead += n;
            return n;
        }

        @Override
        public void close() throws java.io.IOException {
            delegate.close();
        }
    }
}
