package com.knutolof.helpbox.storage.storage;

import com.knutolof.helpbox.storage.util.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Identifies a single Sack page or index.
 */
public record SackKey(Type type, String customName) {

    public static final Comparator<SackKey> DISPLAY_ORDER = (a, b) -> {
        if (a.type() == Type.SACK_INDEX) return 1;
        if (b.type() == Type.SACK_INDEX) return -1;
        return Comparator.comparing(SackKey::type).thenComparing(SackKey::displayName).compare(a, b);
    };

    public SackKey(Type type) {
        this(type, type.displayName);
    }

    public static SackKey canonical(SackKey key) {
        if (key.type() != Type.CUSTOM_SACK) return key;
        return fromTitle(Component.literal(key.customName())).orElse(key);
    }

    /**
     * Tries to identify a Sack page from a screen title.
     * Matches longer aliases first so "Large Enchanted Agronomy Sack" matches ENCHANTED_AGRONOMY instead of AGRONOMY.
     */
    public static Optional<SackKey> fromTitle(Component title) {
        String text = TextUtils.stripText(title).trim();
        String lower = text.toLowerCase();

        // Exclude Bazaar, Auction House, Trades, and non-sack screens
        if (lower.contains("bazaar") || lower.contains("auction") || lower.contains("trade")
                || lower.contains("recipe") || lower.contains("crafting") || lower.contains("bank")
                || lower.contains("upgrade") || lower.contains("shop") || lower.contains("merchant")
                || lower.contains("info") || lower.contains("slot") || lower.contains("buy")
                || lower.contains("tier") || lower.contains("unlock") || lower.contains("settings")
                || lower.contains("config") || lower.contains("preview") || lower.contains("rules")
                || lower.contains("select") || lower.contains("menu") || lower.contains("reward")) {
            return Optional.empty();
        }

        // All Hypixel Sack screens MUST contain "sack" or "sacks" in their title
        if (!lower.contains("sack")) {
            return Optional.empty();
        }

        if (lower.equals("sack of sacks") || lower.equals("sacks") || lower.equals("sack storage")
                || lower.contains("sack of sacks") || lower.contains("sacks storage") || lower.startsWith("sacks")) {
            return Optional.of(new SackKey(Type.SACK_INDEX));
        }

        List<Type> sortedTypes = Arrays.stream(Type.values())
                .filter(t -> t != Type.SACK_INDEX && t != Type.CUSTOM_SACK)
                .sorted((t1, t2) -> {
                    int max1 = Arrays.stream(t1.aliases).mapToInt(String::length).max().orElse(0);
                    int max2 = Arrays.stream(t2.aliases).mapToInt(String::length).max().orElse(0);
                    return Integer.compare(max2, max1);
                })
                .toList();

        for (Type t : sortedTypes) {
            for (String alias : t.aliases) {
                if (lower.contains(alias)) {
                    return Optional.of(new SackKey(t));
                }
            }
        }

        // If onlyOfficialSacks is enabled, never create custom sack types for unknown containers
        if (com.knutolof.helpbox.storage.config.SackConfig.onlyOfficialSacks) {
            return Optional.empty();
        }

        // For custom sacks, require the title to strictly end with "sack" or "sacks"
        if (lower.endsWith("sack") || lower.endsWith("sacks")) {
            return Optional.of(new SackKey(Type.CUSTOM_SACK, text));
        }

        return Optional.empty();
    }

    /**
     * Identifies a Sack key from an item inside the Sack of Sacks index menu.
     */
    public static Optional<SackKey> fromIndexItem(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();

        // 1. Exclude non-sack items by material
        if (stack.is(net.minecraft.world.item.Items.ARROW) || stack.is(net.minecraft.world.item.Items.BARRIER)
                || stack.getItem().toString().contains("stained_glass_pane")) {
            return Optional.empty();
        }

        Component nameComp = stack.getHoverName();
        String name = TextUtils.stripText(nameComp).trim();
        String lower = name.toLowerCase();

        if (lower.contains("locked") || lower.contains("empty") || lower.contains("go back")
                || lower.contains("close") || lower.contains("previous") || lower.contains("next")
                || lower.contains("upgrade") || lower.contains("shop") || lower.contains("information")
                || lower.contains("slot") || lower.contains("info")) {
            return Optional.empty();
        }

        // 2. Check Hypixel ExtraAttributes.id if present
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains("ExtraAttributes")) {
                net.minecraft.nbt.CompoundTag ea = tag.getCompound("ExtraAttributes").orElse(null);
                if (ea != null && ea.contains("id")) {
                    String sbId = ea.getString("id").orElse("").toUpperCase();
                    if (!sbId.endsWith("_SACK") && !sbId.endsWith("_SACKS") && !sbId.contains("SACK")) {
                        return Optional.empty();
                    }
                }
            }
        }

        // 3. Verify lore contains sack attributes (e.g. Stored, Capacity, or SACK rarity)
        var lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            boolean hasSackLore = false;
            for (Component line : lore.lines()) {
                String lStr = TextUtils.stripText(line).toLowerCase();
                if (lStr.contains("stored") || lStr.contains("capacity") || lStr.endsWith("sack") || lStr.contains("sack")) {
                    hasSackLore = true;
                    break;
                }
            }
            if (!hasSackLore) {
                return Optional.empty();
            }
        }

        return fromTitle(nameComp);
    }

    public static Optional<SackKey> fromId(String id) {
        if (id.startsWith("custom_")) {
            return Optional.of(new SackKey(Type.CUSTOM_SACK, id.substring(7).replace('_', ' ')));
        }
        try {
            Type type = Type.valueOf(id.toUpperCase());
            return Optional.of(new SackKey(type));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String id() {
        if (type == Type.CUSTOM_SACK) {
            return "custom_" + customName.toLowerCase().replace(' ', '_');
        }
        return type.name().toLowerCase();
    }

    public String displayName() {
        if (type == Type.CUSTOM_SACK) {
            return customName;
        }
        return type.displayName;
    }

    public boolean isEnchanted() {
        return type.isEnchanted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SackKey sackKey = (SackKey) o;
        if (type != sackKey.type) {
            // Check if one is CUSTOM_SACK with matching name to standard type
            if (type == Type.CUSTOM_SACK && customName.equalsIgnoreCase(sackKey.displayName())) return true;
            if (sackKey.type == Type.CUSTOM_SACK && sackKey.customName.equalsIgnoreCase(displayName())) return true;
            return false;
        }
        return Objects.equals(customName, sackKey.customName);
    }

    @Override
    public int hashCode() {
        if (type != Type.CUSTOM_SACK) {
            return Objects.hash(type);
        }
        return Objects.hash(customName.toLowerCase());
    }

    public enum Type {

        // Normal Sacks (21)
        AGRONOMY("Agronomy Sack", false, "agronomy sack", "agronomy"),
        HUSBANDRY("Husbandry Sack", false, "husbandry sack", "husbandry"),
        MINING("Mining Sack", false, "mining sack", "mining"),
        COMBAT("Combat Sack", false, "combat sack", "combat"),
        FORAGING("Foraging Sack", false, "foraging sack", "foraging"),
        FISHING("Fishing Sack", false, "fishing sack", "fishing"),
        GEMSTONE("Gemstone Sack", false, "gemstone sack", "gemstones sack", "gemstone", "gemstones"),
        NETHER("Nether Sack", false, "nether sack", "nether"),
        LAVA_FISHING("Lava Fishing Sack", false, "lava fishing sack", "lava fishing"),
        SLAYER("Slayer Sack", false, "slayer sack", "slayer"),
        DUNGEON("Dungeon Sack", false, "dungeon sack", "dungeons sack", "dungeon", "dungeons"),
        RUNE("Rune Sack", false, "rune sack", "runes sack", "rune", "runes"),
        DRAGON("Dragon Sack", false, "dragon sack", "dragons sack", "dragon", "dragons"),
        FLOWER("Flower Sack", false, "flower sack", "flowers sack", "flower", "flowers"),
        DWARVEN("Dwarven Sack", false, "dwarven sack", "dwarven"),
        CRYSTAL_HOLLOWS("Crystal Hollows Sack", false, "crystal hollows sack", "crystal hollows"),
        GARDEN("Garden Sack", false, "garden sack", "garden"),
        BRONZE_TROPHY_FISHING("Bronze Trophy Fishing Sack", false, "bronze trophy fishing sack", "bronze trophy fishing"),
        SILVER_TROPHY_FISHING("Silver Trophy Fishing Sack", false, "silver trophy fishing sack", "silver trophy fishing"),
        SPOOKY("Spooky Sack", false, "spooky sack", "spooky"),
        WINTER("Winter Sack", false, "winter sack", "winter"),

        // Enchanted Sacks (6)
        ENCHANTED_AGRONOMY("Large Enchanted Agronomy Sack", true, "large enchanted agronomy sack", "medium enchanted agronomy sack", "small enchanted agronomy sack", "enchanted agronomy sack"),
        ENCHANTED_HUSBANDRY("Large Enchanted Husbandry Sack", true, "large enchanted husbandry sack", "medium enchanted husbandry sack", "small enchanted husbandry sack", "enchanted husbandry sack"),
        ENCHANTED_MINING("Large Enchanted Mining Sack", true, "large enchanted mining sack", "medium enchanted mining sack", "small enchanted mining sack", "enchanted mining sack"),
        ENCHANTED_COMBAT("Large Enchanted Combat Sack", true, "large enchanted combat sack", "medium enchanted combat sack", "small enchanted combat sack", "enchanted combat sack"),
        ENCHANTED_FORAGING("Large Enchanted Foraging Sack", true, "large enchanted foraging sack", "medium enchanted foraging sack", "small enchanted foraging sack", "enchanted foraging sack"),
        ENCHANTED_FISHING("Large Enchanted Fishing Sack", true, "large enchanted fishing sack", "medium enchanted fishing sack", "small enchanted fishing sack", "enchanted fishing sack"),

        CUSTOM_SACK("Custom Sack", false),

        // Sack of Sacks Main Menu (Ordered last)
        SACK_INDEX("Sack of Sacks", false, "sack of sacks", "sacks");

        public final String displayName;
        public final boolean isEnchanted;
        public final String[] aliases;

        Type(String displayName, boolean isEnchanted, String... aliases) {
            this.displayName = displayName;
            this.isEnchanted = isEnchanted;
            this.aliases = aliases;
        }
    }
}
