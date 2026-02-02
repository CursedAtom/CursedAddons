package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.commandaliases.AliasHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin for ChatScreen to handle command aliases.
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
}
