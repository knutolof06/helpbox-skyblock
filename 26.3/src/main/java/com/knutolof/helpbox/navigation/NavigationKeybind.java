package com.knutolof.helpbox.navigation;

import com.knutolof.helpbox.HelpBoxMod;
import com.knutolof.helpbox.navigation.ui.NavigationScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class NavigationKeybind {
    
    // Default keybind: 'G' (GLFW_KEY_G = 71)
    public static final KeyMapping openGpsScreen = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.helpbox.open_gps",
                    InputConstants.KEY_G,
                    com.knutolof.helpbox.config.HelpBoxConfig.CATEGORY
            )
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGpsScreen.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.setScreenAndShow(new NavigationScreen());
                }
            }
        });
        
        HelpBoxMod.LOGGER.debug("[HelpBox] NavigationKeybind registered.");
    }
}
