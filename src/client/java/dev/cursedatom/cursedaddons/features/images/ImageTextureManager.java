package dev.cursedatom.cursedaddons.features.images;

import com.mojang.blaze3d.platform.NativeImage;
import dev.cursedatom.cursedaddons.CursedAddons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Manages GPU texture lifecycle for image tooltips.
 * Handles static and animated (GIF) textures.
 */
public final class ImageTextureManager {
    // Use insertion-ordered maps so eviction removes the oldest entries (LRU-like)
    private static final Map<String, TextureEntry> staticCache = new LinkedHashMap<>();
    private static final Map<String, AnimatedTextureEntry> animatedCache = new LinkedHashMap<>();
    private static final Object textureCacheLock = new Object();
    private static final int MAX_TOTAL_TEXTURES = 50;
    private static final AtomicLong textureIdCounter = new AtomicLong(0);

    private ImageTextureManager() {}

    public static String getCacheKey(String url, int maxWidth, int maxHeight) {
        return url + "@" + maxWidth + "x" + maxHeight;
    }

    private static final int GIF_BATCH_SIZE = 8;

    /**
     * Prepares NativeImages from ImageResult on the calling thread (should be async),
     * then schedules fast texture registration on the render thread.
     * For animated GIFs, frames are decoded and registered in small batches so the
     * first frame renders immediately while remaining frames load asynchronously.
     */
    public static void prepareAndRegister(String cacheKey, ImageResult result) {
        if (hasTextures(cacheKey)) return;

        try {
            if (result.isAnimated()) {
                prepareAndRegisterAnimated(cacheKey, result);
            } else if (result.hasRawPixels()) {
                int width = result.getWidth();
                int height = result.getHeight();
                int[] argbPixels = result.getRawArgbPixels();

                NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = argbPixels[y * width + x];
                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        nativeImage.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                    }
                }

                Minecraft.getInstance().execute(() -> {
                    if (hasTextures(cacheKey)) {
                        nativeImage.close();
                        return;
                    }
                    registerStaticTexture(cacheKey, nativeImage, width, height);
                    enforceCacheLimit();
                });
            } else {
                byte[] pngData = result.getStaticPngData();
                if (pngData == null) return;

                NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(pngData));
                int width = result.getWidth();
                int height = result.getHeight();
                Minecraft.getInstance().execute(() -> {
                    if (hasTextures(cacheKey)) {
                        nativeImage.close();
                        return;
                    }
                    registerStaticTexture(cacheKey, nativeImage, width, height);
                    enforceCacheLimit();
                });
            }
        } catch (Exception e) {
            CursedAddons.LOGGER.error("[ImageTextureManager] Failed to prepare textures for " + cacheKey + ": " + e.getMessage());
        }
    }

    /**
     * Decodes and registers animated GIF frames in batches of {@link #GIF_BATCH_SIZE}.
     * The first batch is decoded immediately on the calling async thread, so the
     * first frame can render right away. Remaining batches are decoded on separate
     * async threads and registered on the render thread as they complete.
     */
    private static void prepareAndRegisterAnimated(String cacheKey, ImageResult result) {
        byte[][] framePngData = result.getGifFramePngData();
        int[] delays = result.getGifDelays();
        if (framePngData == null || delays == null) return;

        final int MAX_GIF_FRAMES = MAX_TOTAL_TEXTURES / 2;
        int totalFrames = framePngData.length;
        if (totalFrames > MAX_GIF_FRAMES) {
            byte[][] truncated = new byte[MAX_GIF_FRAMES][];
            System.arraycopy(framePngData, 0, truncated, 0, MAX_GIF_FRAMES);
            framePngData = truncated;
            int[] truncatedDelays = new int[MAX_GIF_FRAMES];
            System.arraycopy(delays, 0, truncatedDelays, 0, MAX_GIF_FRAMES);
            delays = truncatedDelays;
            totalFrames = MAX_GIF_FRAMES;
        }
        final byte[][] frames = framePngData;
        final int[] frameDelays = delays;
        final int frameCount = totalFrames;
        int width = result.getWidth();
        int height = result.getHeight();

        ResourceLocation[] frameLocations = new ResourceLocation[frameCount];
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AnimatedTextureEntry entry = new AnimatedTextureEntry(frameLocations, frameDelays, width, height);

        synchronized (textureCacheLock) {
            animatedCache.put(cacheKey, entry);
        }

        int firstBatchEnd = Math.min(GIF_BATCH_SIZE, frameCount);
        try {
            registerFrameBatch(cacheKey, frames, frameLocations, 0, firstBatchEnd, cancelled);
        } catch (Exception e) {
            CursedAddons.LOGGER.error("[ImageTextureManager] Failed to prepare initial GIF batch for " + cacheKey + ": " + e.getMessage());
            synchronized (textureCacheLock) {
                animatedCache.remove(cacheKey);
            }
            return;
        }

        if (firstBatchEnd < frameCount) {
            CompletableFuture.runAsync(() -> {
                for (int batchStart = firstBatchEnd; batchStart < frameCount; batchStart += GIF_BATCH_SIZE) {
                    boolean exists;
                    synchronized (textureCacheLock) {
                        exists = animatedCache.containsKey(cacheKey);
                    }
                    if (cancelled.get() || !exists) return;
                    int batchEnd = Math.min(batchStart + GIF_BATCH_SIZE, frameCount);
                    try {
                        registerFrameBatch(cacheKey, frames, frameLocations, batchStart, batchEnd, cancelled);
                    } catch (Exception e) {
                        CursedAddons.LOGGER.error("[ImageTextureManager] Failed to prepare GIF batch [" + batchStart + "-" + batchEnd + "] for " + cacheKey + ": " + e.getMessage());
                        return;
                    }
                }
            });
        }

        Minecraft.getInstance().execute(ImageTextureManager::enforceCacheLimit);
    }

    /**
     * Decodes a batch of frames to NativeImages (CPU-heavy, runs on async thread),
     * then schedules their texture registration on the render thread.
     */
    private static void registerFrameBatch(String cacheKey, byte[][] framePngData,
                                           ResourceLocation[] frameLocations,
                                           int start, int end, AtomicBoolean cancelled) throws Exception {
        NativeImage[] batch = new NativeImage[end - start];
        try {
            for (int i = 0; i < batch.length; i++) {
                if (cancelled.get()) {
                    closeBatch(batch);
                    return;
                }
                batch[i] = NativeImage.read(new ByteArrayInputStream(framePngData[start + i]));
            }
        } catch (Exception e) {
            closeBatch(batch);
            throw e;
        }

        final int batchStart = start;
        Minecraft.getInstance().execute(() -> {
            boolean exists;
            synchronized (textureCacheLock) {
                exists = animatedCache.containsKey(cacheKey);
            }
            if (cancelled.get() || !exists) {
                closeBatch(batch);
                return;
            }
            for (int i = 0; i < batch.length; i++) {
                int frameIndex = batchStart + i;
                try {
                    long id = textureIdCounter.getAndIncrement();
                    Supplier<String> nameSupplier = () -> "cursedaddons_gif_" + id;
                    DynamicTexture dynamicTexture = new DynamicTexture(nameSupplier, batch[i]);

                    String path = "gif_frames/" + id;
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath("cursedaddons", path);

                    Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
                    frameLocations[frameIndex] = location;
                    batch[i] = null; // Ownership transferred
                } catch (Exception e) {
                    CursedAddons.LOGGER.error("[ImageTextureManager] Failed to register GIF frame " + frameIndex + " for " + cacheKey + ": " + e.getMessage());
                }
            }
            closeBatch(batch);
        });
    }

    private static void closeBatch(NativeImage[] batch) {
        for (NativeImage img : batch) {
            if (img != null) img.close();
        }
    }

    /**
     * Gets the current image data for rendering. For animated images,
     * returns the current frame based on elapsed time.
     */
    public static ImageHoverEvent.ImageData getImageData(String cacheKey, String imageUrl) {
        synchronized (textureCacheLock) {
            TextureEntry entry = staticCache.get(cacheKey);
            if (entry != null) {
                return new ImageHoverEvent.ImageData(imageUrl, entry.textureLocation, entry.width, entry.height);
            }

            AnimatedTextureEntry animated = animatedCache.get(cacheKey);
            if (animated != null) {
                ResourceLocation currentFrame = animated.getCurrentFrameLocation();
                if (currentFrame != null) {
                    return new ImageHoverEvent.ImageData(imageUrl, currentFrame, animated.width, animated.height);
                }
            }
        }

        return null;
    }

    /**
     * Checks if textures have been created for this cache key.
     */
    public static boolean hasTextures(String cacheKey) {
        synchronized (textureCacheLock) {
            return staticCache.containsKey(cacheKey) || animatedCache.containsKey(cacheKey);
        }
    }

    /**
     * Schedules texture removal on the render thread when a cache entry is evicted.
     * Called from ImageCache's LRU eviction callback.
     */
    public static void scheduleTextureRemoval(String cacheKey) {
        Minecraft.getInstance().execute(() -> removeTexture(cacheKey));
    }

    public static void clear() {
        var textureManager = Minecraft.getInstance().getTextureManager();

        synchronized (textureCacheLock) {
            for (TextureEntry entry : staticCache.values()) {
                textureManager.release(entry.textureLocation);
            }
            staticCache.clear();

            for (AnimatedTextureEntry entry : animatedCache.values()) {
                releaseAnimatedEntry(entry);
            }
            animatedCache.clear();
        }
    }

    public static void removeTexture(String cacheKey) {
        synchronized (textureCacheLock) {
            TextureEntry staticEntry = staticCache.remove(cacheKey);
            if (staticEntry != null) {
                Minecraft.getInstance().getTextureManager().release(staticEntry.textureLocation);
            }

            AnimatedTextureEntry animatedEntry = animatedCache.remove(cacheKey);
            if (animatedEntry != null) {
                releaseAnimatedEntry(animatedEntry);
            }
        }
    }

    private static void registerStaticTexture(String cacheKey, NativeImage nativeImage, int width, int height) {
        try {
            long id = textureIdCounter.getAndIncrement();
            Supplier<String> nameSupplier = () -> "cursedaddons_tooltip_image_" + id;
            DynamicTexture dynamicTexture = new DynamicTexture(nameSupplier, nativeImage);

            String path = "tooltip_images/" + id;
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath("cursedaddons", path);

            Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
            synchronized (textureCacheLock) {
                staticCache.put(cacheKey, new TextureEntry(location, width, height));
            }
        } catch (Exception e) {
            nativeImage.close();
            CursedAddons.LOGGER.error("[ImageTextureManager] Failed to register static texture for " + cacheKey + ": " + e.getMessage());
        }
    }

    private static void releaseAnimatedEntry(AnimatedTextureEntry entry) {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation loc : entry.frameLocations) {
            if (loc != null) {
                textureManager.release(loc);
            }
        }
    }

    private static int getTotalTextureCount() {
        // Caller must hold textureCacheLock — only count actually registered textures
        int count = staticCache.size();
        for (AnimatedTextureEntry entry : animatedCache.values()) {
            for (ResourceLocation loc : entry.frameLocations) {
                if (loc != null) count++;
            }
        }
        return count;
    }

    private static void enforceCacheLimit() {
        synchronized (textureCacheLock) {
            while (getTotalTextureCount() > MAX_TOTAL_TEXTURES) {
                // Evict oldest static entry first (cheaper to re-create), then oldest animated
                if (!staticCache.isEmpty()) {
                    String firstKey = staticCache.keySet().iterator().next();
                    TextureEntry entry = staticCache.remove(firstKey);
                    if (entry != null) {
                        Minecraft.getInstance().getTextureManager().release(entry.textureLocation);
                    }
                } else if (!animatedCache.isEmpty()) {
                    String firstKey = animatedCache.keySet().iterator().next();
                    AnimatedTextureEntry entry = animatedCache.remove(firstKey);
                    if (entry != null) {
                        releaseAnimatedEntry(entry);
                    }
                } else {
                    break;
                }
            }
        }
    }

    private static class TextureEntry {
        final ResourceLocation textureLocation;
        final int width;
        final int height;

        TextureEntry(ResourceLocation textureLocation, int width, int height) {
            this.textureLocation = textureLocation;
            this.width = width;
            this.height = height;
        }
    }

    private static class AnimatedTextureEntry {
        final ResourceLocation[] frameLocations;
        final int[] delays;
        final int width;
        final int height;
        final long startTime;
        final int totalDuration;

        AnimatedTextureEntry(ResourceLocation[] frameLocations, int[] delays, int width, int height) {
            this.frameLocations = frameLocations;
            this.delays = delays;
            this.width = width;
            this.height = height;
            this.startTime = System.currentTimeMillis();
            int total = 0;
            for (int d : delays) total += d;
            this.totalDuration = total;
        }

        ResourceLocation getCurrentFrameLocation() {
            if (frameLocations.length == 0) return null;

            if (totalDuration <= 0) return findNearestLoaded(0);

            long elapsed = System.currentTimeMillis() - startTime;
            long loopedTime = elapsed % totalDuration;

            int accumulator = 0;
            for (int i = 0; i < frameLocations.length; i++) {
                accumulator += delays[i];
                if (loopedTime < accumulator) {
                    return findNearestLoaded(i);
                }
            }

            return findNearestLoaded(frameLocations.length - 1);
        }

        /**
         * Returns the frame at the given index if loaded, otherwise falls back
         * to the nearest previously loaded frame. This allows partial rendering
         * while remaining frames are still being decoded asynchronously.
         */
        private ResourceLocation findNearestLoaded(int targetIndex) {
            if (frameLocations[targetIndex] != null) return frameLocations[targetIndex];

            for (int i = targetIndex - 1; i >= 0; i--) {
                if (frameLocations[i] != null) return frameLocations[i];
            }

            for (int i = targetIndex + 1; i < frameLocations.length; i++) {
                if (frameLocations[i] != null) return frameLocations[i];
            }

            return null;
        }
    }
}
