package com.knutolof.helpbox;
import com.knutolof.helpbox.storage.StorageInitializer;

import com.knutolof.helpbox.config.HelpBoxConfig;
import com.knutolof.helpbox.config.HelpBoxCommands;
import com.knutolof.helpbox.features.ItemNameCopier;
import com.knutolof.helpbox.navigation.NavigationKeybind;
import com.knutolof.helpbox.navigation.WaypointManager;
import com.knutolof.helpbox.navigation.render.GpsHudOverlay;
import com.knutolof.helpbox.navigation.render.WaypointWorldRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class HelpBoxMod implements ClientModInitializer {

    public static final String MOD_ID = "helpbox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[HelpBox] Initializing Hypixel HelpBox...");

        // 1. Waypoint Manager & Button Store
        WaypointManager.load();
        com.knutolof.helpbox.inventory.buttons.ButtonStore.load();

        // 2. Keybinds & Commands
        HelpBoxConfig.register();
        NavigationKeybind.register();
        HelpBoxCommands.register();

        // 3. Features
        ItemNameCopier.register();
        com.knutolof.helpbox.console.ConsoleHistory.register();

        // 4. Renderers & HUD
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "gps_hud"), new GpsHudOverlay());
        LevelRenderEvents.BEFORE_GIZMOS.register(WaypointWorldRenderer::render);

        // 5. Tick Events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WaypointManager.tick();
            while (HelpBoxConfig.openConsole != null && HelpBoxConfig.openConsole.consumeClick()) {
                client.setScreenAndShow(new com.knutolof.helpbox.console.ConsoleScreen());
            }
            while (HelpBoxConfig.openControlCenter != null && HelpBoxConfig.openControlCenter.consumeClick()) {
                client.setScreenAndShow(new com.knutolof.helpbox.ui.HelpBoxControlCenterScreen());
            }
        });

        // 6. HelpBox Vault (Storage) & Sacks
        try {
            com.knutolof.helpbox.storage.StorageInitializer.init();
        } catch (Exception e) {
            LOGGER.error("[HelpBox] Failed to initialize HelpBox Vault / Sacks", e);
        }

        LOGGER.info("[HelpBox] Hypixel HelpBox initialized successfully.");
    }
}
