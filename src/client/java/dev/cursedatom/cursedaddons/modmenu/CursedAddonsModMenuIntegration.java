package dev.cursedatom.cursedaddons.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.cursedatom.cursedaddons.config.ConfigScreen;

/**
 * Integrates CursedAddons with the ModMenu mod, providing the config screen factory.
 */
public class CursedAddonsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
