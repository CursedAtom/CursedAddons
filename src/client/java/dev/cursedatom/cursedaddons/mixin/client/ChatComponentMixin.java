package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.general.ClickEventsPreviewer;
import dev.cursedatom.cursedaddons.features.images.ImageHoverPreview;
import dev.cursedatom.cursedaddons.features.images.PlainTextUrlAnnotator;
import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for {@link net.minecraft.client.gui.components.ChatComponent} that adds image URL annotation
 * and click event preview enrichment to incoming chat messages.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component cursedaddons$annotateImageUrls(Component content) {
        return PlainTextUrlAnnotator.annotate(content);
    }

    @Inject(method = "getClickedComponentStyleAt", at = @At(value = "RETURN"), cancellable = true, require = 0)
    public void modifyHoverEvent(double x, double y, CallbackInfoReturnable<Style> cir) {
        Style style = cir.getReturnValue();
        if (style == null) return;

        if (cursedaddons$hasImageUrlClickEvent(style)) {
            if (ImageHoverPreview.isEnabled()) {
                // Ensure image-URL styles have a HoverEvent so renderComponentHoverEffect fires —
                // covers messages received while the feature was off.
                if (style.getHoverEvent() == null) {
                    style = style.withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("cursedaddons.texts.ImageHoverPreview.Loading")));
                    cir.setReturnValue(style);
                }
                return;
            } else {
                // Feature disabled: clear our baked-in "Loading..." placeholder so it doesn't
                // show as raw text. ClickEventsPreviewer can then run on this style normally.
                if (cursedaddons$isLoadingPlaceholder(style.getHoverEvent())) {
                    style = style.withHoverEvent(null);
                    cir.setReturnValue(style);
                }
            }
        }

        // Apply click event previews dynamically at hover time so toggling the feature
        // immediately affects all messages without needing them to be re-sent.
        if (ConfigProvider.getBoolean(ConfigKeys.CLICK_EVENTS_ENABLED, false)) {
            style = ClickEventsPreviewer.enrichStyleWithPreview(style);
            cir.setReturnValue(style);
        }
    }

    /** Returns true if the hover event is exactly our "Loading..." placeholder. */
    @Unique
    private boolean cursedaddons$isLoadingPlaceholder(HoverEvent hoverEvent) {
        if (!(hoverEvent instanceof HoverEvent.ShowText showText)) return false;
        Component value = showText.value();
        return value.getContents() instanceof TranslatableContents tc
            && tc.getKey().equals("cursedaddons.texts.ImageHoverPreview.Loading")
            && value.getSiblings().isEmpty();
    }

    /** Returns true if the style has an image-URL click event, regardless of feature enabled state. */
    @Unique
    private boolean cursedaddons$hasImageUrlClickEvent(Style style) {
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent instanceof ClickEvent.OpenUrl openUrlEvent) {
            String url = openUrlEvent.uri().toString();
            return ImageHoverPreview.isImageUrl(url) && ImageHoverPreview.isWhitelisted(url);
        }
        return false;
    }
}
