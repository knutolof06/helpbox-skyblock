package com.knutolof.helpbox.storage.storage;
import com.knutolof.helpbox.storage.StorageInitializer;


import com.knutolof.helpbox.storage.compat.CatharsisCompat;
import com.knutolof.helpbox.storage.screen.StorageContainerScreen;
import com.knutolof.helpbox.storage.util.TextUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Watches container screens; when a screen belonging to a storage page is
 * open, it snapshots the container's contents every tick and writes the
 * final state into {@link StorageCache}.
 */
public final class StorageCaptureHandler {

    private static final int PLAYER_SLOT_COUNT = 36;
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("profile id:\\s*([0-9a-f-]{36})");
    private static final int INDEX_STABLE_TICKS = 10;

    private StorageCaptureHandler() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((Minecraft client, Screen screen, int scaledWidth, int scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            Optional<StorageKey> keyOpt = StorageKey.fromTitle(containerScreen.getTitle());
            if (keyOpt.isEmpty()) return;
            StorageKey key = keyOpt.get();

            if (key.type() == StorageKey.Type.RIFT) {
                discoverRiftPages(containerScreen.getTitle());
            }

            IndexPruneState pruneState =
                    key.type() == StorageKey.Type.STORAGE_INDEX ? new IndexPruneState() : null;

            ScreenEvents.afterTick(screen).register(s -> {
                if (!ContainerContentTracker.hasReceived(containerScreen.getMenu().containerId)) return;

                capture(key, containerScreen.getMenu());
                if (pruneState != null) {
                    Set<StorageKey> before = Set.copyOf(StorageCache.getInstance().allKnown());
                    tickIndex(containerScreen.getMenu(), pruneState);
                    if (!before.equals(StorageCache.getInstance().allKnown())
                            && s instanceof StorageContainerScreen storageScreen) {
                        Minecraft.getInstance().schedule(storageScreen::refreshCards);
                    }
                }
            });

            ScreenEvents.remove(screen).register(s -> {
                if (ContainerContentTracker.hasReceived(containerScreen.getMenu().containerId)) {
                    capture(key, containerScreen.getMenu());
                }
                StorageCache.getInstance().saveToDisk();
                StorageNames.getInstance().saveToDisk();
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> {
                    ContainerContentTracker.reset();
                    StorageProfile.getInstance().adoptLastKnownProfile();
                    StorageCache.getInstance().reloadForCurrentProfile();
                    StorageNames.getInstance().reloadForCurrentProfile();
                    StorageOrder.getInstance().reloadForCurrentProfile();
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            StorageCache.getInstance().saveToDisk();
            StorageNames.getInstance().saveToDisk();
            StorageOrder.getInstance().saveToDisk();
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) onGameMessage(message);
        });
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, overlay) -> {
            if (!overlay) onGameMessage(message);
        });
    }

    private static void onGameMessage(net.minecraft.network.chat.Component message) {
        Matcher m = PROFILE_ID_PATTERN.matcher(TextUtils.stripText(message));
        if (!m.find()) return;

        String newProfileId = m.group(1);
        StorageProfile profile = StorageProfile.getInstance();

        if (profile.current().filter(newProfileId::equals).isPresent()) {
            if (!profile.isConfirmed()) {
                profile.markConfirmed();
                StorageInitializer.LOGGER.info("SkyBlock profile {} confirmed; flushing pending saves", newProfileId);
                StorageCache.getInstance().saveToDisk();
                StorageNames.getInstance().saveToDisk();
                StorageOrder.getInstance().saveToDisk();
            }
            return;
        }

        if (profile.isConfirmed()) {
            StorageCache.getInstance().saveToDisk();
            StorageNames.getInstance().saveToDisk();
            StorageOrder.getInstance().saveToDisk();
        }

        StorageInitializer.LOGGER.info("SkyBlock profile changed to {}", newProfileId);
        profile.markConfirmed();
        profile.onProfileIdSeen(newProfileId);
    }

    private static void capture(StorageKey key, AbstractContainerMenu menu) {
        int containerSlots = menu.slots.size() - PLAYER_SLOT_COUNT;
        if (containerSlots <= 0) return;

        boolean index = key.type() == StorageKey.Type.STORAGE_INDEX;
        int startSlot = 9;

        List<ItemStack> items = new ArrayList<>(containerSlots);
        for (int i = startSlot; i < containerSlots; i++) {
            if (!shouldCaptureSlot(key, i)) continue;

            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem().copy();
            if (index) bakeCatharsisModel(stack, slot.index);
            items.add(stack);
        }

        StorageCache.getInstance().put(key, items);
    }

    private static void bakeCatharsisModel(ItemStack stack, int slotIndex) {
        if (stack.isEmpty() || !CatharsisCompat.isLoaded()) return;

        Identifier model = CatharsisCompat.getSlotModel(slotIndex);
        if (model != null) {
            stack.set(DataComponents.ITEM_MODEL, model);
        }
    }

    private static boolean shouldCaptureSlot(StorageKey key, int slotIndex) {
        if (key.type() != StorageKey.Type.STORAGE_INDEX) return true;
        int row = slotIndex / 9;
        return row == 1 || row == 3 || row == 4;
    }

    private static void tickIndex(AbstractContainerMenu menu, IndexPruneState state) {
        Set<StorageKey> found = scanIndex(menu);
        if (found.isEmpty()) return;

        StorageCache cache = StorageCache.getInstance();
        cache.addKnown(found);

        if (found.equals(state.lastFound)) {
            state.stableTicks++;
        } else {
            state.lastFound = found;
            state.stableTicks = 1;
        }

        if (state.stableTicks != INDEX_STABLE_TICKS) return;

        Set<StorageKey> foundEnder = found.stream()
                .filter(k -> k.type() == StorageKey.Type.ENDER_CHEST)
                .collect(Collectors.toSet());
        Set<StorageKey> foundBackpack = found.stream()
                .filter(k -> k.type() == StorageKey.Type.BACKPACK)
                .collect(Collectors.toSet());

        cache.replaceKnown(StorageKey.Type.ENDER_CHEST, foundEnder);
        cache.replaceKnown(StorageKey.Type.BACKPACK, foundBackpack);

        cache.retainOnly(StorageKey.Type.ENDER_CHEST, foundEnder);
        cache.retainOnly(StorageKey.Type.BACKPACK, foundBackpack);
    }

    private static Set<StorageKey> scanIndex(AbstractContainerMenu menu) {
        int containerSlots = menu.slots.size() - PLAYER_SLOT_COUNT;
        Set<StorageKey> found = new HashSet<>();

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            StorageKey.fromIndexItem(stack.getHoverName()).ifPresent(found::add);
        }
        return found;
    }

    public static void discoverRiftPages(net.minecraft.network.chat.Component title) {
        Matcher m = TextUtils.matcher(title, StorageKey.RIFT_PATTERN);
        if (!m.matches()) return;

        Set<StorageKey> allRift = new HashSet<>();
        for (int p = 1; p <= Integer.parseInt(m.group(2)); p++) {
            allRift.add(new StorageKey(StorageKey.Type.RIFT, p));
        }
        StorageCache.getInstance().replaceKnown(StorageKey.Type.RIFT, allRift);
    }

    private static final class IndexPruneState {
        Set<StorageKey> lastFound = Set.of();
        int stableTicks;
    }
}