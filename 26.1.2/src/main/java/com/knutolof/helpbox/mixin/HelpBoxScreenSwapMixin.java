package com.knutolof.helpbox.mixin;

import com.knutolof.helpbox.storage.StorageInitializer;
import com.knutolof.helpbox.storage.config.EnhancedStorageConfig;
import com.knutolof.helpbox.storage.config.SackConfig;
import com.knutolof.helpbox.storage.gui.SackOverlayState;
import com.knutolof.helpbox.storage.screen.SackContainerScreen;
import com.knutolof.helpbox.storage.screen.StorageContainerScreen;
import com.knutolof.helpbox.storage.storage.SackCache;
import com.knutolof.helpbox.storage.storage.SackKey;
import com.knutolof.helpbox.storage.storage.StorageKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(value = Minecraft.class, priority = 500)
public abstract class HelpBoxScreenSwapMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, name = "screen")
    private Screen helpbox$swapScreen(Screen screen) {
        if (screen instanceof SackContainerScreen || screen instanceof StorageContainerScreen) {
            return screen;
        }

        ChestMenu chestMenu = null;
        Component title = null;
        if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> acs && acs.getMenu() instanceof ChestMenu cm) {
            chestMenu = cm;
            title = screen.getTitle();
        } else if (screen instanceof com.daqem.uilib.gui.AbstractContainerScreen<?> uacs && uacs.getMenu() instanceof ChestMenu cm) {
            chestMenu = cm;
            title = screen.getTitle();
        }

        if (chestMenu == null || title == null) return screen;

        // 1. Sacks check (HelpBox Sack Screen)
        Optional<SackKey> sackKey = SackKey.fromTitle(title);
        if (sackKey.isPresent()) {
            if (StorageInitializer.bypassNextSackOverlay) {
                StorageInitializer.bypassNextSackOverlay = false;
                return screen;
            }

            if (!SackConfig.enableSackOverlay) {
                return screen;
            }

            SackKey key = sackKey.get();
            Minecraft mc = Minecraft.getInstance();

            boolean isIndex = key.type() == SackKey.Type.SACK_INDEX;
            boolean isNavigating = SackOverlayState.session().isNavigating();

            if (isIndex) {
                // Opened Sack of Sacks menu directly (/sacks) -> Sack of Sacks is unlocked!
                SackCache.getInstance().setSackOfSacksUnlocked(true);
            } else if (!isNavigating) {
                // An individual sack was opened directly from inventory/hand (not by overlay navigation).
                // If Sack of Sacks is not unlocked on this profile, or sacks are carried in inventory,
                // open the original vanilla/Hypixel container screen!
                boolean hasSacksInInv = SackKey.hasSacksInInventory(mc);
                boolean sackOfSacksLocked = !SackCache.getInstance().isSackOfSacksUnlocked();
                boolean neverOpenedIndex = SackCache.getInstance().getIndexSnapshot().isEmpty();

                if (sackOfSacksLocked || (hasSacksInInv && (neverOpenedIndex || SackConfig.openOriginalIfSacksInInventory))) {
                    return screen;
                }
            }

            return new SackContainerScreen(
                    chestMenu,
                    mc.player.getInventory(),
                    title,
                    key);
        }

        // 2. Vault / Rift check (HelpBox Storage Screen)
        final ChestMenu finalChestMenu = chestMenu;
        final Component finalTitle = title;
        return StorageKey.fromTitle(title)
                .filter(key -> key.type() == StorageKey.Type.RIFT
                        ? EnhancedStorageConfig.enableRiftOverlay
                        : EnhancedStorageConfig.enableOverlay)
                .<Screen>map(key -> new StorageContainerScreen(
                        finalChestMenu,
                        Minecraft.getInstance().player.getInventory(),
                        finalTitle,
                        key))
                .orElse(screen);
    }
}
