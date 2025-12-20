package dev.cursedatom.cursedaddons.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.cursedatom.cursedaddons.features.doublechatfix.GLFWPollCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @Inject(method = "pollEvents",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwPollEvents()V", shift = At.Shift.AFTER),
            remap = false)
    private static void injectGlfwPoll(CallbackInfo ci) {
        GLFWPollCallback.EVENT.invoker().onPoll();
    }
}
