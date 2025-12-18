package dev.cursedatom.chathotkey;

import dev.cursedatom.chathotkey.command.CommandRegistry;
import dev.cursedatom.chathotkey.config.ConfigScreenGenerator;
import dev.cursedatom.chathotkey.features.chatkeybindings.Macro;
import dev.cursedatom.chathotkey.utils.ConfigUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ChatHotkeyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ConfigUtils.init();
		CommandRegistry.register();
		
		ClientTickEvents.START_WORLD_TICK.register(client -> {
		    Object enabled = ConfigUtils.get("chatkeybindings.Macro.Enabled");
            if (enabled != null && (boolean) enabled) {
                Macro.tick();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (CommandRegistry.shouldOpenConfigScreen) {
                Screen currentScreen = Minecraft.getInstance().screen;
                if (currentScreen instanceof net.minecraft.client.gui.screens.ChatScreen) {
                    Minecraft.getInstance().setScreen(null); // Close the chat screen if it's still open
                }
                Minecraft.getInstance().setScreen(ConfigScreenGenerator.getConfigBuilder().setParentScreen(currentScreen).build());
                CommandRegistry.shouldOpenConfigScreen = false; // Reset the flag
            }
        });
	}
}
