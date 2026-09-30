package com.knutolof.helpbox.storage.gui;

import com.knutolof.helpbox.storage.config.SackConfig;
import com.knutolof.helpbox.storage.storage.SackKey;
import org.jspecify.annotations.Nullable;

public class SackOverlayState {

    private static final SackOverlayState SESSION = new SackOverlayState();
    private @Nullable SackKey openKey;
    private String searchQuery = "";
    private double scrollAmount = 0;
    private @Nullable SackKey pendingSackKey;
    private long navigatingUntil = 0;

    public static SackOverlayState session() {
        return SESSION;
    }

    public @Nullable SackKey consumePendingSackKey() {
        SackKey key = pendingSackKey;
        pendingSackKey = null;
        return key;
    }

    public void setPendingSackKey(@Nullable SackKey key) {
        this.pendingSackKey = key;
    }

    public double getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(double amount) {
        this.scrollAmount = amount;
    }

    public void beginNavigation() {
        navigatingUntil = System.currentTimeMillis() + 2000;
    }

    public boolean isNavigating() {
        return System.currentTimeMillis() < navigatingUntil;
    }

    public void onSackScreenOpened() {
        navigatingUntil = 0;
    }

    public void onStorageScreenOpened() {
        onSackScreenOpened();
    }

    public void onSackScreenClosed() {
        if (isNavigating()) {
            return;
        }
        openKey = null;
        searchQuery = "";
        if (!isNavigating()) {
            scrollAmount = 0;
        }
    }

    public void onStorageScreenClosed() {
        onSackScreenClosed();
    }

    public String getSearchQuery() {
        return searchQuery == null ? "" : searchQuery;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query;
    }

    public @Nullable SackKey getOpenKey() {
        return openKey;
    }

    public void setOpenKey(@Nullable SackKey openKey) {
        this.openKey = openKey;
    }

    public boolean isOpen(SackKey key) {
        return key.equals(openKey);
    }

    public boolean isSearching() {
        return searchQuery != null && !searchQuery.isBlank();
    }
}
