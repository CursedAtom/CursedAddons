package dev.cursedatom.cursedaddons;

import dev.cursedatom.cursedaddons.command.CommandRegistry;
import dev.cursedatom.cursedaddons.config.ConfigScreenGenerator;
import dev.cursedatom.cursedaddons.features.chatkeybindings.Macro;
import dev.cursedatom.cursedaddons.features.doublechatfix.DoubleChatFix;
import dev.cursedatom.cursedaddons.utils.ConfigUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Main client-side initializer for CursedAddons mod.
 * Handles initialization of all client-side features and event registration.
 */
public class CursedAddonsClient implements ClientModInitializer {
	private static boolean macroEnabled = false;

	private static void updateCachedConfig() {
		Object enabled = ConfigUtils.get("chatkeybindings.Macro.Enabled");
		macroEnabled = enabled != null && (boolean) enabled;
	}

	@Override
	public void onInitializeClient() {
		ConfigUtils.init();
		CommandRegistry.register();
        DoubleChatFix.init();

        // Cache initial config state
        updateCachedConfig();

		ClientTickEvents.START_WORLD_TICK.register(client -> {
            if (macroEnabled) {
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
