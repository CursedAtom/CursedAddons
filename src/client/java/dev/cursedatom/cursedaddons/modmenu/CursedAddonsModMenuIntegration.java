package dev.cursedatom.cursedaddons.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.cursedatom.cursedaddons.config.ConfigScreenGenerator;

public class CursedAddonsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ConfigScreenGenerator.getConfigBuilder().setParentScreen(parent).build();
    }
}
