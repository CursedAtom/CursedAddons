package dev.cursedatom.chathotkey.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.cursedatom.chathotkey.config.ConfigScreenGenerator;
import net.minecraft.client.gui.screens.Screen;

public class ChatHotkeyModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ConfigScreenGenerator.getConfigBuilder().setParentScreen(parent).build();
    }
}
