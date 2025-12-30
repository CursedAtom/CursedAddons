package dev.cursedatom.cursedaddons.mixin.client;

import dev.cursedatom.cursedaddons.features.chatnotifications.ChatNotifications;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleSystemChat", at = @At("TAIL"))
    public void onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        Component message = packet.content();
        ChatNotifications.checkMessage(message);
    }

    @Inject(method = "handlePlayerChat", at = @At("TAIL"))
    public void onPlayerChat(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
        Component message = packet.unsignedContent();
        ChatNotifications.checkMessage(message);
    }

    @Inject(method = "handleDisguisedChat", at = @At("TAIL"))
    public void onDisguisedChat(ClientboundDisguisedChatPacket packet, CallbackInfo ci) {
        Component message = packet.message();
        ChatNotifications.checkMessage(message);
    }
}
