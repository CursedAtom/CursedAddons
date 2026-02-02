package dev.cursedatom.cursedaddons.features.images;

/**
 * Unified result for a loaded image. Stores pre-encoded PNG byte data
 * (the heavy encoding is done on the async thread). The render thread
 * only needs to do NativeImage.read(bytes) which is fast (~1-5ms).
 *
 * Can represent either a static image or an animated GIF (multiple frames + delays).
 */
public final class ImageResult {
    private final byte[] staticPngData;
    private final byte[][] gifFramePngData;
    private final int[] gifDelays;
    private final int width;
    private final int height;

    private ImageResult(byte[] staticPngData, byte[][] gifFramePngData, int[] gifDelays, int width, int height) {
        this.staticPngData = staticPngData;
        this.gifFramePngData = gifFramePngData;
        this.gifDelays = gifDelays;
        this.width = width;
        this.height = height;
    }

    public static ImageResult ofStatic(byte[] pngData, int width, int height) {
        return new ImageResult(pngData, null, null, width, height);
    }

    public static ImageResult ofGif(byte[][] framePngData, int[] delays, int width, int height) {
        return new ImageResult(null, framePngData, delays, width, height);
    }

    public boolean isAnimated() {
        return gifFramePngData != null && gifFramePngData.length > 1;
    }

    public byte[] getStaticPngData() {
        return staticPngData;
    }

    public byte[][] getGifFramePngData() {
        return gifFramePngData;
    }

    public int[] getGifDelays() {
        return gifDelays;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFrameCount() {
        return gifFramePngData != null ? gifFramePngData.length : 1;
    }
}
