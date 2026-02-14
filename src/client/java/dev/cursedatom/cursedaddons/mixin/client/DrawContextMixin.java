package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.images.ImageCache;
import dev.cursedatom.cursedaddons.features.images.ImageHoverEvent;
import dev.cursedatom.cursedaddons.features.images.ImageHoverPreview;
import dev.cursedatom.cursedaddons.features.images.ImageTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
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

/**
 * Mixin for {@link net.minecraft.client.gui.GuiGraphics} that intercepts component hover rendering
 * to display image previews when hovering over image URLs in chat.
 */
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

        Minecraft minecraft = Minecraft.getInstance();
        boolean shiftHeld = GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                         || GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        int screenWidth = this.guiWidth();
        int screenHeight = this.guiHeight();

        int maxWidth, maxHeight;
        if (shiftHeld) {
            maxWidth = screenWidth;
            maxHeight = screenHeight;
        } else {
            maxWidth = screenWidth / 4;
            maxHeight = screenHeight / 4;
        }

        String cacheKey = ImageTextureManager.getCacheKey(imageUrl, maxWidth, maxHeight);
        ImageHoverEvent.ImageData imageData = ImageTextureManager.getImageData(cacheKey, imageUrl);

        if (imageData == null || imageData.getTextureLocation() == null) {
            String failureReason = ImageCache.getFailureReason(imageUrl, maxWidth, maxHeight);
            if (failureReason != null) {
                this.setTooltipForNextFrame(font,
                    Component.literal("Error Loading Image: " + failureReason),
                    mouseX, mouseY);
            } else {
                cursedaddons$loadImageAsync(imageUrl, maxWidth, maxHeight);
                this.setTooltipForNextFrame(font,
                    Component.translatable("cursedaddons.texts.ImageHoverPreview.Loading"),
                    mouseX, mouseY);
            }
            return;
        }

        this.nextStratum();
        cursedaddons$renderImageTooltip(graphics, imageData, mouseX, mouseY);
    }

    @Unique
    private String cursedaddons$findImageUrl(Style style) {
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
                ImageTextureManager.prepareAndRegister(cacheKey, result);
            }
        });
    }

    @Unique
    private void cursedaddons$renderImageTooltip(GuiGraphics graphics, ImageHoverEvent.ImageData imageData, int mouseX, int mouseY) {
        int screenWidth = this.guiWidth();
        int screenHeight = this.guiHeight();

        int imageWidth = imageData.getWidth();
        int imageHeight = imageData.getHeight();

        Vector2ic pos = DefaultTooltipPositioner.INSTANCE.positionTooltip(
            screenWidth, screenHeight, mouseX, mouseY, imageWidth, imageHeight);
        int x = pos.x();
        int y = pos.y();

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            imageData.getTextureLocation(),
            x, y,
            0.0f, 0.0f,
            imageWidth, imageHeight,
            imageWidth, imageHeight
        );
    }
}
