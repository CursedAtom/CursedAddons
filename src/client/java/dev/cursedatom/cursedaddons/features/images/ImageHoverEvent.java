package dev.cursedatom.cursedaddons.features.images;

import net.minecraft.resources.ResourceLocation;

/**
 * Container for image hover data used by {@link dev.cursedatom.cursedaddons.mixin.client.DrawContextMixin}
 * to render image tooltip overlays.
 */
public final class ImageHoverEvent {

    private ImageHoverEvent() {
    }

    public static class ImageData {
        private final String imageUrl;
        private final ResourceLocation textureLocation;
        private final int width;
        private final int height;

        public ImageData(String imageUrl, ResourceLocation textureLocation, int width, int height) {
            this.imageUrl = imageUrl;
            this.textureLocation = textureLocation;
            this.width = width;
            this.height = height;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public ResourceLocation getTextureLocation() {
            return textureLocation;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
