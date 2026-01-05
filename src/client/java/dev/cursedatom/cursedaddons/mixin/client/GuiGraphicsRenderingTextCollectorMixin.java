package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.general.ClickEventsPreviewer;
import dev.cursedatom.cursedaddons.utils.ConfigUtils;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * This mixin is used to intercept the chat styling and check if a click action 
 * will be executed. 
 * @see ClickEventsPreviewer 
 */
@Mixin(targets = "net.minecraft.client.gui.GuiGraphics$RenderingTextCollector")
public abstract class GuiGraphicsRenderingTextCollectorMixin {

    @ModifyVariable(method = "accept(Lnet/minecraft/network/chat/Style;)V", at = @At("HEAD"), argsOnly = true)
    public Style modifyStyle(Style style) {
        if (style == null || (style.getClickEvent() == null && style.getInsertion() == null)) {
            return style;
        }

        Object enabledObj = ConfigUtils.get("general.PreviewClickEvents.Enabled");
        if (enabledObj == null || (boolean) enabledObj) {
            return ClickEventsPreviewer.work(style);
        }
        return style;
}
}
