package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.general.ClickEventsPreviewer;
import dev.cursedatom.cursedaddons.features.images.ImageHoverPreview;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(method = "getClickedComponentStyleAt", at = @At(value = "RETURN"), cancellable = true)
    public void modifyHoverEvent(double x, double y, CallbackInfoReturnable<Style> cir) {
        Style style = cir.getReturnValue();
        if (style == null) return;

        // Handle click event previews (skip for image URLs — DrawContextMixin handles those)
        Object enabledObj = ConfigProvider.get("general.PreviewClickEvents.Enabled");
        if (enabledObj != null && (boolean) enabledObj && !cursedaddons$isImageUrl(style)) {
            style = ClickEventsPreviewer.work(style);
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
