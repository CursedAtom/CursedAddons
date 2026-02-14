package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.general.ClickEventsPreviewer;
import dev.cursedatom.cursedaddons.features.images.ImageHoverPreview;
import dev.cursedatom.cursedaddons.features.images.PlainTextUrlAnnotator;
import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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
        if (!ImageHoverPreview.isEnabled()) return content;
        return PlainTextUrlAnnotator.annotate(content);
    }

    @Inject(method = "getClickedComponentStyleAt", at = @At(value = "RETURN"), cancellable = true)
    public void modifyHoverEvent(double x, double y, CallbackInfoReturnable<Style> cir) {
        Style style = cir.getReturnValue();
        if (style == null) return;

        // Handle click event previews (skip for image URLs — DrawContextMixin handles those)
        if (ConfigProvider.getBoolean(ConfigKeys.CLICK_EVENTS_ENABLED, false) && !cursedaddons$isImageUrl(style)) {
            style = ClickEventsPreviewer.enrichStyleWithPreview(style);
            cir.setReturnValue(style);
        }
    }

    @Unique
    private boolean cursedaddons$isImageUrl(Style style) {
        if (!ImageHoverPreview.isEnabled()) return false;

        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent instanceof ClickEvent.OpenUrl openUrlEvent) {
            String url = openUrlEvent.uri().toString();
            if (ImageHoverPreview.isImageUrl(url) && ImageHoverPreview.isWhitelisted(url)) {
                return true;
            }
        }

        return false;
    }
}
