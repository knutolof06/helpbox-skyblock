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
import net.minecraft.network.chat.Component;
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
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("profile id:\\s*([0-9a-f-]{36})", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCRAW_PROFILE_PATTERN = Pattern.compile("\"profile_id\"\\s*:\\s*\"([0-9a-f-]{36})\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAT_PROFILE_PATTERN = Pattern.compile("(?:Switching to (?:profile )?|You are currently playing on (?:profile:? )?|Your active profile is now:?\\s*|Playing on Profile:?\\s*)([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOREBOARD_PROFILE_PATTERN = Pattern.compile("(?:Profile|Profil):\\s*(?:[^A-Za-z0-9_]*\\s*)?([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE);
    private static final int INDEX_STABLE_TICKS = 10;

    public static Optional<String> detectProfileFromScoreboard(Minecraft client) {
        if (client == null || client.level == null) return Optional.empty();
        net.minecraft.world.scores.Scoreboard scoreboard = client.level.getScoreboard();
        net.minecraft.world.scores.Objective objective = scoreboard.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR);
        if (objective == null) return Optional.empty();

        for (net.minecraft.world.scores.PlayerScoreEntry score : scoreboard.listPlayerScores(objective)) {
            String owner = score.owner();
            net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayersTeam(owner);
            String line;
            if (team != null) {
                line = TextUtils.stripText(team.getPlayerPrefix()) + owner + TextUtils.stripText(team.getPlayerSuffix());
            } else {
                line = owner;
            }
            Matcher m = SCOREBOARD_PROFILE_PATTERN.matcher(line);
            if (m.find()) {
                String prof = m.group(1).trim();
                if (!prof.equalsIgnoreCase("none") && !prof.equalsIgnoreCase("unknown")) {
                    return Optional.of(prof);
                }
            }
        }
        return Optional.empty();
    }

    private StorageCaptureHandler() {
    }

    public static void checkScoreboardProfile(Minecraft client) {
        detectProfileFromScoreboard(client).ifPresent(profName -> {
            StorageProfile profile = StorageProfile.getInstance();
            if (!profile.current().filter(profName::equalsIgnoreCase).isPresent()) {
                profile.onProfileIdSeen(profName);
            }
        });
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((Minecraft client, Screen screen, int scaledWidth, int scaledHeight) -> {
            checkScoreboardProfile(client);

            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            String title = TextUtils.stripText(containerScreen.getTitle()).toLowerCase();
            if (title.contains("profile")) {
                ScreenEvents.afterTick(screen).register(s -> {
                    if (!ContainerContentTracker.hasReceived(containerScreen.getMenu().containerId)) return;
                    for (Slot slot : containerScreen.getMenu().slots) {
                        if (slot.getItem().isEmpty()) continue;
                        var lore = slot.getItem().get(DataComponents.LORE);
                        if (lore != null) {
                            for (Component line : lore.lines()) {
                                String l = TextUtils.stripText(line).toLowerCase();
                                if (l.contains("currently playing") || l.contains("selected")) {
                                    String name = TextUtils.stripText(slot.getItem().getHoverName()).trim();
                                    if (name.toLowerCase().startsWith("profile:")) {
                                        name = name.substring(8).trim();
                                    }
                                    if (!name.isBlank()) {
                                        StorageProfile.getInstance().onProfileIdSeen(name);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                });
            }

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
                    SackCache.getInstance().reloadForCurrentProfile();
                    StorageNames.getInstance().reloadForCurrentProfile();
                    StorageOrder.getInstance().reloadForCurrentProfile();

                    // Query Hypixel locraw after 2 seconds to auto-detect profile ID
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {}
                        client.execute(() -> {
                            if (client.player != null && client.player.connection != null) {
                                client.player.connection.sendCommand("locraw");
                            }
                        });
                    }, "HelpBox-ProfileDetector").start();
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            StorageCache.getInstance().saveToDisk();
            SackCache.getInstance().saveToDisk();
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
        String text = TextUtils.stripText(message);
        String newProfileId = null;

        Matcher m = PROFILE_ID_PATTERN.matcher(text);
        if (m.find()) {
            newProfileId = m.group(1);
        } else {
            m = LOCRAW_PROFILE_PATTERN.matcher(text);
            if (m.find()) {
                newProfileId = m.group(1);
            } else {
                m = CHAT_PROFILE_PATTERN.matcher(text);
                if (m.find()) {
                    newProfileId = m.group(1);
                }
            }
        }

        if (newProfileId == null || newProfileId.isBlank()) return;

        StorageProfile profile = StorageProfile.getInstance();
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