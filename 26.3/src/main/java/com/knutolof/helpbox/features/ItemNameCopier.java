package com.knutolof.helpbox.features;

import com.knutolof.helpbox.HelpBoxMod;
import com.knutolof.helpbox.config.HelpBoxConfig;
import com.knutolof.helpbox.inventory.buttons.ButtonStore;
import com.knutolof.helpbox.inventory.buttons.PopupEditor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

/**
 * Özellik: Envanter Eşyası & Dünyadaki Blok/Kafaları Kopyalama
 *
 * <p>Bu sınıf, oyuncu container GUI'lerinde (envanter, sandık vb.) veya
 * oyunda dünyadaki blok ve özel kafa (skull) nesnelerine baktığında,
 * tuş ataması ile blok/eşya ismini veya skull doku kodunu panoya kopyalar.</p>
 */
public final class ItemNameCopier {

    private static final String FORMATTING_CODE_PATTERN = "§[0-9a-fk-orA-FK-OR]";
    private static final Field HOVERED_SLOT_FIELD;
    private static final MethodHandle SCREEN_GETTER;
    private static final MethodHandle PROFILE_NAME_GETTER;

    static {
        Field field = null;
        try {
            field = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            HelpBoxMod.LOGGER.error(
                "[HelpBox] Could not find 'hoveredSlot' field in AbstractContainerScreen. " +
                "The Copy Item Name feature in GUI will be disabled.", e
            );
        }
        HOVERED_SLOT_FIELD = field;

        MethodHandle screenHandle = null;
        try {
            Method m = net.minecraft.client.gui.Gui.class.getMethod("screen");
            screenHandle = MethodHandles.lookup().unreflect(m);
        } catch (Throwable t1) {
            try {
                Field f = Minecraft.class.getField("screen");
                screenHandle = MethodHandles.lookup().unreflectGetter(f);
            } catch (Throwable ignored) {}
        }
        SCREEN_GETTER = screenHandle;

        MethodHandle nameHandle = null;
        try {
            Method m = com.mojang.authlib.GameProfile.class.getMethod("name");
            nameHandle = MethodHandles.lookup().unreflect(m);
        } catch (Throwable t1) {
            try {
                Method m = com.mojang.authlib.GameProfile.class.getMethod("getName");
                nameHandle = MethodHandles.lookup().unreflect(m);
            } catch (Throwable ignored) {}
        }
        PROFILE_NAME_GETTER = nameHandle;
    }

    public static boolean isScreenOpen(Minecraft client) {
        if (client == null) return false;
        if (SCREEN_GETTER != null) {
            try {
                Object target = (SCREEN_GETTER.type().parameterType(0) == Minecraft.class) ? client : client.gui;
                if (target != null) {
                    return SCREEN_GETTER.invoke(target) != null;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static String getProfileName(com.mojang.authlib.GameProfile profile) {
        if (profile == null || PROFILE_NAME_GETTER == null) return null;
        try {
            return (String) PROFILE_NAME_GETTER.invoke(profile);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void register() {
        // 1. Ekran açıkken (GUI / Envanter / Sandık)
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                // Klavye tuşlarına basıldığında
                ScreenKeyboardEvents.allowKeyPress(screen).register((scr, event) -> {
                    if (HelpBoxConfig.copyItemName.matches(event)) {
                        boolean handled = handleCopy(client, containerScreen, 0);
                        return !handled;
                    }
                    if (HelpBoxConfig.copyItemSkull.matches(event)) {
                        boolean handled = handleCopy(client, containerScreen, 1);
                        return !handled;
                    }
                    return true;
                });

                // Fare tuşlarına basıldığında (Keybind fareye atandıysa)
                ScreenMouseEvents.allowMouseClick(screen).register((scr, event) -> {
                    if (HelpBoxConfig.copyItemName.matchesMouse(event)) {
                        boolean handled = handleCopy(client, containerScreen, 0);
                        return !handled;
                    }
                    if (HelpBoxConfig.copyItemSkull.matchesMouse(event)) {
                        boolean handled = handleCopy(client, containerScreen, 1);
                        return !handled;
                    }
                    return true;
                });
            }
        });

        // 2. Oyundayken (GUI kapalıyken) baktığımız blok veya kafaları kopyalama
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (isScreenOpen(client)) return;

            while (HelpBoxConfig.copyItemName != null && HelpBoxConfig.copyItemName.consumeClick()) {
                handleWorldCopy(client, 0);
            }
            while (HelpBoxConfig.copyItemSkull != null && HelpBoxConfig.copyItemSkull.consumeClick()) {
                handleWorldCopy(client, 1);
            }
        });

        HelpBoxMod.LOGGER.debug("[HelpBox] ItemNameCopier registered.");
    }

    /**
     * GUI içindeyken hover edilen eşyayı kopyalar.
     * @param mode 0 = İsim kopyala, 1 = İkon/Skull kodu kopyala
     */
    private static boolean handleCopy(Minecraft client, AbstractContainerScreen<?> screen, int mode) {
        Slot hoveredSlot = getHoveredSlot(screen);

        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return false;
        }

        ItemStack stack = hoveredSlot.getItem();
        String resultToCopy;
        String displayMsg;

        if (mode == 1) { // İkon / Skull kopyalama
            if (stack.is(Items.PLAYER_HEAD)) {
                String hash = getSkullHash(stack);
                if (hash == null || hash.isEmpty()) {
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.translatable("message.helpbox.no_skull"));
                    }
                    return true;
                }
                resultToCopy = "skull:" + hash;
                displayMsg = resultToCopy;
            } else {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                // SkyBlock ID'sini ExtraAttributes.id'den çek
                String skyId = getSkyBlockId(stack);
                if (skyId != null) {
                    resultToCopy = id + "[sky:" + skyId + "]";
                    displayMsg = id + "[sky:" + skyId + "]";
                } else {
                    resultToCopy = id;
                    displayMsg = id;
                }
            }
            // Tam ItemStack'i pending cache'e koy — buton kaydedilince kullanılacak
            ButtonStore.iconPendingStack = stack.copy();
            // iconStr'yi pending'e al — PopupEditor veya ButtonEditorScreen varsa otomatik doldur
            ButtonStore.pendingIconStr = resultToCopy;
            PopupEditor.setIconValue(resultToCopy);
        } else { // İsim kopyalama (mode == 0)
            String rawName = stack.getHoverName().getString();
            resultToCopy = stripFormattingCodes(rawName);
            displayMsg = resultToCopy;
            if (resultToCopy.isBlank()) {
                return false;
            }
        }

        client.keyboardHandler.setClipboard(resultToCopy);

        if (client.player != null) {
            if (mode == 1) {
                client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_icon", displayMsg));
            } else {
                client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_item", displayMsg));
            }
        }

        HelpBoxMod.LOGGER.debug("[HelpBox] Copied item info: '{}'", resultToCopy);
        return true;
    }

    /**
     * Oyundayken (dünyadayken) baktığımız blok veya kafaları kopyalar.
     * @param mode 0 = Blok/kafa adı kopyala, 1 = İkon/Skull kodu kopyala
     */
    private static boolean handleWorldCopy(Minecraft client, int mode) {
        if (client.player == null || client.level == null) return false;

        HitResult hit = client.hitResult;
        // Eğer standart erişim mesafesinde hedef yoksa veya MISS ise, 20 blokluk uzatılmış ışın ile tara
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            Vec3 eyePos = client.player.getEyePosition(1.0f);
            Vec3 viewVector = client.player.getViewVector(1.0f);
            Vec3 reachVec = eyePos.add(viewVector.x * 20.0, viewVector.y * 20.0, viewVector.z * 20.0);
            hit = client.level.clip(new ClipContext(
                    eyePos,
                    reachVec,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    client.player
            ));
        }

        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            BlockEntity be = client.level.getBlockEntity(pos);

            if (be instanceof SkullBlockEntity skullEntity) {
                ResolvableProfile profile = skullEntity.getOwnerProfile();

                if (mode == 1) { // Kafa dokusu (skull:hash) kopyalama
                    String hash = getSkullHashFromProfile(profile);
                    if (hash == null || hash.isEmpty()) {
                        client.player.sendSystemMessage(Component.translatable("message.helpbox.no_skull"));
                        return true;
                    }
                    String resultToCopy = "skull:" + hash;
                    ItemStack headStack = Items.PLAYER_HEAD.getDefaultInstance();
                    if (profile != null) {
                        headStack.set(DataComponents.PROFILE, profile);
                    }
                    ButtonStore.iconPendingStack = headStack;
                    ButtonStore.pendingIconStr = resultToCopy;
                    PopupEditor.setIconValue(resultToCopy);

                    client.keyboardHandler.setClipboard(resultToCopy);
                    client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_skull", resultToCopy));
                    return true;
                } else { // Kafa sahibi / Blok adı kopyalama
                    String name = null;
                    if (profile != null && profile.partialProfile() != null) {
                        String pName = getProfileName(profile.partialProfile());
                        if (pName != null && !pName.isBlank()) {
                            name = pName;
                        }
                    }
                    if (name == null || name.isBlank()) {
                        name = state.getBlock().getName().getString();
                    }
                    String cleanName = stripFormattingCodes(name);
                    client.keyboardHandler.setClipboard(cleanName);
                    client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_block", cleanName));
                    return true;
                }
            } else {
                // Normal Blok
                if (mode == 1) { // İkon kopyalama
                    Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    String iconStr = (blockId != null) ? blockId.toString() : "minecraft:stone";
                    ItemStack itemStack = state.getBlock().asItem().getDefaultInstance();
                    if (!itemStack.isEmpty()) {
                        ButtonStore.iconPendingStack = itemStack;
                    }
                    ButtonStore.pendingIconStr = iconStr;
                    PopupEditor.setIconValue(iconStr);

                    client.keyboardHandler.setClipboard(iconStr);
                    client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_icon", iconStr));
                    return true;
                } else { // Blok adı kopyalama
                    String name = stripFormattingCodes(state.getBlock().getName().getString());
                    client.keyboardHandler.setClipboard(name);
                    client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_block", name));
                    return true;
                }
            }
        } else if (hit instanceof EntityHitResult ehr && hit.getType() == HitResult.Type.ENTITY) {
            net.minecraft.world.entity.Entity entity = ehr.getEntity();
            if (entity instanceof ItemFrame itemFrame && !itemFrame.getItem().isEmpty()) {
                ItemStack stack = itemFrame.getItem();
                if (mode == 1) {
                    if (stack.is(Items.PLAYER_HEAD)) {
                        String hash = getSkullHash(stack);
                        if (hash != null && !hash.isEmpty()) {
                            String resultToCopy = "skull:" + hash;
                            ButtonStore.iconPendingStack = stack.copy();
                            ButtonStore.pendingIconStr = resultToCopy;
                            PopupEditor.setIconValue(resultToCopy);
                            client.keyboardHandler.setClipboard(resultToCopy);
                            client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_skull", resultToCopy));
                            return true;
                        }
                    }
                    Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    String idStr = (id != null) ? id.toString() : "minecraft:air";
                    ButtonStore.iconPendingStack = stack.copy();
                    ButtonStore.pendingIconStr = idStr;
                    PopupEditor.setIconValue(idStr);
                    client.keyboardHandler.setClipboard(idStr);
                    client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_icon", idStr));
                    return true;
                } else {
                    String name = stripFormattingCodes(stack.getHoverName().getString());
                    client.keyboardHandler.setClipboard(name);
                    client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_item", name));
                    return true;
                }
            } else if (entity instanceof LivingEntity living) {
                ItemStack headItem = living.getItemBySlot(EquipmentSlot.HEAD);
                if (mode == 1 && headItem.is(Items.PLAYER_HEAD)) {
                    String hash = getSkullHash(headItem);
                    if (hash != null && !hash.isEmpty()) {
                        String resultToCopy = "skull:" + hash;
                        ButtonStore.iconPendingStack = headItem.copy();
                        ButtonStore.pendingIconStr = resultToCopy;
                        PopupEditor.setIconValue(resultToCopy);
                        client.keyboardHandler.setClipboard(resultToCopy);
                        client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_skull", resultToCopy));
                        return true;
                    }
                }
                if (mode == 0) {
                    String name = stripFormattingCodes(entity.getName().getString());
                    client.keyboardHandler.setClipboard(name);
                    client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_block", name));
                    return true;
                }
            } else if (entity != null && mode == 0) {
                String name = stripFormattingCodes(entity.getName().getString());
                client.keyboardHandler.setClipboard(name);
                client.player.sendSystemMessage(Component.translatable("message.helpbox.copied_block", name));
                return true;
            }
        }

        // Baktığı yerde blok/hedef yoksa bildirim ver
        client.player.sendSystemMessage(Component.translatable("message.helpbox.no_block_looked"));
        return false;
    }

    public static String getSkullHash(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) return null;
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        return getSkullHashFromProfile(profile);
    }

    public static String getSkullHashFromProfile(ResolvableProfile profile) {
        if (profile == null || profile.partialProfile() == null) return null;
        
        com.mojang.authlib.GameProfile gameProfile = profile.partialProfile();
        Collection<com.mojang.authlib.properties.Property> textures = gameProfile.properties().get("textures");
        if (textures == null) return null;

        for (com.mojang.authlib.properties.Property prop : textures) {
            String base64 = prop.value();
            if (base64 != null) {
                try {
                    String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                    int urlIdx = json.indexOf("\"url\"");
                    if (urlIdx > 0) {
                        int startUrl = json.indexOf("\"", urlIdx + 5) + 1;
                        int endUrl = json.indexOf("\"", startUrl);
                        if (startUrl > 0 && endUrl > startUrl) {
                            String url = json.substring(startUrl, endUrl);
                            int lastSlash = url.lastIndexOf('/');
                            if (lastSlash >= 0 && lastSlash + 1 < url.length()) {
                                String hash = url.substring(lastSlash + 1).trim();
                                if (hash.endsWith("\"")) hash = hash.substring(0, hash.length() - 1);
                                return hash;
                            }
                        }
                    }
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private static Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
        if (HOVERED_SLOT_FIELD == null) {
            return null;
        }
        try {
            return (Slot) HOVERED_SLOT_FIELD.get(screen);
        } catch (IllegalAccessException e) {
            HelpBoxMod.LOGGER.error("[HelpBox] Failed to access hoveredSlot via reflection", e);
            return null;
        }
    }

    private static String stripFormattingCodes(String text) {
        return text.replaceAll(FORMATTING_CODE_PATTERN, "").trim();
    }

    /** SkyBlock eşyasının ID'sini bulur. Root id ve ExtraAttributes.id dener. */
    private static String getSkyBlockId(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) return null;
        try {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();

            // 1. Root seviyesinde id (yeni Hypixel format)
            String rootId = tag.getString("id").orElse(null);
            if (rootId != null && !rootId.isEmpty()) return rootId;

            // 2. ExtraAttributes.id (eski format)
            if (tag.contains("ExtraAttributes")) {
                net.minecraft.nbt.CompoundTag extra = tag.getCompoundOrEmpty("ExtraAttributes");
                String id = extra.getString("id").orElse(null);
                if (id != null && !id.isEmpty()) return id;
            }

            // 3. Alt compound'ları tara (fallback)
            for (String key : tag.keySet()) {
                net.minecraft.nbt.Tag child = tag.get(key);
                if (child instanceof net.minecraft.nbt.CompoundTag compound) {
                    String id = compound.getString("id").orElse(null);
                    if (id != null && !id.isEmpty() && id.equals(id.toUpperCase())) return id;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private ItemNameCopier() {}
}
