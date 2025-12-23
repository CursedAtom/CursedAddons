package dev.cursedatom.cursedaddons.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.cursedatom.cursedaddons.features.doublechatfix.MC122477FixUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    @Inject(method = "flipFrame", at = @At("HEAD"))
    private static void onFlipFrame(long window, CallbackInfo ci) {
        MC122477FixUtil.incrementPollCount();
    }
}