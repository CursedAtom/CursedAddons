package dev.cursedatom.cursedaddons.features.images;

import com.mojang.blaze3d.platform.NativeImage;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Manages GPU texture lifecycle for image tooltips.
 * Handles static and animated (GIF) textures.
 */
public final class ImageTextureManager {
    private static final Map<String, TextureEntry> staticCache = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTextureEntry> animatedCache = new ConcurrentHashMap<>();
    private static final int MAX_TOTAL_TEXTURES = 50;

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
            LoggerUtils.error("[ImageTextureManager] Failed to prepare textures for " + cacheKey + ": " + e.getMessage());
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

        int totalFrames = framePngData.length;
        int width = result.getWidth();
        int height = result.getHeight();

        // Create the entry with empty frame slots up front so it can be rendered
        // as soon as the first frame is registered
        ResourceLocation[] frameLocations = new ResourceLocation[totalFrames];
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AnimatedTextureEntry entry = new AnimatedTextureEntry(frameLocations, delays, width, height);

        // Register the entry immediately so hasTextures() returns true and
        // rendering can begin as soon as frame 0 is ready
        animatedCache.put(cacheKey, entry);

        // Decode first batch synchronously on the current async thread
        int firstBatchEnd = Math.min(GIF_BATCH_SIZE, totalFrames);
        try {
            registerFrameBatch(cacheKey, framePngData, frameLocations, 0, firstBatchEnd, cancelled);
        } catch (Exception e) {
            LoggerUtils.error("[ImageTextureManager] Failed to prepare initial GIF batch for " + cacheKey + ": " + e.getMessage());
            animatedCache.remove(cacheKey);
            return;
        }

        // Decode remaining batches asynchronously
        if (firstBatchEnd < totalFrames) {
            CompletableFuture.runAsync(() -> {
                for (int batchStart = firstBatchEnd; batchStart < totalFrames; batchStart += GIF_BATCH_SIZE) {
                    if (cancelled.get() || !animatedCache.containsKey(cacheKey)) return;
                    int batchEnd = Math.min(batchStart + GIF_BATCH_SIZE, totalFrames);
                    try {
                        registerFrameBatch(cacheKey, framePngData, frameLocations, batchStart, batchEnd, cancelled);
                    } catch (Exception e) {
                        LoggerUtils.error("[ImageTextureManager] Failed to prepare GIF batch [" + batchStart + "-" + batchEnd + "] for " + cacheKey + ": " + e.getMessage());
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
            if (cancelled.get() || !animatedCache.containsKey(cacheKey)) {
                closeBatch(batch);
                return;
            }
            for (int i = 0; i < batch.length; i++) {
                int frameIndex = batchStart + i;
                try {
                    Supplier<String> nameSupplier = () -> "cursedaddons_gif_" + cacheKey.hashCode() + "_frame_" + frameIndex;
                    DynamicTexture dynamicTexture = new DynamicTexture(nameSupplier, batch[i]);

                    String path = "gif_frames/" + Integer.toHexString(cacheKey.hashCode()) + "_" + frameIndex;
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath("cursedaddons", path.toLowerCase());

                    Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
                    frameLocations[frameIndex] = location;
                    batch[i] = null; // Ownership transferred
                } catch (Exception e) {
                    LoggerUtils.error("[ImageTextureManager] Failed to register GIF frame " + frameIndex + " for " + cacheKey + ": " + e.getMessage());
                }
            }
            closeBatch(batch); // Clean up any that failed to transfer
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
        // Check static cache
        TextureEntry entry = staticCache.get(cacheKey);
        if (entry != null) {
            return new ImageHoverEvent.ImageData(imageUrl, entry.textureLocation, entry.width, entry.height);
        }

        // Check animated cache
        AnimatedTextureEntry animated = animatedCache.get(cacheKey);
        if (animated != null) {
            ResourceLocation currentFrame = animated.getCurrentFrameLocation();
            if (currentFrame != null) {
                return new ImageHoverEvent.ImageData(imageUrl, currentFrame, animated.width, animated.height);
            }
        }

        return null;
    }

    /**
     * Checks if textures have been created for this cache key.
     */
    public static boolean hasTextures(String cacheKey) {
        return staticCache.containsKey(cacheKey) || animatedCache.containsKey(cacheKey);
    }

    public static void clear() {
        var textureManager = Minecraft.getInstance().getTextureManager();

        for (TextureEntry entry : staticCache.values()) {
            textureManager.release(entry.textureLocation);
        }
        staticCache.clear();

        for (AnimatedTextureEntry entry : animatedCache.values()) {
            releaseAnimatedEntry(entry);
        }
        animatedCache.clear();
    }

    public static void removeTexture(String imageUrl) {
        TextureEntry staticEntry = staticCache.remove(imageUrl);
        if (staticEntry != null) {
            Minecraft.getInstance().getTextureManager().release(staticEntry.textureLocation);
        }

        AnimatedTextureEntry animatedEntry = animatedCache.remove(imageUrl);
        if (animatedEntry != null) {
            releaseAnimatedEntry(animatedEntry);
        }
    }

    private static void registerStaticTexture(String cacheKey, NativeImage nativeImage, int width, int height) {
        try {
            Supplier<String> nameSupplier = () -> "cursedaddons_tooltip_image_" + cacheKey.hashCode();
            DynamicTexture dynamicTexture = new DynamicTexture(nameSupplier, nativeImage);

            String path = "tooltip_images/" + Integer.toHexString(cacheKey.hashCode());
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath("cursedaddons", path.toLowerCase());

            Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
            staticCache.put(cacheKey, new TextureEntry(location, width, height));
        } catch (Exception e) {
            nativeImage.close();
            LoggerUtils.error("[ImageTextureManager] Failed to register static texture for " + cacheKey + ": " + e.getMessage());
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
        int count = staticCache.size();
        for (AnimatedTextureEntry entry : animatedCache.values()) {
            count += entry.frameLocations.length;
        }
        return count;
    }

    private static void enforceCacheLimit() {
        while (getTotalTextureCount() > MAX_TOTAL_TEXTURES) {
            // Evict from static cache first (cheaper to re-create)
            if (!staticCache.isEmpty()) {
                String firstKey = staticCache.keySet().iterator().next();
                removeTexture(firstKey);
            } else if (!animatedCache.isEmpty()) {
                String firstKey = animatedCache.keySet().iterator().next();
                removeTexture(firstKey);
            } else {
                break;
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

        AnimatedTextureEntry(ResourceLocation[] frameLocations, int[] delays, int width, int height) {
            this.frameLocations = frameLocations;
            this.delays = delays;
            this.width = width;
            this.height = height;
            this.startTime = System.currentTimeMillis();
        }

        ResourceLocation getCurrentFrameLocation() {
            if (frameLocations.length == 0) return null;

            long elapsed = System.currentTimeMillis() - startTime;

            int totalDuration = 0;
            for (int d : delays) totalDuration += d;
            if (totalDuration <= 0) return findNearestLoaded(0);

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
            // Try the target frame first
            if (frameLocations[targetIndex] != null) return frameLocations[targetIndex];

            // Fall back to the closest loaded frame before the target
            for (int i = targetIndex - 1; i >= 0; i--) {
                if (frameLocations[i] != null) return frameLocations[i];
            }

            // Last resort: any loaded frame after the target
            for (int i = targetIndex + 1; i < frameLocations.length; i++) {
                if (frameLocations[i] != null) return frameLocations[i];
            }

            return null;
        }
    }
}
