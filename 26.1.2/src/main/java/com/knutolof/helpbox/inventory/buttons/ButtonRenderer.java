package com.knutolof.helpbox.inventory.buttons;

import com.knutolof.helpbox.HelpBoxMod;
import com.knutolof.helpbox.inventory.buttons.model.InventoryButton;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Envanter ekranı üzerine butonları çizen renderer.
 *
 * <p>Bu sınıf durumsuz (stateless) bir yardımcıdır; render her zaman
 * {@link MixinContainerScreen}'den iletilen {@link GuiGraphicsExtractor} bağlamında çalışır.</p>
 *
 * <p>İkon çözümleme önceliği (Firmament uyumlu):</p>
 * <ol>
 *   <li>{@code skull:<hash>} — Mojang texture hash → player head</li>
 *   <li>{@code ITEM_ID} ya da {@code namespace:id} — vanilla/mod item</li>
 *   <li>null / bilinmeyen → "?" kutusu (crash etmez)</li>
 * </ol>
 */
public class ButtonRenderer {

    /** String → ItemStack önbelleği: her frame aynı string'i yeniden parse etme. */
    private static final Map<String, ItemStack> ICON_CACHE = new HashMap<>();

    // ── Ana Render ─────────────────────────────────────────────────────────────

    /**
     * Tüm geçerli butonları render eder. Envanter dikdörtgeni ile çakışanlar atlanır.
     *
     * @param graphics    mixin'den gelen GUI grafik bağlamı
     * @param screenW     ekran genişliği (GUI piksel)
     * @param screenH     ekran yüksekliği (GUI piksel)
     * @param invLeft     envanter sol kenarı
     * @param invTop      envanter üst kenarı
     * @param invWidth    envanter genişliği
     * @param invHeight   envanter yüksekliği
     * @param mouseX      fare X konumu
     * @param mouseY      fare Y konumu
     */
    public static void render(GuiGraphicsExtractor graphics,
                               int screenW, int screenH,
                               int invLeft, int invTop, int invWidth, int invHeight,
                               int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (!ButtonStore.enabled) return;

        for (InventoryButton btn : ButtonStore.buttons) {
            if (!btn.isValid()) continue;

            int[] bounds = btn.getBounds(invLeft, invTop, invWidth, invHeight);
            int bx = bounds[0];
            int by = bounds[1];
            int size = btn.getSize();

            boolean hovered = mouseX >= bx && mouseX < bx + size
                           && mouseY >= by && mouseY < by + size;

            // Cache'den tam ItemStack al (resource pack CIT için)
            ItemStack cachedStack = ButtonStore.iconItemCache.get(btn.id);

            renderButton(graphics, mc, btn, bx, by, size, hovered, mouseX, mouseY, cachedStack);
        }
    }


    private static void renderButton(GuiGraphicsExtractor graphics, Minecraft mc,
                                      InventoryButton btn, int bx, int by, int size,
                                      boolean hovered, int mouseX, int mouseY,
                                      ItemStack cachedStack) {
        // 1. Arka plan kutusu
        int border;
        int bg;
        if (ButtonStore.editMode) {
            // Düzenleme modunda turuncu çerçeve ile sürüklenebilir göster
            border = hovered ? 0xFFFFCC44 : 0xFFAA7700;
            bg     = hovered ? 0xDD443300 : 0xBB332200;
        } else {
            border = hovered ? 0xFF8888FF : 0xFF444466;
            bg     = hovered ? 0xDD334466 : 0xBB222233;
        }
        graphics.fill(bx - 1,      by - 1,      bx + size + 1, by + size + 1, border);
        graphics.fill(bx,          by,           bx + size,     by + size,     bg);

        // 2. İtem ikonu — cache varsa doğrudan kullan (resource pack CIT tam uyumlu)
        ItemStack stack = (cachedStack != null && !cachedStack.isEmpty())
                ? cachedStack
                : resolveIcon(btn != null ? btn.icon : null);
        if (stack != null && !stack.isEmpty()) {
            try {
                graphics.item(
                    stack,
                    bx + (size - 16) / 2,
                    by + (size - 16) / 2
                );
            } catch (Exception e) {
                HelpBoxMod.LOGGER.debug("[HelpBox] Item render hatası: {}", btn.icon, e);
                drawFallback(graphics, mc, bx, by, size);
            }
        } else {
            drawFallback(graphics, mc, bx, by, size);
        }

        // 3. Hover alt çizgisi ve Tooltip
        if (hovered && !ButtonStore.editMode) {
            graphics.fill(bx, by + size - 1, bx + size, by + size, 0xFF6688FF);
            // @macro:isim komutlarında sadece makro adını göster, @macro: prefix'ini gizle
            String tooltipText = btn.command != null ? btn.command : "";
            if (tooltipText.toLowerCase().startsWith("@macro:")) {
                tooltipText = tooltipText.substring(7); // "@macro:" kısmını çıkar
            }
            if (!tooltipText.isEmpty()) {
                graphics.setTooltipForNextFrame(mc.font, net.minecraft.network.chat.Component.literal(tooltipText), mouseX, mouseY);
            }
        }
        // 4. Düzenleme modu: sürükleme ikonu (köşe noktaları)
        if (ButtonStore.editMode) {
            int dot = 0xFFFFCC44;
            graphics.fill(bx,          by,          bx + 2,     by + 2,     dot);
            graphics.fill(bx + size-2, by,          bx + size,  by + 2,     dot);
            graphics.fill(bx,          by + size-2, bx + 2,     by + size,  dot);
            graphics.fill(bx + size-2, by + size-2, bx + size,  by + size,  dot);
        }
    }

    /** Bilinmeyen ikon için "?" kutusu çizer. */
    private static void drawFallback(GuiGraphicsExtractor graphics, Minecraft mc,
                                      int bx, int by, int size) {
        graphics.fill(bx, by, bx + size, by + size, 0xFF333333);
        graphics.centeredText(mc.font, "?", bx + size / 2, by + size / 2 - 4, 0xFFAAAAAA);
    }

    // ── Tooltip ────────────────────────────────────────────────────────────────

    // ── İkon Çözümleme ─────────────────────────────────────────────────────────

    /**
     * İkon string'inden ItemStack üretir. Sonuç önbelleklenir.
     */
    // SkyBlock özel item ID → kafatası texture hash eşlemesi
    private static final java.util.Map<String, String> SKYBLOCK_SKULL_MAP = new java.util.HashMap<>();
    static {
        SKYBLOCK_SKULL_MAP.put("GARDEN_SACK",      "184712ecbf1c6ce9de1230bfa05831aa3c5d0ee9d1ba59deef7a31cafd7d8ef3");
        SKYBLOCK_SKULL_MAP.put("LARGE_SACK_OF_SACKS", "fb49a2cb90737993201fe72a1f1ab75c5c93c28f047f685ffad5ab20c7b0caf0");
        SKYBLOCK_SKULL_MAP.put("MEDIUM_SACK_OF_SACKS", "fb49a2cb90737993201fe72a1f1ab75c5c93c28f047f685ffad5ab20c7b0caf0");
        SKYBLOCK_SKULL_MAP.put("SMALL_SACK_OF_SACKS", "fb49a2cb90737993201fe72a1f1ab75c5c93c28f047f685ffad5ab20c7b0caf0");
    }

    public static ItemStack resolveIcon(String icon) {
        if (icon == null || icon.isBlank()) return ItemStack.EMPTY;
        return ICON_CACHE.computeIfAbsent(icon, ButtonRenderer::resolveUncached);
    }

    private static ItemStack resolveUncached(String icon) {
        if (icon.startsWith("skull:")) {
            return resolveSkull(icon.substring(6));
        }

        // Custom component string parsing
        if (icon.contains("[") && icon.endsWith("]")) {
            int bracketIdx = icon.indexOf("[");
            String itemId = icon.substring(0, bracketIdx);
            ItemStack result = resolveItem(itemId);

            if (!result.isEmpty()) {
                int dataIdx = icon.indexOf("minecraft:custom_data={");
                if (dataIdx > 0) {
                    int start = dataIdx + 22;
                    int end = icon.lastIndexOf("}");
                    if (end >= start) {
                        String snbt = icon.substring(start, end + 1);
                        try {
                            net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseCompoundFully(snbt);
                            result.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                        } catch (Exception e) {
                            HelpBoxMod.LOGGER.debug("[HelpBox] Icon NBT ayrıştırma hatası", e);
                        }
                    }
                }
                return result;
            }
        }

        // SkyBlock CIT uyumlu format: "minecraft:item_id[sky:SKYBLOCK_ID]"
        if (icon.contains("[sky:") && icon.endsWith("]")) {
            int bracketIdx = icon.indexOf("[sky:");
            String itemId = icon.substring(0, bracketIdx);
            String skyId  = icon.substring(bracketIdx + 5, icon.length() - 1);
            ItemStack result = resolveItem(itemId);
            if (!result.isEmpty()) {
                try {
                    // id'yi root seviyeye ekle — Hypixel yeni formatta ExtraAttributes değil root'ta tutuyor
                    net.minecraft.nbt.CompoundTag root = new net.minecraft.nbt.CompoundTag();
                    root.putString("id", skyId);
                    result.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                               net.minecraft.world.item.component.CustomData.of(root));
                } catch (Exception e) {
                    HelpBoxMod.LOGGER.debug("[HelpBox] sky: NBT ekleme hatası", e);
                }
                return result;
            }
        }

        if (icon.startsWith("/")) {
            // Firmament skull /give command check
            int valueIdx = icon.indexOf("name:\"textures\",value:\"");
            if (valueIdx > 0) {
                int start = valueIdx + 23;
                int end = icon.indexOf("\"", start);
                if (end > start) {
                    String base64 = icon.substring(start, end);
                    return resolveSkullBase64(base64);
                }
            }
            // Normal /give command fallback
            String itemId = extractItemIdFromGive(icon);
            if (itemId != null) {
                return resolveItem(itemId);
            }
        }

        // SkyBlock özel ID'si mi? (büyük harf, kayıt defterinde yok)
        String upperIcon = icon.trim().toUpperCase();
        if (SKYBLOCK_SKULL_MAP.containsKey(upperIcon)) {
            return resolveSkull(SKYBLOCK_SKULL_MAP.get(upperIcon));
        }

        ItemStack result = resolveItem(icon);
        // Vanilla'da bulunamazsa ve tamamen büyük harfliyse SkyBlock ID'si kabul et
        if (result.isEmpty() && icon.equals(icon.toUpperCase()) && !icon.contains(":")) {
            HelpBoxMod.LOGGER.debug("[HelpBox] SkyBlock ID '{}' için vanilla item bulunamadı.", icon);
        }
        return result;
    }

    private static ItemStack resolveSkullBase64(String base64Value) {
        try {
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
            UUID profileId = UUID.nameUUIDFromBytes(base64Value.getBytes(StandardCharsets.UTF_8));
            
            com.google.common.collect.ImmutableMultimap<String, Property> props =
                com.google.common.collect.ImmutableMultimap.of("textures", new Property("textures", base64Value));
            com.mojang.authlib.properties.PropertyMap propertyMap = new com.mojang.authlib.properties.PropertyMap(props);

            GameProfile profile = new GameProfile(profileId, "HelpBox_Skull", propertyMap);
            skull.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            return skull;
        } catch (Exception e) {
            HelpBoxMod.LOGGER.debug("[HelpBox] Base64 skull resolve hatası.", e);
            return new ItemStack(Items.PLAYER_HEAD);
        }
    }

    /**
     * Mojang texture hash'ından player head ItemStack oluşturur.
     */
    private static ItemStack resolveSkull(String textureHash) {
        try {
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);

            String textureUrl  = "http://textures.minecraft.net/texture/" + textureHash;
            String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
            String base64Value = Base64.getEncoder()
                    .encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));

            UUID profileId = UUID.nameUUIDFromBytes(textureHash.getBytes(StandardCharsets.UTF_8));
            
            com.google.common.collect.ImmutableMultimap<String, Property> props =
                com.google.common.collect.ImmutableMultimap.of("textures", new Property("textures", base64Value));
            com.mojang.authlib.properties.PropertyMap propertyMap = new com.mojang.authlib.properties.PropertyMap(props);

            GameProfile profile = new GameProfile(profileId, "HelpBox_" + textureHash.substring(0, Math.min(8, textureHash.length())), propertyMap);

            skull.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            return skull;
        } catch (Exception e) {
            HelpBoxMod.LOGGER.debug("[HelpBox] Skull resolve hatası: {}", textureHash, e);
            return new ItemStack(Items.PLAYER_HEAD);
        }
    }

    public static ItemStack resolveVanillaIcon(String icon, ItemStack cachedStack) {
        if (cachedStack != null && !cachedStack.isEmpty()) {
            return new ItemStack(cachedStack.getItem());
        }
        if (icon == null || icon.isBlank()) return ItemStack.EMPTY;
        String baseId = icon;
        if (baseId.startsWith("skull:")) return new ItemStack(Items.PLAYER_HEAD);
        if (baseId.contains("[")) baseId = baseId.substring(0, baseId.indexOf("["));
        return resolveItem(baseId);
    }

    /**
     * Vanilla / mod item registry ID'sinden ItemStack çözümler.
     * Büyük harf ID'leri (ENDER_CHEST) ve namespace'li ID'leri (minecraft:ender_chest) destekler.
     */
    public static ItemStack resolveItem(String itemId) {
        try {
            String normalized = itemId.trim().toLowerCase();
            Identifier rl;
            if (normalized.contains(":")) {
                String[] parts = normalized.split(":", 2);
                rl = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
                var opt = BuiltInRegistries.ITEM.getOptional(rl);
                if (opt.isPresent()) return new ItemStack(opt.get());
            } else {
                // Önce minecraft namespace'inde ara
                rl = Identifier.fromNamespaceAndPath("minecraft", normalized);
                var opt = BuiltInRegistries.ITEM.getOptional(rl);
                if (opt.isPresent()) return new ItemStack(opt.get());
                
                // Bulunamazsa tüm modlarda (namespace'lerde) bu path'i ara (örneğin "garden_sack")
                for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                    if (id.getPath().equals(normalized)) {
                        var optId = BuiltInRegistries.ITEM.getOptional(id);
                        if (optId.isPresent()) {
                            return new ItemStack(optId.get());
                        }
                    }
                }
            }
        } catch (Exception e) {
            HelpBoxMod.LOGGER.debug("[HelpBox] Item resolve hatası: {}", itemId, e);
        }
        return ItemStack.EMPTY;
    }

    // ── Yardımcı ──────────────────────────────────────────────────────────────

    /**
     * İkon önbelleğini temizler.
     * Editörde buton değiştirildiğinde çağrılmalıdır.
     */
    public static void clearCache() {
        ICON_CACHE.clear();
    }

    /**
     * Her türlü ikon girdisini (/give, /setblock, /summon, ArmorStand, ItemDisplay, Mannequin, Title, LootTable,
     * Minecraft URL, Base64, raw 64-hex hash, skull:hash vb.) otomatik olarak algılayıp temiz skull:hash veya item_id formatına dönüştürür.
     */
    public static String normalizeIconInput(String input) {
        if (input == null || input.isBlank()) return "";
        String str = input.trim();

        // 1. Minecraft texture URL (http://textures.minecraft.net/texture/<hash> veya textures.minecraft.net/texture/<hash>)
        int textureIdx = str.indexOf("textures.minecraft.net/texture/");
        if (textureIdx >= 0) {
            String sub = str.substring(textureIdx + "textures.minecraft.net/texture/".length());
            StringBuilder hash = new StringBuilder();
            for (char c : sub.toCharArray()) {
                if (Character.isLetterOrDigit(c)) {
                    hash.append(c);
                } else {
                    break;
                }
            }
            if (!hash.isEmpty()) {
                return "skull:" + hash.toString();
            }
        }

        // 2. Base64 içeriyor mu? (value:"eyJ0..." veya value="eyJ0..." veya doğrudan eyJ0...)
        if (str.contains("eyJ")) {
            int start = str.indexOf("eyJ");
            int end = start;
            while (end < str.length() && (Character.isLetterOrDigit(str.charAt(end)) || str.charAt(end) == '=' || str.charAt(end) == '+' || str.charAt(end) == '/')) {
                end++;
            }
            if (end > start) {
                String base64 = str.substring(start, end);
                String hash = extractHashFromBase64(base64);
                if (hash != null && !hash.isEmpty()) return "skull:" + hash;
            }
        }

        // 3. Raw 64 karakter hex hash (örn: f98bc63f05f6378bf29ef10e3d82acb3ceb73a720bf80f30bc576d0ad8c40cfb)
        if (str.length() == 64 && str.matches("[0-9a-fA-F]{64}")) {
            return "skull:" + str.toLowerCase();
        }

        // 4. skull: prefix ile başlamışsa
        if (str.startsWith("skull:")) {
            String hash = str.substring(6).trim();
            return "skull:" + hash;
        }

        // 5. Komutlar (/give, /setblock, /summon, armor_stand, item_display, mannequin, title, loot vb.)
        if (str.startsWith("/") || str.toLowerCase().startsWith("give ") || str.toLowerCase().startsWith("setblock ") || str.toLowerCase().startsWith("summon ") || str.toLowerCase().startsWith("loot ")) {
            String itemId = extractItemIdFromGive(str);
            if (itemId != null) return itemId;
        }

        return str;
    }

    private static String extractHashFromBase64(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            int urlIdx = json.indexOf("\"url\"");
            if (urlIdx > 0) {
                int startUrl = json.indexOf("\"", urlIdx + 5) + 1;
                int endUrl = json.indexOf("\"", startUrl);
                if (startUrl > 0 && endUrl > startUrl) {
                    String url = json.substring(startUrl, endUrl);
                    int lastSlash = url.lastIndexOf('/');
                    if (lastSlash > 0 && lastSlash + 1 < url.length()) {
                        return url.substring(lastSlash + 1);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * {@code /give} komutundan item ID'sini ayrıştırır.
     * Örn: {@code "/give @s minecraft:compass{...}"} → {@code "minecraft:compass"}
     *
     * @param giveCommand yapıştırılan /give komutu
     * @return item ID string veya null (parse edilemezse)
     */
    public static String extractItemIdFromGive(String giveCommand) {
        if (giveCommand == null) return null;
        String cmd = giveCommand.trim();
        if (cmd.startsWith("/give")) cmd = cmd.substring(5).trim();
        // format: @s <item_id> [count] [nbt]
        String[] parts = cmd.split("\\s+", 3);
        if (parts.length < 2) return null;
        String itemPart = parts[1]; // item ID (namespace:id ya da sadece id)
        // NBT veya tag var mı? { karakterini kes
        int braceIdx = itemPart.indexOf('{');
        if (braceIdx > 0) itemPart = itemPart.substring(0, braceIdx);
        return itemPart.isBlank() ? null : itemPart;
    }
}
