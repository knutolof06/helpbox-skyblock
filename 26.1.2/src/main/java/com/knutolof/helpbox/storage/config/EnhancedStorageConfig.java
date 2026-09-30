package com.knutolof.helpbox.storage.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class EnhancedStorageConfig extends MidnightConfig {
    public static final String CATEGORY_GENERAL = "general";

    @Comment(category = CATEGORY_GENERAL, centered = true)
    public static Comment sectionVisibility;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean enableOverlay = true;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean enableRiftOverlay = true;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean showStorageOverviewCard = true;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean showToolkitButton = true;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean showSettingsButton = true;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean showThemeButton = true;

    @Comment(category = CATEGORY_GENERAL)
    public static Comment spacerAppearance;

    @Comment(category = CATEGORY_GENERAL, centered = true)
    public static Comment sectionAppearance;

    @Entry(category = CATEGORY_GENERAL)
    public static BackgroundType backgroundType = BackgroundType.TRANSPARENT;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 1, max = 10, precision = 1)
    public static int maxPagePerRow = 3;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 0, max = 10, precision = 1)
    public static int cardSpacing = 3;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 0, max = 50, precision = 1)
    public static int horizontalMargin = 15;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 0, max = 50, precision = 1)
    public static int overviewTopMargin = 15;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 0, max = 50, precision = 1)
    public static int overviewBottomMargin = 25;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 0, max = 50, precision = 1)
    public static int extraTopAndBottomMarginForQuickNav = 10;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 0, max = 50, precision = 1)
    public static int extraBottomMarginForRecipeSearchBar = 35;

    @Comment(category = CATEGORY_GENERAL)
    public static Comment spacerBehavior;

    @Comment(category = CATEGORY_GENERAL, centered = true)
    public static Comment sectionBehavior;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean rememberSearchOnReopen = false;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean rememberScrollOnReopen = false;

    @Entry(category = CATEGORY_GENERAL)
    public static AutoScrollMode autoScrollToOpenPage = AutoScrollMode.IF_PARTLY_HIDDEN;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 1, max = 60, precision = 1)
    public static int overlayScrollSpeed = 18;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean saveCursorPosition = true;

    @Entry(category = CATEGORY_GENERAL, isSlider = true, min = 0.01, max = 5, precision = 1000)
    public static double saveCursorPositionWindow = 0.5;

    @Comment(category = CATEGORY_GENERAL)
    public static Comment spacerPerformance;

    @Comment(category = CATEGORY_GENERAL, centered = true)
    public static Comment sectionPerformance;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean showItemTooltipsOnCachedItems = true;

    @Entry(category = CATEGORY_GENERAL)
    public static boolean fastCachedItemDecorations = true;

    public enum BackgroundType {
        LIGHT, DARK, TRANSPARENT
    }

    public enum AutoScrollMode {
        OFF,
        IF_PARTLY_HIDDEN,
        IF_FULLY_HIDDEN
    }
}
