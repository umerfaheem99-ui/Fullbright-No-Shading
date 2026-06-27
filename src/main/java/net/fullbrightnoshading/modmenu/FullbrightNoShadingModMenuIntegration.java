package net.fullbrightnoshading.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fullbrightnoshading.config.FullbrightNoShadingConfigScreen;

public class FullbrightNoShadingModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FullbrightNoShadingConfigScreen::create;
    }
}
