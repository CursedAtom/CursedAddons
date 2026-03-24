package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.commandaliases.AliasHandler;
import dev.cursedatom.cursedaddons.features.general.ChatMessageCopier;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for ChatScreen to handle command aliases and copy chat messages.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @ModifyVariable(
        method = "handleChatInput",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String modifyChatMessage(String message) {
        return AliasHandler.processMessage(message);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void cursedaddons$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == 1 && ChatMessageCopier.isEnabled()) {
            if (ChatMessageCopier.tryCopyMessageAt(event.x(), event.y())) {
                cir.setReturnValue(true);
            }
        }
    }
}
