package com.knutolof.helpbox.storage;

import com.knutolof.helpbox.storage.config.EnhancedStorageConfig;
import com.knutolof.helpbox.storage.config.SackConfig;
import com.knutolof.helpbox.storage.gui.StorageOverlay;
import com.knutolof.helpbox.storage.screen.SackContainerScreen;
import com.knutolof.helpbox.storage.screen.StorageContainerScreen;
import com.knutolof.helpbox.storage.storage.*;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageInitializer {
    public static final String MOD_ID = "helpbox_vault";
    public static final String TEXTURE_NAMESPACE = "enhanced_storage";
    public static final String SACK_MOD_ID = "helpbox_sacks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean bypassNextSackOverlay = false;

    public static void init() {

        MidnightConfig.init(MOD_ID, EnhancedStorageConfig.class);
        MidnightConfig.init(SACK_MOD_ID, SackConfig.class);

        StorageProfile.getInstance().setBeforeChange(() -> {
            StorageCache.getInstance().saveToDisk();
            SackCache.getInstance().saveToDisk();
            StorageNames.getInstance().saveToDisk();
            StorageOrder.getInstance().saveToDisk();
        });

        StorageProfile.getInstance().setOnChange(() -> {
            StorageCache.getInstance().reloadForCurrentProfile();
            SackCache.getInstance().reloadForCurrentProfile();
            StorageNames.getInstance().reloadForCurrentProfile();
            StorageOrder.getInstance().reloadForCurrentProfile();

            // If a storage or sack overlay is currently open, rebuild it against the new data.
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (getCurrentScreen() instanceof StorageContainerScreen storageScreen) {
                    storageScreen.rebuildForProfileChange();
                }
                if (getCurrentScreen() instanceof SackContainerScreen sackScreen) {
                    sackScreen.rebuildForProfileChange();
                }
            });

            LOGGER.info("Storage profile changed to {}; caches reloaded.", StorageProfile.getInstance().current().orElse("default"));
        });

        StorageCaptureHandler.register();
        SackCaptureHandler.register();
        SackChatListener.register();
        HypixelApiSync.start();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("teststorage")
                    .executes(context -> {
                        Minecraft.getInstance().schedule(() -> {
                            Minecraft.getInstance().setScreenAndShow(new StorageOverlay());
                        });
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("sax")
                    .executes(context -> {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> {
                            StorageInitializer.bypassNextSackOverlay = true;
                            if (mc.player != null && mc.player.connection != null) {
                                mc.player.connection.sendCommand("sacks");
                            }
                        });
                        return 1;
                    }));
        });

        LOGGER.info("Enhanced Storage initialized.");
    }

    public static net.minecraft.client.gui.screens.Screen getCurrentScreen() {
        Minecraft mc = Minecraft.getInstance();
        try {
            var m = mc.gui.getClass().getMethod("screen");
            return (net.minecraft.client.gui.screens.Screen) m.invoke(mc.gui);
        } catch (Throwable ignored) {
            try {
                var f = Minecraft.class.getDeclaredField("screen");
                f.setAccessible(true);
                return (net.minecraft.client.gui.screens.Screen) f.get(mc);
            } catch (Throwable t) {
                return null;
            }
        }
    }
}
