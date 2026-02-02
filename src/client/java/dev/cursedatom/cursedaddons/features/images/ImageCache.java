package dev.cursedatom.cursedaddons.features.images;

import dev.cursedatom.cursedaddons.utils.LoggerUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final int MAX_CACHE_SIZE = 10;
    private static final Map<String, CacheEntry> cache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private static final Map<String, CompletableFuture<ImageResult>> loadingTasks = new ConcurrentHashMap<>();
    private static final Map<String, Long> failedUrls = new ConcurrentHashMap<>();
    private static final long RETRY_DELAY_MS = 30_000; // Wait 30 seconds before retrying a failed URL

    public static CompletableFuture<ImageResult> loadImage(String url, int maxWidth, int maxHeight) {
        String cacheKey = getCacheKey(url, maxWidth, maxHeight);

        // Check if this URL recently failed — don't retry until the cooldown expires
        Long failedAt = failedUrls.get(cacheKey);
        if (failedAt != null) {
            if (System.currentTimeMillis() - failedAt < RETRY_DELAY_MS) {
                return CompletableFuture.completedFuture(null);
            }
            failedUrls.remove(cacheKey);
        }

        // Check if already loading
        CompletableFuture<ImageResult> existing = loadingTasks.get(cacheKey);
        if (existing != null) {
            return existing;
        }

        // Check cache
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null) {
            return CompletableFuture.completedFuture(entry.result);
        }

        // Start loading
        CompletableFuture<ImageResult> future = new CompletableFuture<>();
        loadingTasks.put(cacheKey, future);

        CompletableFuture.runAsync(() -> {
            try {
                ImageResult result;
                if (isGifUrl(url)) {
                    result = loadGif(url, maxWidth, maxHeight);
                } else {
                    result = loadStatic(url, maxWidth, maxHeight);
                }
                cache.put(cacheKey, new CacheEntry(result));
                future.complete(result);
            } catch (Exception e) {
                LoggerUtils.error("[ImageHoverPreview] Failed to load image from " + url + ": " + e.getMessage());
                failedUrls.put(cacheKey, System.currentTimeMillis());
                future.completeExceptionally(e);
            } finally {
                loadingTasks.remove(cacheKey);
            }
        });

        return future;
    }

    private static String getCacheKey(String url, int maxWidth, int maxHeight) {
        return url + "@" + maxWidth + "x" + maxHeight;
    }

    private static ImageResult loadStatic(String url, int maxWidth, int maxHeight) throws Exception {
        BufferedImage image = ImageIO.read(new URI(url).toURL());
        if (image == null) {
            throw new Exception("Failed to read image");
        }
        
        // Scale image if it exceeds max dimensions
        BufferedImage processedImage = scaleIfNeeded(image, maxWidth, maxHeight);
        int w = processedImage.getWidth();
        int h = processedImage.getHeight();

        byte[] pngData = bufferedToPng(processedImage);

        // Clean up scaled image if it's different from original
        if (processedImage != image) {
            processedImage.flush();
        }
        image.flush();

        return ImageResult.ofStatic(pngData, w, h);
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
        try (InputStream stream = new URI(url).toURL().openStream()) {
            GifDecoder.GifData gifData = GifDecoder.decode(stream, maxWidth, maxHeight);

            List<BufferedImage> frames = gifData.getFrames();
            if (frames.isEmpty()) {
                throw new Exception("GIF has no frames");
            }

            // Single-frame GIF — treat as static
            if (frames.size() == 1) {
                BufferedImage frame = frames.get(0);
                byte[] pngData = bufferedToPng(frame);
                frame.flush();
                return ImageResult.ofStatic(pngData, gifData.getWidth(), gifData.getHeight());
            }

            // Encode all frames to PNG bytes on this async thread (heavy work)
            byte[][] framePngData = new byte[frames.size()][];
            for (int i = 0; i < frames.size(); i++) {
                framePngData[i] = bufferedToPng(frames.get(i));
                frames.get(i).flush();
            }

            return ImageResult.ofGif(framePngData, gifData.getDelays(),
                                     gifData.getWidth(), gifData.getHeight());
        }
    }

    /**
     * Encodes a BufferedImage to PNG byte array. CPU-intensive — call from async thread.
     */
    private static byte[] bufferedToPng(BufferedImage image) throws Exception {
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
        cache.clear();
        loadingTasks.clear();
        failedUrls.clear();
    }

    private static class CacheEntry {
        final ImageResult result;

        CacheEntry(ImageResult result) {
            this.result = result;
        }
    }
}
