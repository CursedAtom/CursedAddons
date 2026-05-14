package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.features.images.ImageHoverPreview;
import dev.cursedatom.cursedaddons.features.images.PlainTextUrlAnnotator;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mixin for {@link net.minecraft.client.gui.components.ChatComponent} that adds image URL annotation
 * to incoming chat messages.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component cursedaddons$annotateImageUrls(Component content) {
        content = PlainTextUrlAnnotator.annotate(content);
        content = cursedaddons$ensureImageHoverEvents(content);
        if (ConfigProvider.getBoolean(ConfigKeys.CLICK_EVENTS_ENABLED, false)) {
            content = cursedaddons$ensureClickEventHoverEvents(content);
        }
        return content;
    }

    /**
     * Adds a "Loading..." HoverEvent to any image URL ClickEvent styles that lack one.
     * Covers server-sent messages that include ClickEvent.OpenUrl but no HoverEvent.
     * Returns the original component unchanged if no modifications are needed.
     */
    @Unique
    private Component cursedaddons$ensureImageHoverEvents(Component content) {
        // Check if any segment needs a HoverEvent added
        boolean[] needsFix = {false};
        content.visit((style, text) -> {
            if (needsFix[0]) return Optional.empty();
            if (cursedaddons$isImageUrlWithoutHoverEvent(style)) {
                needsFix[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (!needsFix[0]) return content;

        // Rebuild with HoverEvent added to image URL ClickEvent styles
        List<Object[]> segments = new ArrayList<>();
        content.visit((style, text) -> {
            segments.add(new Object[]{style, text});
            return Optional.empty();
        }, Style.EMPTY);

        MutableComponent result = Component.empty();
        for (Object[] seg : segments) {
            Style style = (Style) seg[0];
            String text = (String) seg[1];
            if (cursedaddons$isImageUrlWithoutHoverEvent(style)) {
                style = style.withHoverEvent(new HoverEvent.ShowText(
                    Component.translatable("cursedaddons.texts.ImageHoverPreview.Loading")));
            }
            result.append(Component.literal(text).setStyle(style));
        }
        return result;
    }

    /**
     * Adds a blank ShowText hover event to any style that has a click event or insertion text
     * but no hover event (and isn't an image URL; those are handled by
     * cursedaddons$ensureImageHoverEvents). This makes vanilla set hoveredTextStyle for these
     * elements so componentHoverEffect is called, allowing the @ModifyVariable in
     * DrawContextMixin to inject the click event / insertion preview.
     */
    @Unique
    private Component cursedaddons$ensureClickEventHoverEvents(Component content) {
        boolean[] needsFix = {false};
        content.visit((style, text) -> {
            if (!needsFix[0] && cursedaddons$needsHoverPlaceholder(style)) {
                needsFix[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (!needsFix[0]) return content;

        List<Object[]> segments = new ArrayList<>();
        content.visit((style, text) -> {
            segments.add(new Object[]{style, text});
            return Optional.empty();
        }, Style.EMPTY);

        MutableComponent result = Component.empty();
        for (Object[] seg : segments) {
            Style style = (Style) seg[0];
            String text = (String) seg[1];
            if (cursedaddons$needsHoverPlaceholder(style)) {
                style = style.withHoverEvent(new HoverEvent.ShowText(Component.empty()));
            }
            result.append(Component.literal(text).setStyle(style));
        }
        return result;
    }

    @Unique
    private boolean cursedaddons$needsHoverPlaceholder(Style style) {
        if (style.getHoverEvent() != null) return false;
        if (style.getClickEvent() != null) return true;
        String insertion = style.getInsertion();
        return insertion != null && !insertion.isBlank();
    }

    @Unique
    private boolean cursedaddons$isImageUrlWithoutHoverEvent(Style style) {
        if (style.getHoverEvent() != null) return false;
        if (!(style.getClickEvent() instanceof ClickEvent.OpenUrl openUrl)) return false;
        String url = openUrl.uri().toString();
        return ImageHoverPreview.isImageUrl(url) && ImageHoverPreview.isWhitelisted(url);
    }
}
