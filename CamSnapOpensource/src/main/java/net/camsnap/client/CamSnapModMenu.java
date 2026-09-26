package net.camsnap.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu integration for CamSnap.
 * Exposes the Cloth Config GUI via the "Configure" button in ModMenu.
 * Target: Minecraft 26.3 / Fabric Client / Java 25.
 */
public final class CamSnapModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CamSnapConfig::createClothConfigScreen;
    }
}
