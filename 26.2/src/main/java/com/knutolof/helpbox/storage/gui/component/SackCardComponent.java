package com.knutolof.helpbox.storage.gui.component;
import com.knutolof.helpbox.storage.StorageInitializer;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.api.widget.IWidget;
import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.sprite.SpriteComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.knutolof.helpbox.storage.config.EnhancedStorageConfig;
import com.knutolof.helpbox.storage.config.SackConfig;
import com.knutolof.helpbox.storage.gui.SackOverlayState;
import com.knutolof.helpbox.storage.screen.SackContainerScreen;
import com.knutolof.helpbox.storage.storage.SackCache;
import com.knutolof.helpbox.storage.storage.SackKey;
import com.knutolof.helpbox.storage.util.ItemSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static com.knutolof.helpbox.storage.StorageInitializer.MOD_ID;

/**
 * A clickable Sack card component in the Sack Overlay GUI.
 */
public class SackCardComponent extends AbstractComponent {

    private final SackKey key;
    private final boolean cached;
    private final TextComponent pageTitle;

    public SackCardComponent(int x, int y, int width, int height,
                             SackKey key,
                             SackOverlayState state,
                             List<ItemStack> items,
                             Map<Integer, SackCache.SackItemMeta> metaMap,
                             boolean cached, boolean live,
                             int cardBorder, int titleAreaHeight,
                             int slotsAcross, int slotSize,
                             Consumer<SackKey> onClick) {
        super(x, y, width, height);
        this.key = key;
        this.cached = cached;

        String searchQuery = (state == null) ? "" : state.getSearchQuery();

        if (live) {
            SpriteComponent background = new SpriteComponent(0, 0, width, height, getPageCardActiveTexture());
            this.addComponent(background);
        } else {
            WidgetSprites idleSprites = new WidgetSprites(
                    getPageCardIdleTexture(), getPageCardIdleTexture(), getPageCardIdleTexture());
            WidgetSprites activeSprites = new WidgetSprites(
                    getPageCardActiveTexture(), getPageCardActiveTexture(), getPageCardActiveTexture());

            PageCardButtonWidget background = new PageCardButtonWidget(
                    0, 0, width, height,
                    Component.empty(),
                    () -> idleSprites,
                    btn -> onClick.accept(key)
            );
            this.addWidget(background);
        }

        var font = Minecraft.getInstance().font;

        String displayName = key.displayName();
        int titleX = 3;
        int titleY = 3;
        int titleColor = key.isEnchanted() ? getEnchantedTitleColor() : getTitleTextColor();
        Component titleComp = Component.literal(displayName).withStyle(s -> s.withColor(titleColor));
        this.pageTitle = new TextComponent(titleX, titleY, titleComp);
        this.pageTitle.setDrawShadow(shouldDrawTitleShadow());
        this.addComponent(this.pageTitle);

        boolean hasFullItem = metaMap.values().stream().anyMatch(SackCache.SackItemMeta::isFull);
        if (hasFullItem) {
            TextComponent fullText = new TextComponent(width - font.width("FULL!") - 28, titleY, Component.translatable("enhanced_storage.ui.full"), 0xFFFF5555);
            fullText.setDrawShadow(true);
            this.addComponent(fullText);
        }

        if (key.type() != SackKey.Type.SACK_INDEX) {
            int closeX = width - 28;
            TextComponent closeBtn = new TextComponent(closeX, titleY, Component.literal("\u2715"), 0xFFFF6666);
            closeBtn.setDrawShadow(shouldDrawTitleShadow());
            this.addComponent(closeBtn);

            int handleX = width - 15;
            TextComponent dragHandle = new TextComponent(handleX, titleY, Component.literal("\u22ee\u22ee"), 0xFFAAAAAA);
            dragHandle.setDrawShadow(shouldDrawTitleShadow());
            this.addComponent(dragHandle);
        }

        if (cached || live) {
            int pageRows = 6;

            Map<Integer, ItemStack> displayMap = new HashMap<>();
            Map<Integer, SackCache.SackItemMeta> displayMetaMap = new HashMap<>();
            int navCount = 0;

            if (!live && items != null) {
                for (int i = 0; i < items.size(); i++) {
                    ItemStack stack = items.get(i);
                    if (stack == null || stack.isEmpty()) continue;
                    if (stack.getItem().toString().contains("stained_glass_pane")) {
                        continue;
                    }

                    if (SackContainerScreen.isNavigationItem(stack)) {
                        int col = getNavTargetCol(stack, navCount++);
                        displayMap.put(5 * 9 + col, stack);
                    } else if (i < 45) {
                        displayMap.put(i, stack);
                        if (metaMap != null && metaMap.containsKey(i)) {
                            displayMetaMap.put(i, metaMap.get(i));
                        }
                    }
                }
            }

            // The whole slot grid is one tiled sprite (18x18 tile mcmeta) — one draw call
            // instead of one per slot.
            SpriteComponent slotGrid = new SpriteComponent(
                    cardBorder, titleAreaHeight + 2,
                    slotsAcross * slotSize, pageRows * slotSize,
                    getStorageSlotTexture());
            this.addComponent(slotGrid);

            if (!live) {
                for (Map.Entry<Integer, ItemStack> entry : displayMap.entrySet()) {
                    int slotIndex = entry.getKey();
                    ItemStack stack = entry.getValue();
                    if (stack == null || stack.isEmpty()) continue;

                    int slotRow = slotIndex / slotsAcross;
                    int slotCol = slotIndex % slotsAcross;

                    StorageSlotComponent slotComponent = new StorageSlotComponent(
                            cardBorder + slotCol * slotSize,
                            (titleAreaHeight + 2) + slotRow * slotSize,
                            slotSize, slotSize,
                            getStorageSlotHighlightBackTexture(),
                            getStorageSlotHighlightFrontTexture(),
                            stack);
                    slotComponent.setTooltipEnabled(SackConfig.showItemTooltipsOnCachedSackItems);
                    if (!searchQuery.isBlank()) {
                        slotComponent.setSearchState(
                                ItemSearch.matches(stack, searchQuery)
                                        ? StorageSlotComponent.SearchState.MATCH
                                        : StorageSlotComponent.SearchState.NO_MATCH);
                    }
                    this.addComponent(slotComponent);
                }
            }
        } else {
            Component label = key.type() == SackKey.Type.SACK_INDEX
                    ? Component.translatable("enhanced_storage.ui.sack_go_back")
                    : Component.translatable("enhanced_storage.ui.click_to_open");
            int textX = (width - font.width(label)) / 2;
            int textY = (height - font.lineHeight) / 2;
            TextComponent pageInfo = new TextComponent(textX, textY, label, getTitleTextColor());
            pageInfo.setDrawShadow(shouldDrawTitleShadow());
            this.addComponent(pageInfo);
        }
    }

    public static String formatCompact(long number) {
        if (number >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fB", number / 1_000_000_000.0);
        }
        if (number >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", number / 1_000_000.0);
        }
        if (number >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fk", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    private static int getEnchantedTitleColor() {
        long time = System.currentTimeMillis();
        double cycle = (Math.sin(time / 150.0) + 1.0) / 2.0;

        return switch (SackConfig.sackBackgroundType) {
            case LIGHT -> {
                int r = (int) (0x5B + (0x99 - 0x5B) * cycle);
                int g = (int) (0x00 + (0x00 - 0x00) * cycle);
                int b = (int) (0x9A + (0xCC - 0x9A) * cycle);
                yield 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            default -> {
                int r = (int) (0xA0 + (0xFF - 0xA0) * cycle);
                int g = (int) (0x40 + (0x55 - 0x40) * cycle);
                int b = (int) (0xFF + (0xFF - 0xFF) * cycle);
                yield 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        };
    }

    private static int getTitleTextColor() {
        return switch (SackConfig.sackBackgroundType) {
            case LIGHT -> 0xFF000000;
            default -> 0xFFAAAAAA;
        };
    }

    private static boolean shouldDrawTitleShadow() {
        return SackConfig.sackBackgroundType != EnhancedStorageConfig.BackgroundType.LIGHT;
    }

    private static Identifier getPageCardIdleTexture() {
        return switch (SackConfig.sackBackgroundType) {
            case TRANSPARENT -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "transparent/page_card_idle_trans");
            case DARK -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "dark/page_card_idle_dark");
            default -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "light/page_card_idle");
        };
    }

    private static Identifier getPageCardActiveTexture() {
        return switch (SackConfig.sackBackgroundType) {
            case TRANSPARENT -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "transparent/page_card_active_trans");
            case DARK -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "dark/page_card_active_dark");
            default -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "light/page_card_active");
        };
    }

    private static Identifier getStorageSlotHighlightFrontTexture() {
        return Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "light/storage_slot_highlight_front");
    }

    private static Identifier getStorageSlotHighlightBackTexture() {
        return Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "light/storage_slot_highlight_back");
    }

    private Identifier getStorageSlotTexture() {
        return switch (SackConfig.sackBackgroundType) {
            case TRANSPARENT -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "transparent/storage_slot_trans");
            case DARK -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "dark/storage_slot_dark");
            default -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "light/storage_slot");
        };
    }

    private static int getNavTargetCol(ItemStack stack, int navIndex) {
        if (stack.is(Items.ARROW)) return 3;
        if (stack.is(Items.BARRIER)) return 4;
        if (stack.is(Items.CAULDRON)) return 5;
        if (stack.is(Items.CHEST)) return 6;
        String name = com.knutolof.helpbox.storage.util.TextUtils.stripText(stack.getHoverName()).trim().toLowerCase();
        if (name.contains("go back") || name.contains("previous")) return 3;
        if (name.contains("close")) return 4;
        if (name.contains("sort") || name.contains("search")) return 5;
        return Math.min(8, 3 + navIndex);
    }

    public SackKey getKey() {
        return key;
    }

    public boolean isCached() {
        return cached;
    }

    @Override
    public void extractRenderStateBase(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
        if (key.isEnchanted() && pageTitle != null) {
            pageTitle.setText(Component.literal(key.displayName()).withStyle(s -> s.withColor(getEnchantedTitleColor())));
        }
        for (IWidget widget : getWidgets()) {
            widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
        this.extractRenderState(guiGraphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);
        for (IComponent component : getComponents()) {
            component.extractRenderStateBase(guiGraphics, mouseX, mouseY, partialTick, getWidth(), getHeight());
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, int parentWidth, int parentHeight) {
    }
}
