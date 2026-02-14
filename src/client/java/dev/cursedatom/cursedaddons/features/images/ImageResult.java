package dev.cursedatom.cursedaddons.features.images;

/**
 * Unified result for a loaded image. Can store either raw ARGB pixel data
 * (for static images, avoiding PNG encode/decode roundtrip) or pre-encoded
 * PNG byte data (for GIF frames where compression matters).
 *
 * Can represent either a static image or an animated GIF (multiple frames + delays).
 */
public final class ImageResult {
    private final byte[] staticPngData;
    private final int[] rawArgbPixels;
    private final byte[][] gifFramePngData;
    private final int[] gifDelays;
    private final int width;
    private final int height;

    private ImageResult(byte[] staticPngData, int[] rawArgbPixels, byte[][] gifFramePngData, int[] gifDelays, int width, int height) {
        this.staticPngData = staticPngData;
        this.rawArgbPixels = rawArgbPixels;
        this.gifFramePngData = gifFramePngData;
        this.gifDelays = gifDelays;
        this.width = width;
        this.height = height;
    }

    public static ImageResult ofStatic(byte[] pngData, int width, int height) {
        return new ImageResult(pngData, null, null, null, width, height);
    }

    public static ImageResult ofStaticRaw(int[] argbPixels, int width, int height) {
        return new ImageResult(null, argbPixels, null, null, width, height);
    }

    public static ImageResult ofGif(byte[][] framePngData, int[] delays, int width, int height) {
        return new ImageResult(null, null, framePngData, delays, width, height);
    }

    public boolean isAnimated() {
        return gifFramePngData != null && gifFramePngData.length > 1;
    }

    public boolean hasRawPixels() {
        return rawArgbPixels != null;
    }

    public int[] getRawArgbPixels() {
        return rawArgbPixels;
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
