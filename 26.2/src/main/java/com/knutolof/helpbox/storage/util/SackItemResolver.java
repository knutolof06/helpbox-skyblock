package com.knutolof.helpbox.storage.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SackItemResolver {

    public static ItemStack resolveByNameOrId(String nameOrId) {
        if (nameOrId == null || nameOrId.isBlank()) return new ItemStack(Items.PAPER);
        String clean = nameOrId.replaceAll("§[0-9a-fk-or]", "").trim();
        String idKey = clean.toUpperCase().replace(" ", "_").replace("ENCHANTED_", "");

        for (Item item : BuiltInRegistries.ITEM) {
            String path = BuiltInRegistries.ITEM.getKey(item).getPath().toUpperCase();
            if (path.equals(idKey) || path.replace("_", "").equals(idKey.replace("_", ""))) {
                ItemStack stack = new ItemStack(item);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal(clean));
                return stack;
            }
        }

        Item fallback = Items.PAPER;
        if (idKey.contains("SUGAR_CANE")) fallback = Items.SUGAR_CANE;
        else if (idKey.contains("CARROT")) fallback = Items.CARROT;
        else if (idKey.contains("POTATO")) fallback = Items.POTATO;
        else if (idKey.contains("WHEAT") || idKey.contains("HAY")) fallback = Items.HAY_BLOCK;
        else if (idKey.contains("PUMPKIN")) fallback = Items.PUMPKIN;
        else if (idKey.contains("MELON")) fallback = Items.MELON;
        else if (idKey.contains("PORK")) fallback = Items.PORKCHOP;
        else if (idKey.contains("BEEF") || idKey.contains("STEAK")) fallback = Items.BEEF;
        else if (idKey.contains("CHICKEN")) fallback = Items.CHICKEN;
        else if (idKey.contains("FEATHER")) fallback = Items.FEATHER;
        else if (idKey.contains("LEATHER")) fallback = Items.LEATHER;
        else if (idKey.contains("BONE")) fallback = Items.BONE;
        else if (idKey.contains("STRING")) fallback = Items.STRING;
        else if (idKey.contains("SPIDER")) fallback = Items.SPIDER_EYE;
        else if (idKey.contains("SLIME")) fallback = Items.SLIME_BALL;
        else if (idKey.contains("MAGMA")) fallback = Items.MAGMA_CREAM;
        else if (idKey.contains("PEARL") || idKey.contains("ENDER")) fallback = Items.ENDER_PEARL;
        else if (idKey.contains("BLAZE")) fallback = Items.BLAZE_ROD;
        else if (idKey.contains("COAL")) fallback = Items.COAL;
        else if (idKey.contains("IRON")) fallback = Items.IRON_INGOT;
        else if (idKey.contains("GOLD")) fallback = Items.GOLD_INGOT;
        else if (idKey.contains("DIAMOND")) fallback = Items.DIAMOND;
        else if (idKey.contains("EMERALD")) fallback = Items.EMERALD;
        else if (idKey.contains("GEMSTONE") || idKey.contains("RUBY") || idKey.contains("SAPPHIRE")) fallback = Items.AMETHYST_SHARD;

        ItemStack stack = new ItemStack(fallback);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(clean));
        return stack;
    }
}
