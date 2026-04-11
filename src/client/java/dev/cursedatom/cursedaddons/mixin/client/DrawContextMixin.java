package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.features.general.ClickEventsPreviewer;
import dev.cursedatom.cursedaddons.features.images.ImageCache;
import dev.cursedatom.cursedaddons.features.images.ImageHoverEvent;
import dev.cursedatom.cursedaddons.features.images.ImageHoverPreview;
import dev.cursedatom.cursedaddons.features.images.ImageTextureManager;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link net.minecraft.client.gui.GuiGraphics} that intercepts component hover rendering
 * to display image previews when hovering over image URLs in chat.
 */
@Mixin(GuiGraphics.class)
public abstract class DrawContextMixin {

    @Shadow
    private Style hoveredTextStyle;

    @Shadow
    private Style clickableTextStyle;

    @Shadow
    private int mouseX;

    @Shadow
    private int mouseY;

    @Shadow
    public abstract int guiWidth();

    @Shadow
    public abstract int guiHeight();

    @Shadow
    public abstract void nextStratum();

    @Shadow
    public abstract void setTooltipForNextFrame(Font font, Component component, int x, int y);

    /**
     * For click-event-only styles (no HoverEvent), hoveredTextStyle is never set, so
     * renderComponentHoverEffect is never called. clickableTextStyle IS set for these styles,
     * so we call renderComponentHoverEffect manually here when the feature is enabled.
     */
    @Inject(at = @At("RETURN"), method = "renderDeferredElements")
    private void cursedaddons$renderClickEventPreview(CallbackInfo ci) {
        if (hoveredTextStyle != null) return;
        if (clickableTextStyle == null) return;
        if (!ConfigProvider.getBoolean(ConfigKeys.CLICK_EVENTS_ENABLED, false)) return;
        GuiGraphics graphics = (GuiGraphics) (Object) this;
        graphics.renderComponentHoverEffect(Minecraft.getInstance().font, clickableTextStyle, mouseX, mouseY);
    }

    /**
     * Enriches the hover style with click event preview text for non-image-URL styles.
     * For image URLs with image preview disabled, strips the "Loading..." placeholder so
     * vanilla doesn't render it, then falls through to click event enrichment if enabled.
     */
    @ModifyVariable(method = "renderComponentHoverEffect", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Style cursedaddons$enrichHoverStyle(Style style) {
        if (style == null) return null;
        if (cursedaddons$findImageUrl(style) != null) {
            if (ImageHoverPreview.isEnabled()) return style; // @Inject below handles rendering
            // Image preview disabled: strip the "Loading..." placeholder so it doesn't render as raw text.
            // Fall through to click event enrichment below.
            style = style.withHoverEvent(null);
        }
        if (ConfigProvider.getBoolean(ConfigKeys.CLICK_EVENTS_ENABLED, false)) {
            return ClickEventsPreviewer.enrichStyleWithPreview(style);
        }
        return style;
    }

    @Inject(at = @At("HEAD"), method = "renderComponentHoverEffect", cancellable = true)
    private void cursedaddons$renderImageHoverEffect(Font font, Style style, int mouseX, int mouseY, CallbackInfo ci) {
        if (style == null) return;

        String imageUrl = cursedaddons$findImageUrl(style);
        if (imageUrl == null) return;

        // When image preview is disabled, don't cancel — @ModifyVariable already stripped the
        // "Loading..." placeholder and may have added click event preview; let vanilla render it.
        if (!ImageHoverPreview.isEnabled()) return;

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
