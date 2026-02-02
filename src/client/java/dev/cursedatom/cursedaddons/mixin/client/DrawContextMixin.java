package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.images.ImageCache;
import dev.cursedatom.cursedaddons.features.images.ImageHoverEvent;
import dev.cursedatom.cursedaddons.features.images.ImageHoverPreview;
import dev.cursedatom.cursedaddons.features.images.ImageTextureManager;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.joml.Vector2ic;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class DrawContextMixin {

    @Shadow
    public abstract int guiWidth();

    @Shadow
    public abstract int guiHeight();

    @Shadow
    public abstract void nextStratum();

    @Shadow
    public abstract void setTooltipForNextFrame(Font font, Component component, int x, int y);

    @Inject(at = @At("HEAD"), method = "renderComponentHoverEffect", cancellable = true)
    private void cursedaddons$renderImageHoverEffect(Font font, Style style, int mouseX, int mouseY, CallbackInfo ci) {
        if (!ImageHoverPreview.isEnabled() || style == null) return;

        String imageUrl = cursedaddons$findImageUrl(style);
        if (imageUrl == null) return;

        ci.cancel();

        GuiGraphics graphics = (GuiGraphics) (Object) this;

        // Determine max dimensions based on shift state
        Minecraft minecraft = Minecraft.getInstance();
        boolean shiftHeld = GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                         || GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        int screenWidth = this.guiWidth();
        int screenHeight = this.guiHeight();

        int maxWidth, maxHeight;
        if (shiftHeld) {
            // Full screen size (minus padding)
            maxWidth = screenWidth - 24;
            maxHeight = screenHeight - 24;
        } else {
            // Tooltip size (25% of screen or config values)
            int defaultMaxWidth = Math.max(screenWidth / 4, 280);
            int defaultMaxHeight = Math.max(screenHeight / 4, 200);
            maxWidth = cursedaddons$getConfigInt("general.ImageHoverPreview.MaxWidth", defaultMaxWidth);
            maxHeight = cursedaddons$getConfigInt("general.ImageHoverPreview.MaxHeight", defaultMaxHeight);
        }

        String cacheKey = ImageTextureManager.getCacheKey(imageUrl, maxWidth, maxHeight);
        ImageHoverEvent.ImageData imageData = ImageTextureManager.getImageData(cacheKey, imageUrl);

        if (imageData == null || imageData.getTextureLocation() == null) {
            cursedaddons$loadImageAsync(imageUrl, maxWidth, maxHeight);
            this.setTooltipForNextFrame(font,
                Component.translatable("cursedaddons.texts.ImageHoverPreview.Loading"),
                mouseX, mouseY);
            return;
        }

        this.nextStratum();
        cursedaddons$renderImageTooltip(graphics, imageData, mouseX, mouseY, shiftHeld);
    }

    @Unique
    private String cursedaddons$findImageUrl(Style style) {
        // Only check if the component has a click event to an image URL.
        // This ensures we only show previews for explicitly clickable links,
        // not for unformatted text or other formatting.
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent instanceof ClickEvent.OpenUrl openUrlEvent) {
            String url = openUrlEvent.uri().toString();
            if (ImageHoverPreview.isImageUrl(url) && ImageHoverPreview.isWhitelisted(url)) {
                return url;
            }
        }

        return null;
    }

    @Unique
    private void cursedaddons$loadImageAsync(String imageUrl, int maxWidth, int maxHeight) {
        String cacheKey = ImageTextureManager.getCacheKey(imageUrl, maxWidth, maxHeight);
        if (ImageTextureManager.hasTextures(cacheKey)) return;

        ImageCache.loadImage(imageUrl, maxWidth, maxHeight).thenAcceptAsync(result -> {
            if (result != null) {
                // NativeImage.read() runs here on the async thread;
                // only fast DynamicTexture registration is scheduled on the render thread
                ImageTextureManager.prepareAndRegister(cacheKey, result);
            }
        });
    }

    @Unique
    private void cursedaddons$renderImageTooltip(GuiGraphics graphics, ImageHoverEvent.ImageData imageData, int mouseX, int mouseY, boolean shiftHeld) {
        int screenWidth = this.guiWidth();
        int screenHeight = this.guiHeight();

        // Image is already pre-scaled to fit our requirements, just use its dimensions
        int imageWidth = imageData.getWidth();
        int imageHeight = imageData.getHeight();

        int paddingLeft = cursedaddons$getConfigInt("general.ImageHoverPreview.PaddingLeft", 8);
        int paddingRight = cursedaddons$getConfigInt("general.ImageHoverPreview.PaddingRight", 8);
        int paddingTop = cursedaddons$getConfigInt("general.ImageHoverPreview.PaddingTop", 8);
        int paddingBottom = cursedaddons$getConfigInt("general.ImageHoverPreview.PaddingBottom", 8);

        int totalWidth = imageWidth + paddingLeft + paddingRight;
        int totalHeight = imageHeight + paddingTop + paddingBottom;

        Vector2ic pos = DefaultTooltipPositioner.INSTANCE.positionTooltip(
            screenWidth, screenHeight, mouseX, mouseY, totalWidth, totalHeight);
        int x = pos.x();
        int y = pos.y();

        TooltipRenderUtil.renderTooltipBackground(graphics, x, y, totalWidth, totalHeight, null);

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            imageData.getTextureLocation(),
            x + paddingLeft, y + paddingTop,
            0.0f, 0.0f,
            imageWidth, imageHeight,
            imageWidth, imageHeight
        );
    }

    @Unique
    private int cursedaddons$getConfigInt(String key, int defaultValue) {
        Object value = ConfigProvider.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}