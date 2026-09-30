package com.knutolof.helpbox.storage.storage;
import com.knutolof.helpbox.storage.StorageInitializer;


import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds cached Sack item stacks and their stored quantities / capacity metadata in memory
 * and persists/restores that state to NBT on disk.
 */
public final class SackCache {

    private static final SackCache INSTANCE = new SackCache();
    private final Map<SackKey, CachedSackPage> pages = new ConcurrentHashMap<>();
    private final Set<SackKey> knownPages = ConcurrentHashMap.newKeySet();
    private final List<String> customOrder = new ArrayList<>();
    private final Object ioLock = new Object();
    private boolean dirty = false;

    private SackCache() {
    }

    public static SackCache getInstance() {
        return INSTANCE;
    }

    private static Path cacheFile() {
        String profile = StorageProfile.getInstance().current().orElse("default");
        return FabricLoader.getInstance().getConfigDir()
                .resolve(StorageInitializer.MOD_ID)
                .resolve("profiles")
                .resolve(profile)
                .resolve("sack_cache.dat");
    }

    private static Optional<RegistryOps<Tag>> registryOps() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return Optional.empty();
        HolderLookup.Provider registries = client.level.registryAccess();
        return Optional.of(registries.createSerializationContext(NbtOps.INSTANCE));
    }

    public void put(SackKey key, List<ItemStack> items, Map<Integer, SackItemMeta> meta) {
        if (key.type() == SackKey.Type.SACK_INDEX) return; // Index is not a real sack page
        knownPages.add(key);
        pages.put(key, new CachedSackPage(List.copyOf(items), Map.copyOf(meta), System.currentTimeMillis()));
        dirty = true;
    }

    public Optional<CachedSackPage> get(SackKey key) {
        return Optional.ofNullable(pages.get(key));
    }

    public Map<SackKey, CachedSackPage> all() {
        return Collections.unmodifiableMap(pages);
    }

    public Set<SackKey> allKnown() {
        return Collections.unmodifiableSet(knownPages);
    }

    public void addKnown(Set<SackKey> keys) {
        if (knownPages.addAll(keys)) {
            dirty = true;
        }
    }

    public void removeKnown(SackKey key) {
        if (key == null) return;
        knownPages.remove(key);
        pages.remove(key);
        customOrder.remove(key.id());
        dirty = true;
        saveToDisk();
    }

    public void pruneInvalidPages() {
        boolean changed = false;
        Iterator<SackKey> it = knownPages.iterator();
        while (it.hasNext()) {
            SackKey key = it.next();
            if (key.type() == SackKey.Type.SACK_INDEX) {
                it.remove();
                pages.remove(key);
                changed = true;
                continue;
            }
            if (com.knutolof.helpbox.storage.config.SackConfig.onlyOfficialSacks && key.type() == SackKey.Type.CUSTOM_SACK) {
                it.remove();
                pages.remove(key);
                customOrder.remove(key.id());
                changed = true;
                continue;
            }
            String lower = key.displayName().toLowerCase();
            if (lower.contains("upgrade") || lower.contains("shop") || lower.contains("merchant")
                    || lower.contains("info") || lower.contains("slot") || lower.contains("buy")
                    || lower.contains("tier") || lower.contains("unlock") || lower.contains("settings")
                    || lower.contains("close") || lower.contains("empty") || lower.contains("locked")
                    || lower.contains("preview") || lower.contains("rules") || lower.contains("menu")) {
                it.remove();
                pages.remove(key);
                customOrder.remove(key.id());
                changed = true;
                continue;
            }
            CachedSackPage page = pages.get(key);
            if (key.type() == SackKey.Type.CUSTOM_SACK && (page == null || page.items().isEmpty())) {
                it.remove();
                pages.remove(key);
                customOrder.remove(key.id());
                changed = true;
            }
        }
        if (changed) {
            dirty = true;
            saveToDisk();
        }
    }

    public List<String> getCustomOrder() {
        return Collections.unmodifiableList(customOrder);
    }

    public void moveSack(SackKey key, int delta, List<SackKey> currentKeys) {
        if (customOrder.isEmpty()) {
            for (SackKey k : currentKeys) {
                customOrder.add(k.id());
            }
        }
        String id = key.id();
        int idx = customOrder.indexOf(id);
        if (idx == -1) {
            customOrder.add(id);
            idx = customOrder.size() - 1;
        }
        int newIdx = idx + delta;
        if (newIdx >= 0 && newIdx < customOrder.size()) {
            Collections.swap(customOrder, idx, newIdx);
            dirty = true;
            saveToDisk();
        }
    }

    public void swapSacks(SackKey key1, SackKey key2, List<SackKey> currentKeys) {
        if (customOrder.isEmpty()) {
            for (SackKey k : currentKeys) {
                customOrder.add(k.id());
            }
        }
        String id1 = key1.id();
        String id2 = key2.id();
        int idx1 = customOrder.indexOf(id1);
        int idx2 = customOrder.indexOf(id2);
        if (idx1 == -1) {
            customOrder.add(id1);
            idx1 = customOrder.size() - 1;
        }
        if (idx2 == -1) {
            customOrder.add(id2);
            idx2 = customOrder.size() - 1;
        }
        if (idx1 != -1 && idx2 != -1 && idx1 != idx2) {
            Collections.swap(customOrder, idx1, idx2);
            dirty = true;
            saveToDisk();
        }
    }

    public void replaceKnown(Set<SackKey> keys) {
        boolean changed = knownPages.removeIf(k -> k.type() != SackKey.Type.SACK_INDEX && !keys.contains(k));
        changed |= knownPages.addAll(keys);
        if (changed) {
            dirty = true;
        }
    }

    public void saveToDisk() {
        if (!dirty) return;
        if (!StorageProfile.getInstance().isConfirmed()) return;

        Optional<RegistryOps<Tag>> opsOpt = registryOps();
        if (opsOpt.isEmpty()) return;
        RegistryOps<Tag> ops = opsOpt.get();

        CompoundTag root = new CompoundTag();

        ListTag known = new ListTag();
        for (SackKey key : knownPages) {
            known.add(StringTag.valueOf(key.id()));
        }
        root.put("known", known);

        ListTag orderTag = new ListTag();
        for (String id : customOrder) {
            orderTag.add(StringTag.valueOf(id));
        }
        root.put("customOrder", orderTag);

        pages.forEach((key, page) -> {
            ListTag list = new ListTag();
            for (ItemStack stack : page.items()) {
                ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack)
                        .resultOrPartial(err ->
                                StorageInitializer.LOGGER.warn("Failed to encode sack stack in {}: {}", key.id(), err))
                        .ifPresent(list::add);
            }
            CompoundTag pageTag = new CompoundTag();
            pageTag.put("items", list);

            CompoundTag metaTag = new CompoundTag();
            page.meta().forEach((slot, itemMeta) -> {
                CompoundTag m = new CompoundTag();
                m.putLong("stored", itemMeta.stored());
                m.putLong("capacity", itemMeta.capacity());
                m.putBoolean("isFull", itemMeta.isFull());
                metaTag.put(String.valueOf(slot), m);
            });
            pageTag.put("meta", metaTag);
            pageTag.putLong("captured", page.capturedAt());
            root.put(key.id(), pageTag);
        });

        Path file = cacheFile();
        int pageCount = pages.size();
        dirty = false;
        net.minecraft.util.Util.ioPool().execute(() -> {
            synchronized (ioLock) {
                try {
                    Files.createDirectories(file.getParent());
                    Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                    NbtIo.writeCompressed(root, tmp);
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    StorageInitializer.LOGGER.debug("Saved sack cache ({} pages)", pageCount);
                } catch (IOException e) {
                    StorageInitializer.LOGGER.error("Failed to save sack cache", e);
                }
            }
        });
    }

    public void loadFromDisk() {
        Path file = cacheFile();
        if (!Files.exists(file)) return;

        Optional<RegistryOps<Tag>> opsOpt = registryOps();
        if (opsOpt.isEmpty()) return;
        RegistryOps<Tag> ops = opsOpt.get();

        CompoundTag root;
        try {
            root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            StorageInitializer.LOGGER.error("Failed to read sack cache file", e);
            return;
        }

        pages.clear();
        knownPages.clear();
        customOrder.clear();

        root.getList("known").ifPresent(list -> {
            for (Tag tag : list) {
                if (tag instanceof StringTag(String value)) {
                    SackKey.fromId(value).ifPresent(knownPages::add);
                }
            }
        });

        root.getList("customOrder").ifPresent(list -> {
            for (Tag tag : list) {
                if (tag instanceof StringTag(String value)) {
                    customOrder.add(value);
                }
            }
        });

        int loaded = 0;
        for (String id : root.keySet()) {
            if (id.equals("known")) continue;

            Optional<SackKey> keyOpt = SackKey.fromId(id);
            if (keyOpt.isEmpty()) continue;

            Optional<CompoundTag> pageTagOpt = root.getCompound(id);
            if (pageTagOpt.isEmpty()) continue;
            CompoundTag pageTag = pageTagOpt.get();

            ListTag list = pageTag.getList("items").orElse(new ListTag());
            List<ItemStack> items = new ArrayList<>(list.size());
            for (Tag tag : list) {
                items.add(ItemStack.OPTIONAL_CODEC.parse(ops, tag)
                        .resultOrPartial(err ->
                                StorageInitializer.LOGGER.warn("Failed to decode stack in {}: {}", id, err))
                        .orElse(ItemStack.EMPTY));
            }

            Map<Integer, SackItemMeta> metaMap = new HashMap<>();
            pageTag.getCompound("meta").ifPresent(metaTag -> {
                for (String slotKey : metaTag.keySet()) {
                    try {
                        int slot = Integer.parseInt(slotKey);
                        CompoundTag m = metaTag.getCompound(slotKey).orElse(new CompoundTag());
                        long stored = m.getLong("stored").orElse(0L);
                        long capacity = m.getLong("capacity").orElse(0L);
                        boolean isFull = m.getBoolean("isFull").orElse(false);
                        metaMap.put(slot, new SackItemMeta(stored, capacity, isFull));
                    } catch (NumberFormatException ignored) {
                    }
                }
            });

            long captured = pageTag.getLong("captured").orElse(0L);
            pages.put(keyOpt.get(), new CachedSackPage(List.copyOf(items), Map.copyOf(metaMap), captured));
            knownPages.add(keyOpt.get());
            loaded++;
        }

        // Remove any stale SACK_INDEX entries that were erroneously cached before
        pages.keySet().removeIf(k -> k.type() == SackKey.Type.SACK_INDEX);
        knownPages.removeIf(k -> k.type() == SackKey.Type.SACK_INDEX);

        dirty = false;
        StorageInitializer.LOGGER.info("Loaded sack cache: {} pages", loaded);
    }

    public void reloadForCurrentProfile() {
        pages.clear();
        knownPages.clear();
        dirty = false;
        loadFromDisk();
    }

    public void updateFromApiCounts(Map<String, Integer> apiCounts) {
        if (apiCounts.isEmpty()) return;
        boolean updated = false;

        for (Map.Entry<SackKey, CachedSackPage> pageEntry : pages.entrySet()) {
            CachedSackPage page = pageEntry.getValue();
            Map<Integer, SackItemMeta> newMeta = new HashMap<>(page.meta());
            boolean pageUpdated = false;

            for (int i = 0; i < page.items().size(); i++) {
                ItemStack stack = page.items().get(i);
                if (stack == null || stack.isEmpty()) continue;
                String hoverName = com.knutolof.helpbox.storage.util.TextUtils.stripText(stack.getHoverName()).trim().toUpperCase().replace(" ", "_");

                for (Map.Entry<String, Integer> countEntry : apiCounts.entrySet()) {
                    String apiId = countEntry.getKey().toUpperCase();
                    if (apiId.equals(hoverName) || apiId.contains(hoverName) || hoverName.contains(apiId)) {
                        long count = countEntry.getValue();
                        SackItemMeta old = newMeta.get(i);
                        long cap = (old != null) ? old.capacity() : 20000L;
                        boolean isFull = (old != null && old.isFull()) || (cap > 0 && count >= cap);
                        newMeta.put(i, new SackItemMeta(count, cap, isFull));
                        pageUpdated = true;
                        break;
                    }
                }
            }

            if (pageUpdated) {
                pages.put(pageEntry.getKey(), new CachedSackPage(page.items(), Map.copyOf(newMeta), System.currentTimeMillis()));
                updated = true;
            }
        }

        if (updated) {
            dirty = true;
            saveToDisk();
        }
    }

    public void updateItemDelta(String itemName, long delta) {
        if (itemName == null || itemName.isBlank() || delta == 0) return;
        String cleanName = com.knutolof.helpbox.storage.util.TextUtils.stripText(net.minecraft.network.chat.Component.literal(itemName)).trim();
        boolean updated = false;

        for (Map.Entry<SackKey, CachedSackPage> pageEntry : pages.entrySet()) {
            CachedSackPage page = pageEntry.getValue();
            List<ItemStack> currentItems = new ArrayList<>(page.items());
            Map<Integer, SackItemMeta> currentMeta = new HashMap<>(page.meta());

            int matchIndex = -1;
            for (int i = 0; i < currentItems.size(); i++) {
                ItemStack stack = currentItems.get(i);
                if (stack != null && !stack.isEmpty()) {
                    String sName = com.knutolof.helpbox.storage.util.TextUtils.stripText(stack.getHoverName()).trim();
                    if (sName.equalsIgnoreCase(cleanName)) {
                        matchIndex = i;
                        break;
                    }
                }
            }

            if (matchIndex != -1) {
                SackItemMeta old = currentMeta.get(matchIndex);
                long newStored = Math.max(0, (old != null ? old.stored() : 0) + delta);
                long cap = (old != null && old.capacity() > 0) ? old.capacity() : 20000L;
                boolean isFull = (old != null && old.isFull()) || (cap > 0 && newStored >= cap);
                currentMeta.put(matchIndex, new SackItemMeta(newStored, cap, isFull));
                pages.put(pageEntry.getKey(), new CachedSackPage(List.copyOf(currentItems), Map.copyOf(currentMeta), System.currentTimeMillis()));
                updated = true;
                break;
            }
        }

        if (updated) {
            dirty = true;
            saveToDisk();
        }
    }

    public record SackItemMeta(long stored, long capacity, boolean isFull) {
    }

    public record CachedSackPage(List<ItemStack> items, Map<Integer, SackItemMeta> meta, long capturedAt) {
    }
}
