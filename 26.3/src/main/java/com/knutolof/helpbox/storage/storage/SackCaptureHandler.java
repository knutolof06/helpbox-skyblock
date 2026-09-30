package com.knutolof.helpbox.storage.storage;

import com.knutolof.helpbox.storage.util.TextUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures Sack container contents and lore metadata (stored count, capacity & full state)
 * into {@link SackCache}.
 */
public final class SackCaptureHandler {

    private static final int PLAYER_SLOT_COUNT = 36;
    private static final Pattern STORED_CAPACITY_PATTERN =
            Pattern.compile("(?:stored|amount)?:?\\s*([0-9,\\.]+k?m?b?)\\s*/\\s*([0-9,\\.]+k?m?b?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STORED_PATTERN =
            Pattern.compile("(?:stored|amount):?\\s*([0-9,\\.]+k?m?b?)", Pattern.CASE_INSENSITIVE);

    private SackCaptureHandler() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((Minecraft client, Screen screen, int scaledWidth, int scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            Optional<SackKey> keyOpt = SackKey.fromTitle(containerScreen.getTitle());
            if (keyOpt.isEmpty()) return;
            SackKey key = keyOpt.get();

            ScreenEvents.afterTick(screen).register(s -> {
                if (!ContainerContentTracker.hasReceived(containerScreen.getMenu().containerId)) return;

                if (key.type() == SackKey.Type.SACK_INDEX) {
                    scanIndexMenu(containerScreen.getMenu());
                } else {
                    capture(key, containerScreen.getMenu());
                }
            });

            ScreenEvents.remove(screen).register(s -> {
                if (ContainerContentTracker.hasReceived(containerScreen.getMenu().containerId)) {
                    if (key.type() == SackKey.Type.SACK_INDEX) {
                        scanIndexMenu(containerScreen.getMenu());
                    } else {
                        // Only capture if the menu still has real items.
                        // When the server closes the container (e.g. via /sacks), it clears
                        // all slot items to air BEFORE the remove event fires.
                        // Capturing empty slots would overwrite good cached data with nothing.
                        if (hasAnyNonEmptySlot(containerScreen.getMenu())) {
                            capture(key, containerScreen.getMenu());
                        }
                    }
                }
                SackCache.getInstance().saveToDisk();
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> {
                    SackCache.getInstance().reloadForCurrentProfile();
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SackCache.getInstance().saveToDisk();
        });
    }

    private static void scanIndexMenu(AbstractContainerMenu menu) {
        int containerSlots = menu.slots.size() - PLAYER_SLOT_COUNT;
        if (containerSlots <= 0) return;

        Set<SackKey> found = new HashSet<>();
        for (int i = 0; i < containerSlots; i++) {
            ItemStack sackStack = menu.slots.get(i).getItem();
            if (sackStack.isEmpty()) continue;

            Optional<SackKey> keyOpt = SackKey.fromIndexItem(sackStack);
            if (keyOpt.isEmpty()) continue;

            SackKey key = keyOpt.get();
            found.add(key);

            ItemLore lore = sackStack.get(DataComponents.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                parseIndexSackLore(key, lore.lines());
            }
        }

        if (!found.isEmpty()) {
            SackCache.getInstance().addKnown(found);
        }
    }

    private static void parseIndexSackLore(SackKey key, List<Component> lines) {
        Optional<SackCache.CachedSackPage> existingPage = SackCache.getInstance().get(key);
        List<ItemStack> currentItems = existingPage.map(p -> new ArrayList<>(p.items())).orElse(new ArrayList<>());
        Map<Integer, SackCache.SackItemMeta> currentMeta = existingPage.map(p -> new HashMap<>(p.meta())).orElse(new HashMap<>());

        boolean updated = false;
        for (int k = currentItems.size() - 1; k >= 0; k--) {
            ItemStack s = currentItems.get(k);
            if (s != null && !s.isEmpty()) {
                String sName = TextUtils.stripText(s.getHoverName()).trim().toLowerCase();
                if (sName.contains("capacity") || sName.contains("click") || sName.contains("total") || sName.equals("paper")) {
                    currentItems.remove(k);
                    currentMeta.remove(k);
                    updated = true;
                }
            }
        }

        for (Component lineComp : lines) {
            String line = TextUtils.stripText(lineComp).trim();
            if (line.isEmpty() || line.startsWith("Click") || line.startsWith("Right-click") || line.startsWith("Total:")) {
                continue;
            }

            int colonIdx = line.indexOf(':');
            if (colonIdx <= 0 || colonIdx >= line.length() - 1) continue;

            String itemName = line.substring(0, colonIdx).trim();
            String lowerName = itemName.toLowerCase();
            if (lowerName.contains("capacity") || lowerName.contains("click") || lowerName.contains("total")
                    || lowerName.contains("tier") || lowerName.contains("upgrade") || lowerName.contains("unlocked")
                    || lowerName.contains("items")) {
                continue;
            }

            String countPart = line.substring(colonIdx + 1).trim();

            long stored = 0;
            long capacity = 20000;

            if (countPart.contains("/")) {
                String[] parts = countPart.split("/");
                stored = parseAmount(parts[0]);
                capacity = parseAmount(parts[1]);
            } else {
                stored = parseAmount(countPart);
            }

            if (itemName.isEmpty()) continue;

            int itemIndex = -1;
            for (int k = 0; k < currentItems.size(); k++) {
                ItemStack s = currentItems.get(k);
                if (s != null && !s.isEmpty()) {
                    String sName = TextUtils.stripText(s.getHoverName()).trim();
                    if (sName.equalsIgnoreCase(itemName)) {
                        itemIndex = k;
                        break;
                    }
                }
            }

            if (itemIndex == -1) {
                ItemStack newStack = com.knutolof.helpbox.storage.util.SackItemResolver.resolveByNameOrId(itemName);
                currentItems.add(newStack);
                itemIndex = currentItems.size() - 1;
            }

            boolean isFull = (capacity > 0 && stored >= capacity);
            currentMeta.put(itemIndex, new SackCache.SackItemMeta(stored, capacity, isFull));
            updated = true;
        }

        if (updated) {
            SackCache.getInstance().put(key, currentItems, currentMeta);
        }
    }

    private static boolean hasAnyNonEmptySlot(AbstractContainerMenu menu) {
        int containerSlots = menu.slots.size() - PLAYER_SLOT_COUNT;
        for (int i = 0; i < containerSlots; i++) {
            if (!menu.slots.get(i).getItem().isEmpty()) return true;
        }
        return false;
    }

    private static void capture(SackKey key, AbstractContainerMenu menu) {
        int containerSlots = menu.slots.size() - PLAYER_SLOT_COUNT;
        if (containerSlots <= 0) return;

        List<ItemStack> items = new ArrayList<>(containerSlots);
        Map<Integer, SackCache.SackItemMeta> metaMap = new HashMap<>();

        for (int i = 0; i < containerSlots; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (isNavigationSlot(stack, i, containerSlots)) {
                items.add(ItemStack.EMPTY);
            } else {
                items.add(stack.copy());
                if (!stack.isEmpty()) {
                    SackCache.SackItemMeta meta = extractMeta(stack);
                    if (meta != null) {
                        metaMap.put(i, meta);
                    }
                }
            }
        }

        SackCache.getInstance().put(key, items, metaMap);
    }

    private static boolean isNavigationSlot(ItemStack stack, int slotIndex, int totalContainerSlots) {
        if (stack.isEmpty()) return false;
        if (stack.getItem().toString().contains("stained_glass_pane")) {
            return true;
        }
        return false;
    }

    private static SackCache.SackItemMeta extractMeta(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;

        long stored = 0;
        long capacity = 0;
        boolean foundStored = false;
        boolean isFull = false;

        for (Component line : lore.lines()) {
            String rawText = line.getString();
            String clean = TextUtils.stripText(line);

            Matcher m1 = STORED_CAPACITY_PATTERN.matcher(clean);
            if (m1.find()) {
                stored = parseAmount(m1.group(1));
                capacity = parseAmount(m1.group(2));
                foundStored = true;

                if (stored >= capacity && capacity > 0) {
                    isFull = true;
                } else if (rawText.contains("Stored:") || rawText.contains("Stored :")) {
                    String afterStored = rawText.substring(rawText.indexOf("Stored"));
                    if (afterStored.contains("§c") || afterStored.contains("§e") || afterStored.contains("§6")) {
                        isFull = true;
                    }
                }
                break;
            }

            Matcher m2 = STORED_PATTERN.matcher(clean);
            if (m2.find()) {
                stored = parseAmount(m2.group(1));
                foundStored = true;
                if (rawText.contains("§c") || rawText.contains("§e") || rawText.contains("FULL")) {
                    isFull = true;
                }
            }
        }

        if (foundStored) {
            return new SackCache.SackItemMeta(stored, capacity, isFull);
        }
        return null;
    }

    public static long parseAmount(String text) {
        if (text == null || text.isBlank()) return 0;
        String raw = text.replaceAll(",", "").trim().toLowerCase();
        try {
            double multiplier = 1;
            if (raw.endsWith("k")) {
                multiplier = 1_000;
                raw = raw.substring(0, raw.length() - 1);
            } else if (raw.endsWith("m")) {
                multiplier = 1_000_000;
                raw = raw.substring(0, raw.length() - 1);
            } else if (raw.endsWith("b")) {
                multiplier = 1_000_000_000;
                raw = raw.substring(0, raw.length() - 1);
            }
            return (long) (Double.parseDouble(raw) * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
