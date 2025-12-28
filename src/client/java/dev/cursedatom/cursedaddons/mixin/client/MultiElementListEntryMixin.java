package dev.cursedatom.cursedaddons.mixin.client;

import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiElementListEntry.class)
public abstract class MultiElementListEntryMixin<T> {

    @Shadow(remap = false)
    @Final
    private MultiElementListEntry<T>.CategoryLabelWidget widget;

    /**
     * @author cursedatom
     * @reason Propagate mouse events to label widget (necessary for label click to toggle expand/collapse)
     */
    @Inject(method = "mouseClicked", at = @At("RETURN"), cancellable = true, remap = false)
    private void propagateMouseEvents(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            if (widget.mouseClicked(event, doubleClick)) {
                cir.setReturnValue(true);
            }
        }
    }
}