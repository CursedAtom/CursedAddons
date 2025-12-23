package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.doublechatfix.callback.GLFWPollCallback;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTick(boolean render, CallbackInfo ci) {
        GLFWPollCallback.EVENT.invoker().onPoll();
    }
}
