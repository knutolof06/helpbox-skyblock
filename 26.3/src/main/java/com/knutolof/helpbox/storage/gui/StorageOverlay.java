package com.knutolof.helpbox.storage.gui;
import com.knutolof.helpbox.storage.StorageInitializer;

import com.daqem.uilib.gui.AbstractScreen;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;

import com.knutolof.helpbox.storage.storage.StorageCache;
import com.knutolof.helpbox.storage.storage.StorageKey;
import com.knutolof.helpbox.storage.util.ItemSearch;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class StorageOverlay extends AbstractScreen {

    private final StorageOverlayState state = StorageOverlayState.session();
    private final StorageOverlayLayout layout = new StorageOverlayLayout();
    private Deque<ItemStack> searchWarmupQueue;

    public StorageOverlay() {
        super(Component.literal("Storage Overlay"));
    }

    @Override
    protected void init() {
        layout.build(this, this.font, this.width, this.height,
                state, null, 0, this::onPageCardClicked, this::onSearchChanged, this::runToolkitCommand);

        // Queue every cached stack for search-text warmup; init() also runs on every rebuildWidgets, so only build the queue once per screen.
        if (searchWarmupQueue == null) {
            searchWarmupQueue = new ArrayDeque<>();
            StorageCache.getInstance().all().values()
                    .forEach(page -> searchWarmupQueue.addAll(page.items()));
        }

        state.onStorageScreenOpened();
    }

    @Override
    public void tick() {
        super.tick();
        if (searchWarmupQueue != null && !searchWarmupQueue.isEmpty()) {
            ItemSearch.warmUp(searchWarmupQueue, 3);
        }
    }

    // Called by PageCardComponent when a card is clicked.
    private void onPageCardClicked(StorageKey key) {

        if (Objects.equals(key, state.getOpenKey())) {
            return;
        }

        StorageInitializer.LOGGER.info("Storage page card clicked: {} (type={}, cached={})",
                key.displayName(), key.type(),
                StorageCache.getInstance().get(key).isPresent());

        state.setOpenKey(key);
    }

    private void onSearchChanged() {
        Minecraft.getInstance().schedule(() -> {
            this.rebuildWidgets();

            var box = layout.getSearchBox();
            if (box != null) {
                this.setFocused(box);
                box.setFocused(true);
            }
        });
    }

    private void runToolkitCommand(String command) {
        state.beginNavigation();
        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.connection.sendCommand(command);
    }

    @Override
    public void removed() {
        super.removed();
        state.onStorageScreenClosed();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        ScrollContainerWidget overview = layout.getPageOverview();
        if (overview != null && overview.mouseClicked(event, doubleClick)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ScrollContainerWidget overview = layout.getPageOverview();
        if (overview != null
                && mouseX >= overview.getX() && mouseX < overview.getX() + overview.getWidth()
                && mouseY >= overview.getY() && mouseY < overview.getY() + overview.getHeight()
                && overview.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}