package com.knutolof.helpbox.storage.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class SackConfig extends MidnightConfig {

    @Comment(centered = true)
    public static Comment sectionSacksVisibility;

    @Entry
    public static boolean enableSackOverlay = true;

    @Entry
    public static boolean onlyOfficialSacks = true;

    @Entry
    public static boolean showSackOverviewCard = true;

    @Entry
    public static boolean showSackIndexPanel = true;

    @Entry
    public static boolean showSackSettingsButton = true;

    @Entry
    public static boolean showSackThemeButton = true;

    @Entry
    public static boolean showSackInsertInventoryButton = true;

    @Comment(centered = true)
    public static Comment sectionSacksAppearance;

    @Entry
    public static EnhancedStorageConfig.BackgroundType sackBackgroundType = EnhancedStorageConfig.BackgroundType.TRANSPARENT;

    @Entry(isSlider = true, min = 1, max = 10, precision = 1)
    public static int sackMaxPagePerRow = 3;

    @Entry(isSlider = true, min = 0, max = 10, precision = 1)
    public static int sackCardSpacing = 3;

    @Entry(isSlider = true, min = 0, max = 50, precision = 1)
    public static int sackHorizontalMargin = 15;

    @Entry(isSlider = true, min = 0, max = 50, precision = 1)
    public static int sackOverviewTopMargin = 15;

    @Entry(isSlider = true, min = 0, max = 50, precision = 1)
    public static int sackOverviewBottomMargin = 25;

    @Entry(isSlider = true, min = 0, max = 50, precision = 1)
    public static int extraTopAndBottomMarginForQuickNav = 10;

    @Entry(isSlider = true, min = 0, max = 50, precision = 1)
    public static int extraBottomMarginForRecipeSearchBar = 35;

    @Comment(centered = true)
    public static Comment sectionSacksBehavior;

    @Entry
    public static boolean showSackGoBackCard = false;

    @Entry
    public static boolean sackRememberSearchOnReopen = false;

    @Entry
    public static boolean sackRememberScrollOnReopen = false;

    @Entry
    public static EnhancedStorageConfig.AutoScrollMode autoScrollToOpenSack = EnhancedStorageConfig.AutoScrollMode.IF_PARTLY_HIDDEN;

    @Entry(isSlider = true, min = 1, max = 60, precision = 1)
    public static int sackOverlayScrollSpeed = 18;

    @Entry
    public static boolean saveSackCursorPosition = true;

    @Entry(isSlider = true, min = 0.01, max = 5, precision = 1000)
    public static double saveSackCursorPositionWindow = 0.5;

    @Comment(centered = true)
    public static Comment sectionSacksPerformance;

    @Entry
    public static boolean showItemTooltipsOnCachedSackItems = true;

    @Entry
    public static boolean fastCachedSackItemDecorations = true;

    @Comment(centered = true)
    public static Comment sectionSacksHypixelApi;

    @Entry
    public static String hypixelApiKey = "";

    @Entry
    public static boolean enableHypixelApiSync = true;
}
