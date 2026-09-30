package com.knutolof.helpbox.config;

import com.knutolof.helpbox.HelpBoxMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class HelpBoxConfig {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(HelpBoxMod.MOD_ID, "helpbox")
    );

    public static KeyMapping copyItemName;
    public static KeyMapping copyItemSkull;
    public static KeyMapping openConsole;
    public static KeyMapping openControlCenter;

    public static void register() {
        copyItemName = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.helpbox.copy_item_name",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
        ));

        copyItemSkull = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.helpbox.copy_item_skull",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
        ));

        openConsole = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.helpbox.open_console",
            InputConstants.KEY_GRAVE,
            CATEGORY
        ));

        openControlCenter = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.helpbox.open_control_center",
            InputConstants.KEY_RSHIFT,
            CATEGORY
        ));

        HelpBoxMod.LOGGER.debug("[HelpBox] KeyMappings registered.");
    }

    private HelpBoxConfig() {}
}
