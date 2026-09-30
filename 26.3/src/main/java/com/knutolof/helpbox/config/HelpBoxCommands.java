package com.knutolof.helpbox.config;

import com.knutolof.helpbox.HelpBoxMod;
import com.knutolof.helpbox.console.ConsoleScreen;
import com.knutolof.helpbox.navigation.ui.NavigationScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;

public class HelpBoxCommands {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                (LiteralArgumentBuilder<FabricClientCommandSource>)
                LiteralArgumentBuilder
                    .<FabricClientCommandSource>literal("gps")
                    .executes(context -> {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> mc.setScreenAndShow(new NavigationScreen()));
                        return 1;
                    })
            );
            dispatcher.register(
                (LiteralArgumentBuilder<FabricClientCommandSource>)
                LiteralArgumentBuilder
                    .<FabricClientCommandSource>literal("navigation")
                    .executes(context -> {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> mc.setScreenAndShow(new NavigationScreen()));
                        return 1;
                    })
            );
            dispatcher.register(
                (LiteralArgumentBuilder<FabricClientCommandSource>)
                LiteralArgumentBuilder
                    .<FabricClientCommandSource>literal("console")
                    .executes(context -> {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> mc.setScreenAndShow(new ConsoleScreen()));
                        return 1;
                    })
            );
            dispatcher.register(
                (LiteralArgumentBuilder<FabricClientCommandSource>)
                LiteralArgumentBuilder
                    .<FabricClientCommandSource>literal("helpbox")
                    .executes(context -> {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> mc.setScreenAndShow(new com.knutolof.helpbox.ui.HelpBoxControlCenterScreen()));
                        return 1;
                    })
            );
            dispatcher.register(
                (LiteralArgumentBuilder<FabricClientCommandSource>)
                LiteralArgumentBuilder
                    .<FabricClientCommandSource>literal("invbuttons")
                    .executes(context -> {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> {
                            com.knutolof.helpbox.inventory.buttons.ButtonStore.editMode = true;
                            com.knutolof.helpbox.inventory.buttons.ButtonStore.save();
                            if (mc.player != null) {
                                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.helpbox.edit_mode_enabled"));
                                mc.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                            }
                        });
                        return 1;
                    })
            );
        });

        HelpBoxMod.LOGGER.debug("[HelpBox] Commands registered.");
    }
}
