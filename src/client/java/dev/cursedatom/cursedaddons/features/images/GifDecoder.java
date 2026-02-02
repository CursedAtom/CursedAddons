package dev.cursedatom.cursedaddons.features.images;

import dev.cursedatom.cursedaddons.utils.LoggerUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

/**
 * Decodes GIF files into individual frames with delay information.
 * Handles GIF disposal methods for correct frame compositing.
 */
public final class GifDecoder {
    private static final int MAX_FRAMES = 200;
    private static final int DEFAULT_DELAY_MS = 100;
    private static final int MIN_DELAY_MS = 20;

    private GifDecoder() {}

    public static GifData decode(InputStream inputStream, int maxWidth, int maxHeight) throws Exception {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new Exception("No GIF ImageReader available");
        }

        ImageReader reader = readers.next();
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(inputStream)) {
            reader.setInput(imageStream);

            int numFrames = Math.min(reader.getNumImages(true), MAX_FRAMES);
            if (numFrames <= 0) {
                throw new Exception("GIF has no frames");
            }

            // Read the first frame to get canvas dimensions
            BufferedImage firstFrame = reader.read(0);
            int canvasWidth = firstFrame.getWidth();
            int canvasHeight = firstFrame.getHeight();

            // Try to get logical screen dimensions from stream metadata
            IIOMetadata streamMeta = reader.getStreamMetadata();
            if (streamMeta != null) {
                int[] screenDims = extractLogicalScreenDimensions(streamMeta);
                if (screenDims != null) {
                    canvasWidth = Math.max(canvasWidth, screenDims[0]);
                    canvasHeight = Math.max(canvasHeight, screenDims[1]);
                }
            }

            // Calculate scaling factor to fit within max dimensions
            float scale = calculateScale(canvasWidth, canvasHeight, maxWidth, maxHeight);
            int scaledWidth = (int) (canvasWidth * scale);
            int scaledHeight = (int) (canvasHeight * scale);
            boolean needsScaling = scale < 1.0f;

            List<BufferedImage> frames = new ArrayList<>();
            int[] delays = new int[numFrames];

            // Canvas for compositing frames (use scaled dimensions if needed)
            BufferedImage canvas = new BufferedImage(
                needsScaling ? scaledWidth : canvasWidth,
                needsScaling ? scaledHeight : canvasHeight,
                BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = canvas.createGraphics();
            g.setBackground(new Color(0, 0, 0, 0));

            // Enable high-quality scaling if needed
            if (needsScaling) {
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                  java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                  java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            }

            BufferedImage previousCanvas = null;
            String previousDisposal = "none";
            Rectangle previousBounds = null;

            for (int i = 0; i < numFrames; i++) {
                BufferedImage rawFrame = (i == 0) ? firstFrame : reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);

                FrameInfo info = parseFrameInfo(metadata, rawFrame);
                delays[i] = info.delay;

                // Apply disposal from the previous frame before drawing the new one
                if (i > 0) {
                    switch (previousDisposal) {
                        case "restoreToBackgroundColor":
                            if (previousBounds != null) {
                                g.setComposite(AlphaComposite.Clear);
                                if (needsScaling) {
                                    g.fillRect(
                                        (int) (previousBounds.x * scale),
                                        (int) (previousBounds.y * scale),
                                        (int) (previousBounds.width * scale),
                                        (int) (previousBounds.height * scale)
                                    );
                                } else {
                                    g.fillRect(previousBounds.x, previousBounds.y,
                                              previousBounds.width, previousBounds.height);
                                }
                                g.setComposite(AlphaComposite.SrcOver);
                            }
                            break;
                        case "restoreToPrevious":
                            if (previousCanvas != null) {
                                g.setComposite(AlphaComposite.Src);
                                g.drawImage(previousCanvas, 0, 0, null);
                                g.setComposite(AlphaComposite.SrcOver);
                            }
                            break;
                        // "none" / "doNotDispose" — leave canvas as is
                    }
                }

                // Save canvas before drawing if this frame's disposal is "restoreToPrevious"
                if ("restoreToPrevious".equals(info.disposal)) {
                    previousCanvas = copyImage(canvas);
                }

                // Draw current frame at its offset position (scaled if needed)
                if (needsScaling) {
                    int scaledX = (int) (info.x * scale);
                    int scaledY = (int) (info.y * scale);
                    int scaledFrameWidth = (int) (rawFrame.getWidth() * scale);
                    int scaledFrameHeight = (int) (rawFrame.getHeight() * scale);
                    g.drawImage(rawFrame, scaledX, scaledY, scaledFrameWidth, scaledFrameHeight, null);
                } else {
                    g.drawImage(rawFrame, info.x, info.y, null);
                }

                // Store the fully composited frame
                frames.add(copyImage(canvas));

                previousDisposal = info.disposal;
                previousBounds = new Rectangle(info.x, info.y, rawFrame.getWidth(), rawFrame.getHeight());
            }

            g.dispose();
            reader.dispose();

            return new GifData(frames, delays, scaledWidth, scaledHeight);
        } finally {
            reader.dispose();
        }
    }

    private static float calculateScale(int width, int height, int maxWidth, int maxHeight) {
        if (maxWidth <= 0 || maxHeight <= 0) {
            return 1.0f;
        }
        float scaleX = (float) maxWidth / width;
        float scaleY = (float) maxHeight / height;
        return Math.min(Math.min(scaleX, scaleY), 1.0f);
    }

    private static FrameInfo parseFrameInfo(IIOMetadata metadata, BufferedImage frame) {
        int delay = DEFAULT_DELAY_MS;
        String disposal = "none";
        int x = 0;
        int y = 0;

        try {
            Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
            NodeList children = tree.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                if ("GraphicControlExtension".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();

                    Node delayNode = attrs.getNamedItem("delayTime");
                    if (delayNode != null) {
                        int delayCs = Integer.parseInt(delayNode.getNodeValue());
                        // GIF delay is in centiseconds (1/100 second)
                        delay = delayCs * 10;
                        if (delay < MIN_DELAY_MS) {
                            delay = DEFAULT_DELAY_MS;
                        }
                    }

                    Node disposalNode = attrs.getNamedItem("disposalMethod");
                    if (disposalNode != null) {
                        disposal = disposalNode.getNodeValue();
                    }
                }

                if ("ImageDescriptor".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    Node xNode = attrs.getNamedItem("imageLeftPosition");
                    Node yNode = attrs.getNamedItem("imageTopPosition");
                    if (xNode != null) x = Integer.parseInt(xNode.getNodeValue());
                    if (yNode != null) y = Integer.parseInt(yNode.getNodeValue());
                }
            }
        } catch (Exception e) {
            LoggerUtils.warn("[GifDecoder] Failed to parse frame metadata: " + e.getMessage());
        }

        return new FrameInfo(x, y, delay, disposal);
    }

    private static int[] extractLogicalScreenDimensions(IIOMetadata streamMeta) {
        try {
            Node tree = streamMeta.getAsTree("javax_imageio_gif_stream_1.0");
            NodeList children = tree.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if ("LogicalScreenDescriptor".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    int w = Integer.parseInt(attrs.getNamedItem("logicalScreenWidth").getNodeValue());
                    int h = Integer.parseInt(attrs.getNamedItem("logicalScreenHeight").getNodeValue());
                    if (w > 0 && h > 0) {
                        return new int[]{w, h};
                    }
                }
            }
        } catch (Exception e) {
            // Ignore — fall back to frame dimensions
        }
        return null;
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static class FrameInfo {
        final int x;
        final int y;
        final int delay;
        final String disposal;

        FrameInfo(int x, int y, int delay, String disposal) {
            this.x = x;
            this.y = y;
            this.delay = delay;
            this.disposal = disposal;
        }
    }

    public static class GifData {
        private final List<BufferedImage> frames;
        private final int[] delays;
        private final int width;
        private final int height;

        GifData(List<BufferedImage> frames, int[] delays, int width, int height) {
            this.frames = frames;
            this.delays = delays;
            this.width = width;
            this.height = height;
        }

        public List<BufferedImage> getFrames() {
            return frames;
        }

        public int[] getDelays() {
            return delays;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getFrameCount() {
            return frames.size();
        }

        public int getTotalDuration() {
            int total = 0;
            for (int d : delays) total += d;
            return total;
        }
    }
}
