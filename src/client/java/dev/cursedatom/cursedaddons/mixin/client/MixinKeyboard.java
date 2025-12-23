package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.doublechatfix.MC122477FixUtil;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyHead(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        MC122477FixUtil.onKeyPress(window, key, scancode, action, mods);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharHead(long window, int codepoint, int mods, CallbackInfo ci) {
        if (MC122477FixUtil.shouldCancelChar()) {
            ci.cancel();
        }
    }
}
