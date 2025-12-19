package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.general.ClickEventsPreviewer;
import dev.cursedatom.cursedaddons.utils.ConfigUtils;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(method = "getClickedComponentStyleAt", at = @At(value = "RETURN"), cancellable = true)
    public void modifyHoverEvent(double x, double y, CallbackInfoReturnable<Style> cir) {
        Style style = cir.getReturnValue();
        if (style == null) return;

        Object enabledObj = ConfigUtils.get("general.PreviewClickEvents.Enabled");
        if (enabledObj != null && (boolean) enabledObj) {
            cir.setReturnValue(ClickEventsPreviewer.work(style));
        }
    }
}
