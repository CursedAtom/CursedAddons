package dev.cursedatom.cursedaddons;

import dev.cursedatom.cursedaddons.command.CommandRegistry;
import dev.cursedatom.cursedaddons.config.ConfigScreenGenerator;
import dev.cursedatom.cursedaddons.config.utils.GenericEditScreen;
import dev.cursedatom.cursedaddons.features.chatkeybindings.Macro;
import dev.cursedatom.cursedaddons.features.doublechatfix.DoubleChatFix;
import dev.cursedatom.cursedaddons.features.images.ImageHoverPreview;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main client-side initializer for CursedAddons mod.
 * Handles initialization of all client-side features and event registration.
 */
public class CursedAddonsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ConfigProvider.init();
		CommandRegistry.register();
		DoubleChatFix.init();
		ImageHoverPreview.init();

		GenericEditScreen.registerDropdownProvider("sound_event", () -> {
			List<String> sounds = new ArrayList<>();
			for (ResourceLocation id : BuiltInRegistries.SOUND_EVENT.keySet()) {
				sounds.add(id.toString());
			}
			Collections.sort(sounds);
			return sounds;
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (Macro.isEnabled()) {
				Macro.tick();
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (CommandRegistry.shouldOpenConfigScreen) {
				Screen currentScreen = Minecraft.getInstance().screen;
				if (currentScreen instanceof net.minecraft.client.gui.screens.ChatScreen) {
					Minecraft.getInstance().setScreen(null);
				}
				Minecraft.getInstance().setScreen(ConfigScreenGenerator.getConfigScreen(currentScreen));
				CommandRegistry.shouldOpenConfigScreen = false;
			}
		});
	}
}
