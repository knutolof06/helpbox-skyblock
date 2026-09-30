package com.knutolof.helpbox.inventory.buttons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knutolof.helpbox.HelpBoxMod;
import com.knutolof.helpbox.inventory.buttons.model.InventoryButton;
import com.knutolof.helpbox.inventory.buttons.ButtonStore;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Envanter butonlarının JSON'a kalıcı olarak kaydedilmesini ve yüklenmesini yönetir.
 */
public class ButtonStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("helpbox_buttons.json");

    /** Tüm mod bileşenlerinin okuyacağı ortak buton listesi. */
    public static List<InventoryButton> buttons = new ArrayList<>();

    /** Sürükle-bırak düzenleme modu. */
    public static boolean editMode = false;
    
    /** Makroya buton atama işlemi sırasında beklenen makro adı. */
    public static String pendingMacroName = null;

    /** Sistem geneli açma/kapama. false ise butonlar hiç çizilmez. */
    public static boolean enabled = true;

    /** ButtonEditorScreen için görünüm modu. true ise ızgara (varsayılan), false ise liste. */
    public static boolean gridViewEnabled = true;

    /**
     * Icon SNBT'den önceden parse edilmiş ItemStack cache'i.
     * Anahtar = button.id; oyun session'u boyunca geçerli.
     */
    public static final java.util.Map<String, net.minecraft.world.item.ItemStack> iconItemCache
            = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Son kopyalanan ikon için tam ItemStack.
     * PopupEditor kaydettiğinde bu stack'i ilgili butonun cache'ine koyar.
     */
    public static net.minecraft.world.item.ItemStack iconPendingStack = null;

    /**
     * İkon kopyalanınca (ItemNameCopier) iconBox'a yazılmak üzere bekleyen string.
     * PopupEditor.open() veya ButtonEditorScreen.initEditForm() bunu okuyup temizler.
     */
    public static String pendingIconStr = null;

    // ── Yükleme / Kaydetme ────────────────────────────────────────────────────

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            HelpBoxMod.LOGGER.info("[HelpBox] helpbox_buttons.json bulunamadı — varsayılanlar yükleniyor.");
            loadDefaults();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            buttons = parseButtonList(root, false);
            // Kaydedilmiş enabled durumunu oku
            if (root.has("enabled")) {
                enabled = root.get("enabled").getAsBoolean();
            }
            if (root.has("gridViewEnabled")) {
                gridViewEnabled = root.get("gridViewEnabled").getAsBoolean();
            }
            HelpBoxMod.LOGGER.info("[HelpBox] {} buton yüklendi.", buttons.size());
        } catch (Exception e) {
            HelpBoxMod.LOGGER.error("[HelpBox] helpbox_buttons.json okunamadı — varsayılanlar kullanılıyor.", e);
            loadDefaults();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Path tmp = CONFIG_FILE.resolveSibling(CONFIG_FILE.getFileName() + ".tmp");

            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            root.addProperty("gridViewEnabled", gridViewEnabled);
            JsonArray arr = new JsonArray();
            for (InventoryButton btn : buttons) {
                arr.add(buttonToJson(btn));
            }
            root.add("buttons", arr);

            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(root, writer);
            }
            Files.move(tmp, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            HelpBoxMod.LOGGER.debug("[HelpBox] {} buton kaydedildi.", buttons.size());
        } catch (IOException e) {
            HelpBoxMod.LOGGER.error("[HelpBox] helpbox_buttons.json kaydedilemedi.", e);
        }
    }

    // ── Firmament Import ──────────────────────────────────────────────────────

    public static int importFromFirmament(Path firmamentFile) {
        if (!Files.exists(firmamentFile)) {
            HelpBoxMod.LOGGER.warn("[HelpBox] Firmament dosyası bulunamadı: {}", firmamentFile);
            return -1;
        }
        try (Reader reader = Files.newBufferedReader(firmamentFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            List<InventoryButton> imported = parseButtonList(root, true);
            int count = 0;
            for (InventoryButton btn : imported) {
                if (!btn.isValid()) continue;
                InventoryButton existing = buttons.stream()
                        .filter(b -> b.command != null && b.command.equalsIgnoreCase(btn.command))
                        .findFirst().orElse(null);
                if (existing != null) {
                    existing.x = btn.x;
                    existing.y = btn.y;
                    existing.icon = btn.icon;
                    count++;
                } else {
                    buttons.add(btn);
                    count++;
                }
            }
            HelpBoxMod.LOGGER.info("[HelpBox] Firmament'ten {} buton import edildi.", count);
            return count;
        } catch (Exception e) {
            HelpBoxMod.LOGGER.error("[HelpBox] Firmament import hatası.", e);
            return -1;
        }
    }

    // ── Varsayılan Presetler ──────────────────────────────────────────────────

    /**
     * Hypixel SkyBlock için hazır warp ve komut butonlarını yükler.
     * Pozisyonlar envanterın sol-üst köşesine göre ofsettir.
     */
    public static void loadDefaults() {
        buttons.clear();

        // Sol kenara yakın warp butonları (dikey sıra)
        buttons.add(makeButton(-20, 0,
                "skull:c9c8881e42915a9d29bb61a16fb26d059913204d265df5b439b3d792acd56",
                "warp home"));
        buttons.add(makeButton(-20, 20,
                "skull:d7cc6687423d0570d556ac53e0676cb563bbdd9717cd8269bdebed6f6d4e7bf8",
                "warp hub"));
        buttons.add(makeButton(-20, 40,
                "skull:9b56895b9659896ad647f58599238af532d46db9c1b0389b8bbeb70999dab33d",
                "warp dungeon_hub"));
        buttons.add(makeButton(-20, 60,
                "skull:7840b87d52271d2a755dedc82877e0ed3df67dcc42ea479ec146176b02779a5",
                "warp end"));

        // Sağ kenara yakın menü kısayolları
        buttons.add(makeButton(130, -20,
                "skull:86f06eaa3004aeed09b3d5b45d976de584e691c0e9cade133635de93d23b9edb",
                "hotm"));
        buttons.add(makeButton(148, -20,
                "ENDER_CHEST",
                "storage"));
        buttons.add(makeButton(166, -20,
                "BONE",
                "pets"));

        // Alt kısayollar
        buttons.add(makeButton(-20, 80,
                "GOLD_BLOCK",
                "ah"));
        buttons.add(makeButton(-20, 100,
                "GOLD_BARDING",
                "bz"));
    }

    // ── Yardımcı Metodlar ─────────────────────────────────────────────────────

    private static InventoryButton makeButton(int x, int y, String icon, String command) {
        InventoryButton btn = new InventoryButton();
        btn.id = UUID.randomUUID().toString();
        btn.x = x;
        btn.y = y;
        btn.icon = icon;
        btn.command = command;
        btn.isGigantic = false;
        return btn;
    }

    private static InventoryButton jsonToButton(JsonObject obj, boolean isFirmament) {
        InventoryButton btn = new InventoryButton();
        if (obj.has("id") && !obj.get("id").isJsonNull()) {
            btn.id = obj.get("id").getAsString();
        }

        int rawX = obj.has("x") ? obj.get("x").getAsInt() : 0;
        int rawY = obj.has("y") ? obj.get("y").getAsInt() : 0;
        boolean hasAnchorFields = obj.has("anchorRight") || obj.has("anchorBottom");
        boolean anchorRight  = obj.has("anchorRight")  && obj.get("anchorRight").getAsBoolean();
        boolean anchorBottom = obj.has("anchorBottom") && obj.get("anchorBottom").getAsBoolean();

        if (hasAnchorFields) {
            // Hem dahili anchor format hem de Firmament format aynı anlama gelir:
            // x/y envanter kenarlarından ofset.
            btn.anchorRight  = anchorRight;
            btn.anchorBottom = anchorBottom;
            btn.x = rawX;
            btn.y = rawY;
        } else if (isFirmament) {
            // Eski Firmament format: anchor yok, sol-üsten ofset
            btn.anchorRight  = false;
            btn.anchorBottom = false;
            btn.x = rawX;
            btn.y = rawY;
        } else {
            // Eski dahili format: screen-center koordinatlar.
            // Referans envanter (176x168) kullanarak anchor-relative'e dönüştür.
            final int INV_W = 176;
            final int INV_H = 168;
            // rawX: ekran merkezinden ofset; invLeft_from_center = -INV_W/2
            // invLeft = center - INV_W/2, invRight = center + INV_W/2
            if (rawX >= 0) {
                // Sağ yarıdaysa sağ kenardan ölç
                btn.anchorRight = true;
                btn.x = rawX - INV_W / 2;  // invRight_from_center = INV_W/2
            } else {
                // Sol yarıdaysa sol kenardan ölç
                btn.anchorRight = false;
                btn.x = rawX + INV_W / 2;  // invLeft_from_center = -INV_W/2
            }
            if (rawY >= 0) {
                btn.anchorBottom = true;
                btn.y = rawY - INV_H / 2;
            } else {
                btn.anchorBottom = false;
                btn.y = rawY + INV_H / 2;
            }
        }

        btn.icon    = (obj.has("icon")    && !obj.get("icon").isJsonNull())    ? obj.get("icon").getAsString()    : null;
        btn.command = (obj.has("command") && !obj.get("command").isJsonNull()) ? obj.get("command").getAsString() : null;
        btn.isGigantic = obj.has("isGigantic") && obj.get("isGigantic").getAsBoolean();
        btn.useResourcePack = !obj.has("useResourcePack") || obj.get("useResourcePack").getAsBoolean();
        btn.iconSnbt = (obj.has("iconSnbt") && !obj.get("iconSnbt").isJsonNull()) ? obj.get("iconSnbt").getAsString() : null;
        // SNBT varsa cache'e parse et
        if (btn.iconSnbt != null) {
            try {
                // SNBT'yi parse et sonra codec ile ItemStack'e dönüştür
                net.minecraft.nbt.CompoundTag snbtTag = net.minecraft.nbt.TagParser.parseCompoundFully(btn.iconSnbt);
                var registryAccess = net.minecraft.client.Minecraft.getInstance() != null
                    && net.minecraft.client.Minecraft.getInstance().level != null
                    ? net.minecraft.client.Minecraft.getInstance().level.registryAccess()
                    : null;
                if (registryAccess != null) {
                    var result = net.minecraft.world.item.ItemStack.CODEC.parse(
                        registryAccess.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                        snbtTag
                    ).result();
                    result.ifPresent(parsed -> {
                        if (!parsed.isEmpty()) iconItemCache.put(btn.id, parsed);
                    });
                }
            } catch (Exception e) {
                HelpBoxMod.LOGGER.debug("[HelpBox] iconSnbt parse hatasi: {}", e.getMessage());
            }
        }
        return btn;
    }

    static JsonObject buttonToJson(InventoryButton btn) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id",           btn.id);
        obj.addProperty("x",            btn.x);
        obj.addProperty("y",            btn.y);
        obj.addProperty("anchorRight",  btn.anchorRight);
        obj.addProperty("anchorBottom", btn.anchorBottom);
        if (btn.icon != null) obj.addProperty("icon", btn.icon);
        else obj.add("icon", com.google.gson.JsonNull.INSTANCE);
        if (btn.command != null) obj.addProperty("command", btn.command);
        else obj.add("command", com.google.gson.JsonNull.INSTANCE);
        obj.addProperty("isGigantic", btn.isGigantic);
        obj.addProperty("useResourcePack", btn.useResourcePack);
        // iconSnbt: cache'de varsa oradan al, yoksa btn.iconSnbt kullan
        net.minecraft.world.item.ItemStack cachedStack = iconItemCache.get(btn.id);
        if (cachedStack != null && !cachedStack.isEmpty()) {
            try {
                var registryAccess = net.minecraft.client.Minecraft.getInstance() != null
                    && net.minecraft.client.Minecraft.getInstance().level != null
                    ? net.minecraft.client.Minecraft.getInstance().level.registryAccess()
                    : null;
                if (registryAccess != null) {
                    var encoded = net.minecraft.world.item.ItemStack.CODEC.encodeStart(
                        registryAccess.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                        cachedStack
                    ).result();
                    encoded.ifPresent(tag -> obj.addProperty("iconSnbt", tag.toString()));
                } else if (btn.iconSnbt != null) {
                    obj.addProperty("iconSnbt", btn.iconSnbt);
                }
            } catch (Exception e) {
                HelpBoxMod.LOGGER.debug("[HelpBox] iconSnbt serialize hatasi: {}", e.getMessage());
                if (btn.iconSnbt != null) obj.addProperty("iconSnbt", btn.iconSnbt);
            }
        } else if (btn.iconSnbt != null) {
            obj.addProperty("iconSnbt", btn.iconSnbt);
        }
        return obj;
    }

    private static List<InventoryButton> parseButtonList(JsonObject root, boolean isFirmament) {
        List<InventoryButton> result = new ArrayList<>();
        if (!root.has("buttons")) return result;
        JsonArray arr = root.getAsJsonArray("buttons");
        for (JsonElement el : arr) {
            if (el.isJsonObject()) {
                result.add(jsonToButton(el.getAsJsonObject(), isFirmament));
            }
        }
        return result;
    }
}
