package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.doublechatfix.callback.KeyboardCharTypedCallback;
import dev.cursedatom.cursedaddons.features.doublechatfix.callback.KeyboardKeyPressedCallback;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyHead(long window, int key, KeyEvent keyEvent, CallbackInfo ci) {
        InteractionResult result = KeyboardKeyPressedCallback.EVENT.invoker().onKeyPressed(window, key, keyEvent);

        if (result == InteractionResult.FAIL)
            ci.cancel();
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharHead(long window, CharacterEvent characterEvent, CallbackInfo ci) {
        InteractionResult result = KeyboardCharTypedCallback.EVENT.invoker().onCharTyped(window, characterEvent);

        if (result == InteractionResult.FAIL)
            ci.cancel();
    }
}
