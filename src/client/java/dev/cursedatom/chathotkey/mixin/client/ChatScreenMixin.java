package dev.cursedatom.chathotkey.mixin.client;

import dev.cursedatom.chathotkey.features.commandaliases.AliasHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
