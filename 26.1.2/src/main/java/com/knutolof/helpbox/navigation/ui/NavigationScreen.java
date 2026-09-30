package com.knutolof.helpbox.navigation.ui;

import com.knutolof.helpbox.navigation.CoordinateParser;
import com.knutolof.helpbox.navigation.WaypointManager;
import com.knutolof.helpbox.navigation.model.Waypoint;
import com.knutolof.helpbox.util.HelpBoxLang;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public class NavigationScreen extends Screen {

    private EditBox nameBox;
    private EditBox xBox, yBox, zBox;
    
    private double scrollYTop = 0;
    private double scrollYBottom = 0;
    private double maxScrollTop = 0;
    private double maxScrollBottom = 0;
    
    // Edit Overlay State
    private Waypoint editingMapLocation = null;
    private EditBox editNameBox, editXBox, editYBox, editZBox, editDescBox, editHexBox;
    private boolean editSyncEnabled = false;
    private boolean editIsEnabled = true;
    private String editSelectedIcon = "★";
    private String editSelectedHex = "#DDDDDD";
    
    private final String[] COLORS = {"#DDDDDD", "#FF3333", "#33FF33", "#3333FF", "#FFFF33", "#FF33FF", "#33FFFF"};
    private final String[] ICONS = {"★", "⚑", "♦", "●", "▲"};
    
    private String selectedColorHex = "#DDDDDD";
    private String selectedIcon = "★";

    // Minimap state
    private static final Identifier MAP_TEXTURE_LOCATION = Identifier.fromNamespaceAndPath("helpbox", "minimap_texture");
    private static DynamicTexture mapTexture = null;
    private static boolean mapTextureUpdating = false;
    private static final double[] ZOOM_LEVELS = {0.25, 0.5, 1.0, 2.0, 4.0};
    private static final String[] ZOOM_NAMES = {"0.25x", "0.5x", "1.0x", "2.0x", "4.0x"};
    private static int zoomIndex = 2; // Default 1.0x
    private static int lastZoomIndex = -1;
    private static int panX = 0;
    private static int panZ = 0;
    private static int lastPanX = Integer.MIN_VALUE;
    private static int lastPanZ = Integer.MIN_VALUE;

    // Minimap mouse drag state
    private boolean mapDragActive = false;
    private double mapDragStartX = 0;
    private double mapDragStartY = 0;
    private int mapDragInitialPanX = 0;
    private int mapDragInitialPanZ = 0;
    private int mapDragButton = 0;
    private boolean mapHasDragged = false;

    public NavigationScreen() {
        super(HelpBoxLang.tr("helpbox.ui.nav.title"));
    }

    private void playClickSound() {
        try {
            if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        } catch (Exception ignored) {}
    }

    private boolean isHovered(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    protected void init() {
        super.init();
        
        if (editingMapLocation != null) {
            initEditOverlay();
            return;
        }
        
        initMainUI();
        
        if (mapTexture == null && !mapTextureUpdating) {
            updateMapTexture();
        } else if ((lastZoomIndex != zoomIndex || lastPanX != panX || lastPanZ != panZ) && !mapTextureUpdating) {
            updateMapTexture();
        }
    }

    private void updateMapTexture() {
        if (mapTextureUpdating) return;
        mapTextureUpdating = true;
        Thread mapThread = new Thread(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc == null || mc.level == null || mc.player == null) {
                    mapTextureUpdating = false;
                    return;
                }
                
                int texSize = 256;
                double zoom = ZOOM_LEVELS[zoomIndex];
                double blocksPerPixel = 1.0 / zoom;
                int viewRadius = (int)(texSize / 2 * blocksPerPixel);
                
                int px = mc.player.getBlockX();
                int pz = mc.player.getBlockZ();
                int cx = px + panX;
                int cz = pz + panZ;
                
                lastZoomIndex = zoomIndex;
                lastPanX = panX;
                lastPanZ = panZ;
                
                NativeImage image = new NativeImage(texSize, texSize, false);
                
                int startWorldX = cx - viewRadius;
                int startWorldZ = cz - viewRadius;
                
                for (int x = 0; x < texSize; x++) {
                    int worldX = startWorldX + (int)(x * blocksPerPixel);
                    int chunkX = worldX >> 4;
                    int localX = worldX & 15;
                    
                    int prevY = Integer.MIN_VALUE;
                    
                    for (int z = 0; z < texSize; z++) {
                        int worldZ = startWorldZ + (int)(z * blocksPerPixel);
                        int chunkZ = worldZ >> 4;
                        int localZ = worldZ & 15;
                        
                        if (!mc.level.hasChunk(chunkX, chunkZ)) {
                            // Unloaded chunk: tech radar grid
                            boolean isGrid = (worldX % 16 == 0) || (worldZ % 16 == 0);
                            image.setPixel(x, z, isGrid ? 0xFF1E2638 : 0xFF111622);
                            prevY = Integer.MIN_VALUE;
                            continue;
                        }
                        
                        LevelChunk chunk = mc.level.getChunk(chunkX, chunkZ);
                        if (chunk == null) {
                            image.setPixel(x, z, 0xFF111622);
                            prevY = Integer.MIN_VALUE;
                            continue;
                        }
                        
                        int foundY = Integer.MIN_VALUE;
                        MapColor foundColor = null;
                        
                        // 1. Try Heightmap
                        int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
                        if (topY > mc.level.getMinY()) {
                            BlockPos bPos = new BlockPos(worldX, topY - 1, worldZ);
                            BlockState bState = chunk.getBlockState(bPos);
                            if (!bState.isAir()) {
                                MapColor mcCol = bState.getMapColor(mc.level, bPos);
                                if (mcCol != null && mcCol != MapColor.NONE && mcCol.col != 0) {
                                    foundY = topY - 1;
                                    foundColor = mcCol;
                                }
                            }
                        }
                        
                        // 2. Fall back to scanning chunk sections
                        if (foundColor == null) {
                            LevelChunkSection[] sections = chunk.getSections();
                            int highestSection = chunk.getHighestFilledSectionIndex();
                            if (highestSection >= 0 && highestSection < sections.length) {
                                for (int s = highestSection; s >= 0; s--) {
                                    LevelChunkSection sec = sections[s];
                                    if (sec == null || sec.hasOnlyAir()) continue;
                                    
                                    int secBaseY = chunk.getSectionYFromSectionIndex(s) << 4;
                                    for (int ly = 15; ly >= 0; ly--) {
                                        BlockState bState = sec.getBlockState(localX, ly, localZ);
                                        if (!bState.isAir()) {
                                            int curY = secBaseY + ly;
                                            BlockPos bPos = new BlockPos(worldX, curY, worldZ);
                                            MapColor mcCol = bState.getMapColor(mc.level, bPos);
                                            if (mcCol != null && mcCol != MapColor.NONE && mcCol.col != 0) {
                                                foundY = curY;
                                                foundColor = mcCol;
                                                break;
                                            }
                                        }
                                    }
                                    if (foundColor != null) break;
                                }
                            }
                        }
                        
                        if (foundColor != null) {
                            MapColor.Brightness brightness = MapColor.Brightness.NORMAL;
                            if (prevY != Integer.MIN_VALUE) {
                                if (foundY > prevY) brightness = MapColor.Brightness.HIGH;
                                else if (foundY < prevY) brightness = MapColor.Brightness.LOW;
                            }
                            prevY = foundY;
                            int argb = foundColor.calculateARGBColor(brightness);
                            image.setPixel(x, z, argb);
                        } else {
                            // Skyblock void column within loaded chunk
                            prevY = Integer.MIN_VALUE;
                            boolean isVoidGrid = (worldX % 8 == 0) || (worldZ % 8 == 0);
                            image.setPixel(x, z, isVoidGrid ? 0xFF141923 : 0xFF0D1117);
                        }
                    }
                }
                
                mc.execute(() -> {
                    try {
                        if (mapTexture != null) {
                            mapTexture.close();
                        }
                        mapTexture = new DynamicTexture(() -> "minimap", image);
                        mc.getTextureManager().register(MAP_TEXTURE_LOCATION, mapTexture);
                    } catch (Exception ignored) {
                    } finally {
                        mapTextureUpdating = false;
                    }
                });
            } catch (Exception e) {
                mapTextureUpdating = false;
            }
        });
        mapThread.setDaemon(true);
        mapThread.setName("HelpBox-MinimapThread");
        mapThread.start();
    }
    
    private void initEditOverlay() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int w = 240;
        int h = 260;
        int startX = cx - w / 2;
        int startY = cy - h / 2;
        
        editNameBox = new EditBox(this.font, startX + 10, startY + 30, 220, 20, HelpBoxLang.tr("helpbox.ui.nav.name"));
        editNameBox.setValue(editingMapLocation.getName());
        this.addRenderableWidget(editNameBox);
        
        editXBox = new EditBox(this.font, startX + 10, startY + 60, 60, 20, Component.literal("X"));
        editYBox = new EditBox(this.font, startX + 80, startY + 60, 60, 20, Component.literal("Y"));
        editZBox = new EditBox(this.font, startX + 150, startY + 60, 60, 20, Component.literal("Z"));
        editXBox.setValue(String.valueOf(editingMapLocation.getX()));
        editYBox.setValue(String.valueOf(editingMapLocation.getY()));
        editZBox.setValue(String.valueOf(editingMapLocation.getZ()));
        this.addRenderableWidget(editXBox);
        this.addRenderableWidget(editYBox);
        this.addRenderableWidget(editZBox);
        
        editDescBox = new EditBox(this.font, startX + 10, startY + 90, 220, 20, HelpBoxLang.tr("helpbox.ui.nav.desc"));
        editDescBox.setValue(editingMapLocation.getDescription());
        editDescBox.setHint(HelpBoxLang.tr("helpbox.ui.nav.hint_desc"));
        this.addRenderableWidget(editDescBox);
        
        editHexBox = new EditBox(this.font, startX + 10, startY + 120, 80, 20, Component.literal("Hex"));
        editSelectedHex = editingMapLocation.getColorHex();
        editHexBox.setValue(editSelectedHex);
        editHexBox.setResponder(val -> { editSelectedHex = val; });
        this.addRenderableWidget(editHexBox);
        
        Button rndColorBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_random"), btn -> {
            int rgb = java.awt.Color.HSBtoRGB((float)Math.random(), 0.8f, 1.0f);
            editSelectedHex = String.format("#%06X", (0xFFFFFF & rgb));
            editHexBox.setValue(editSelectedHex);
        }).bounds(startX + 130, startY + 120, 100, 20).build();
        this.addRenderableWidget(rndColorBtn);
        
        editSelectedIcon = editingMapLocation.getIconName();
        editIsEnabled = editingMapLocation.isEnabled();
        
        Button toggleBtn = Button.builder(Component.literal(""), btn -> {
            editIsEnabled = !editIsEnabled;
            btn.setMessage(HelpBoxLang.tr("helpbox.ui.nav.show_in_world", (editIsEnabled ? "ON" : "OFF")));
        }).bounds(startX + 10, startY + 180, 140, 20).build();
        toggleBtn.setMessage(HelpBoxLang.tr("helpbox.ui.nav.show_in_world", (editIsEnabled ? "ON" : "OFF")));
        this.addRenderableWidget(toggleBtn);
        
        Button resetBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_reset"), btn -> {
            this.rebuildWidgets();
        }).bounds(startX + 160, startY + 180, 70, 20).build();
        this.addRenderableWidget(resetBtn);
        
        Button delBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_delete"), btn -> {
            WaypointManager.removeWaypoint(editingMapLocation.getId());
            editingMapLocation = null;
            this.rebuildWidgets();
        }).bounds(startX + 10, startY + h - 30, 60, 20).build();
        this.addRenderableWidget(delBtn);
        
        Button saveBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_save"), btn -> {
            try {
                editingMapLocation.setName(editNameBox.getValue());
                editingMapLocation.setX(Integer.parseInt(editXBox.getValue()));
                editingMapLocation.setY(Integer.parseInt(editYBox.getValue()));
                editingMapLocation.setZ(Integer.parseInt(editZBox.getValue()));
                editingMapLocation.setDescription(editDescBox.getValue());
                editingMapLocation.setColorHex(editSelectedHex);
                editingMapLocation.setIconName(editSelectedIcon);
                editingMapLocation.setEnabled(editIsEnabled);
                WaypointManager.updateWaypoint(editingMapLocation); 
                editingMapLocation = null;
                this.rebuildWidgets();
            } catch (Exception ignored) {}
        }).bounds(startX + 80, startY + h - 30, 80, 20).build();
        this.addRenderableWidget(saveBtn);
        
        Button cancelBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_cancel"), btn -> {
            editingMapLocation = null;
            this.rebuildWidgets();
        }).bounds(startX + 170, startY + h - 30, 60, 20).build();
        this.addRenderableWidget(cancelBtn);
    }
    
    private void initMainUI() {
        int rightPanelX = this.width / 2 + 10;
        int rightPanelY = 50;

        nameBox = new EditBox(this.font, rightPanelX + 10, rightPanelY + 30, 160, 20, HelpBoxLang.tr("helpbox.ui.nav.name"));
        nameBox.setHint(HelpBoxLang.tr("helpbox.ui.nav.hint_name"));
        this.addRenderableWidget(nameBox);
        
        Button directPasteBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.paste_clipboard"), btn -> {
            try {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                BlockPos pos = CoordinateParser.parse(clipboard);
                xBox.setValue(String.valueOf(pos.getX()));
                yBox.setValue(String.valueOf(pos.getY()));
                zBox.setValue(String.valueOf(pos.getZ()));
            } catch (Exception ignored) {}
        }).bounds(rightPanelX + 10, rightPanelY + 60, 160, 20).build();
        this.addRenderableWidget(directPasteBtn);

        xBox = new EditBox(this.font, rightPanelX + 10, rightPanelY + 90, 45, 20, Component.literal("X"));
        yBox = new EditBox(this.font, rightPanelX + 65, rightPanelY + 90, 45, 20, Component.literal("Y"));
        zBox = new EditBox(this.font, rightPanelX + 120, rightPanelY + 90, 50, 20, Component.literal("Z"));
        this.addRenderableWidget(xBox);
        this.addRenderableWidget(yBox);
        this.addRenderableWidget(zBox);

        Button addNavBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_nav_target"), btn -> {
            try {
                int x = Integer.parseInt(xBox.getValue());
                int y = Integer.parseInt(yBox.getValue());
                int z = Integer.parseInt(zBox.getValue());
                String name = nameBox.getValue().isEmpty() ? HelpBoxLang.str("helpbox.ui.nav.default_waypoint") : nameBox.getValue();
                
                Waypoint wp = new Waypoint(name, x, y, z, selectedColorHex, selectedIcon, false);
                WaypointManager.addWaypoint(wp);
                
                nameBox.setValue(""); xBox.setValue(""); yBox.setValue(""); zBox.setValue("");
                this.rebuildWidgets();
            } catch (Exception ignored) {}
        }).bounds(rightPanelX + 10, rightPanelY + 160, 75, 20).build();
        this.addRenderableWidget(addNavBtn);
        
        Button addMapBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_map_location"), btn -> {
            try {
                int x = Integer.parseInt(xBox.getValue());
                int y = Integer.parseInt(yBox.getValue());
                int z = Integer.parseInt(zBox.getValue());
                String name = nameBox.getValue().isEmpty() ? HelpBoxLang.str("helpbox.ui.nav.default_map") : nameBox.getValue();
                
                Waypoint wp = new Waypoint(name, x, y, z, selectedColorHex, selectedIcon, true);
                WaypointManager.addWaypoint(wp);
                
                nameBox.setValue(""); xBox.setValue(""); yBox.setValue(""); zBox.setValue("");
                this.rebuildWidgets();
            } catch (Exception ignored) {}
        }).bounds(rightPanelX + 95, rightPanelY + 160, 75, 20).build();
        this.addRenderableWidget(addMapBtn);

        // Left Panel - Dual Lists
        int listWidth = this.width / 2 - 20;
        int topListStartY = 50;
        int topListHeight = (this.height - 60) / 2 - 10;
        
        List<Waypoint> standardWaypoints = WaypointManager.getWaypoints().stream().filter(w -> !w.isMapLocation()).collect(Collectors.toList());
        maxScrollTop = Math.max(0, standardWaypoints.size() * 35 - topListHeight);
        
        for (int i = 0; i < standardWaypoints.size(); i++) {
            Waypoint wp = standardWaypoints.get(i);
            int itemY = topListStartY + i * 35 - (int)scrollYTop;
            
            if (itemY > topListStartY - 10 && itemY < topListStartY + topListHeight - 20) {
                Button navBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_go"), btn -> {
                    WaypointManager.setActiveWaypoint(wp.getId());
                }).bounds(listWidth - 105, itemY + 5, 35, 20).build();
                this.addRenderableWidget(navBtn);
                
                Button stopBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_stop"), btn -> {
                    WaypointManager.clearActiveWaypoint();
                    this.rebuildWidgets();
                }).bounds(listWidth - 65, itemY + 5, 35, 20).build();
                this.addRenderableWidget(stopBtn);
                
                Button delBtn = Button.builder(Component.literal("✕"), btn -> {
                    WaypointManager.removeWaypoint(wp.getId());
                    this.rebuildWidgets();
                }).bounds(listWidth - 25, itemY + 5, 20, 20).build();
                this.addRenderableWidget(delBtn);
            }
        }
        
        int bottomListStartY = topListStartY + topListHeight + 30;
        int bottomListHeight = (this.height - 60) / 2 - 20;
        
        List<Waypoint> mapWaypoints = WaypointManager.getWaypoints().stream().filter(w -> w.isMapLocation()).collect(Collectors.toList());
        maxScrollBottom = Math.max(0, mapWaypoints.size() * 35 - bottomListHeight);
        
        for (int i = 0; i < mapWaypoints.size(); i++) {
            Waypoint wp = mapWaypoints.get(i);
            int itemY = bottomListStartY + i * 35 - (int)scrollYBottom;
            
            if (itemY > bottomListStartY - 10 && itemY < bottomListStartY + bottomListHeight - 20) {
                Button editBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_edit"), btn -> {
                    editingMapLocation = wp;
                    this.rebuildWidgets();
                }).bounds(listWidth - 145, itemY + 5, 35, 20).build();
                this.addRenderableWidget(editBtn);
                
                Button navBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_go"), btn -> {
                    WaypointManager.setActiveWaypoint(wp.getId());
                }).bounds(listWidth - 105, itemY + 5, 35, 20).build();
                this.addRenderableWidget(navBtn);
                
                Button stopBtn = Button.builder(HelpBoxLang.tr("helpbox.ui.nav.btn_stop"), btn -> {
                    WaypointManager.clearActiveWaypoint();
                    this.rebuildWidgets();
                }).bounds(listWidth - 65, itemY + 5, 35, 20).build();
                this.addRenderableWidget(stopBtn);
                
                Button delBtn = Button.builder(Component.literal("✕"), btn -> {
                    WaypointManager.removeWaypoint(wp.getId());
                    this.rebuildWidgets();
                }).bounds(listWidth - 25, itemY + 5, 20, 20).build();
                this.addRenderableWidget(delBtn);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (editingMapLocation != null) return true;
        
        int rightPanelX = this.width / 2 + 10;
        int rightPanelWidth = this.width / 2 - 20;
        int mapAreaY = 250;
        int mapAreaHeight = this.height - mapAreaY - 10;
        int mapDrawSize = Math.min(rightPanelWidth - 20, mapAreaHeight - 32);
        int mapX = rightPanelX + (rightPanelWidth - mapDrawSize) / 2;
        int mapY = mapAreaY + 18;
        
        if (mouseX >= mapX && mouseX <= mapX + mapDrawSize && mouseY >= mapY && mouseY <= mapY + mapDrawSize) {
            if (scrollY > 0) { // scroll UP = zoom in
                if (zoomIndex < ZOOM_LEVELS.length - 1) {
                    zoomIndex++;
                    updateMapTexture();
                    playClickSound();
                }
            } else if (scrollY < 0) { // scroll DOWN = zoom out
                if (zoomIndex > 0) {
                    zoomIndex--;
                    updateMapTexture();
                    playClickSound();
                }
            }
            return true;
        }
        
        int topListStartY = 50;
        int topListHeight = (this.height - 60) / 2 - 10;
        int bottomListStartY = topListStartY + topListHeight + 30;
        int bottomListHeight = (this.height - 60) / 2 - 20;
        
        if (mouseX < this.width / 2) {
            if (mouseY >= topListStartY && mouseY <= topListStartY + topListHeight) {
                this.scrollYTop -= scrollY * 20;
                this.scrollYTop = Math.max(0, Math.min(this.scrollYTop, maxScrollTop));
                this.rebuildWidgets();
                return true;
            } else if (mouseY >= bottomListStartY && mouseY <= bottomListStartY + bottomListHeight) {
                this.scrollYBottom -= scrollY * 20;
                this.scrollYBottom = Math.max(0, Math.min(this.scrollYBottom, maxScrollBottom));
                this.rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean handled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        
        if (editingMapLocation != null) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            int startX = cx - 120;
            int startY = cy - 130;
            
            if (mouseX >= startX + 160 && mouseX <= startX + 170 && mouseY >= startY + 145 && mouseY <= startY + 155) {
                editSyncEnabled = !editSyncEnabled;
                if (editSyncEnabled && this.minecraft.player != null) {
                    BlockPos p = this.minecraft.player.blockPosition();
                    editXBox.setValue(String.valueOf(p.getX()));
                    editYBox.setValue(String.valueOf(p.getY()));
                    editZBox.setValue(String.valueOf(p.getZ()));
                }
                return true;
            }
            
            for (int i = 0; i < ICONS.length; i++) {
                int icx = startX + 45 + (i * 22);
                int icy = startY + 165;
                if (mouseX >= icx && mouseX <= icx + 18 && mouseY >= icy && mouseY <= icy + 18) {
                    editSelectedIcon = ICONS[i];
                    return true;
                }
            }
            return super.mouseClicked(event, handled);
        }
        
        int rightPanelX = this.width / 2 + 10;
        int rightPanelY = 50;
        
        for (int i = 0; i < COLORS.length; i++) {
            int cx = rightPanelX + 10 + (i * 22);
            int cy = rightPanelY + 120;
            if (mouseX >= cx && mouseX <= cx + 18 && mouseY >= cy && mouseY <= cy + 18) {
                selectedColorHex = COLORS[i];
                return true;
            }
        }
        
        for (int i = 0; i < ICONS.length; i++) {
            int cx = rightPanelX + 10 + (i * 22);
            int cy = rightPanelY + 140;
            if (mouseX >= cx && mouseX <= cx + 18 && mouseY >= cy && mouseY <= cy + 18) {
                selectedIcon = ICONS[i];
                return true;
            }
        }
        
        // Minimap Header Buttons
        int rightPanelWidth = this.width / 2 - 20;
        int mapAreaY = 250;
        int btnRefreshX = rightPanelX + rightPanelWidth - 116;
        int btnZoomOutX = rightPanelX + rightPanelWidth - 96;
        int btnZoomInX  = rightPanelX + rightPanelWidth - 44;
        int btnRecenterX= rightPanelX + rightPanelWidth - 24;
        int btnY = mapAreaY + 2;
        int btnH = 13;
        
        if (mouseY >= btnY && mouseY <= btnY + btnH) {
            if (mouseX >= btnRefreshX && mouseX <= btnRefreshX + 16) {
                updateMapTexture();
                playClickSound();
                return true;
            }
            if (mouseX >= btnZoomOutX && mouseX <= btnZoomOutX + 16) {
                if (zoomIndex > 0) {
                    zoomIndex--;
                    updateMapTexture();
                    playClickSound();
                }
                return true;
            }
            if (mouseX >= btnZoomInX && mouseX <= btnZoomInX + 16) {
                if (zoomIndex < ZOOM_LEVELS.length - 1) {
                    zoomIndex++;
                    updateMapTexture();
                    playClickSound();
                }
                return true;
            }
            if (mouseX >= btnRecenterX && mouseX <= btnRecenterX + 18) {
                panX = 0;
                panZ = 0;
                updateMapTexture();
                playClickSound();
                return true;
            }
        }
        
        // Minimap Drag/Click Start
        int mapAreaHeight = this.height - mapAreaY - 10;
        int mapDrawSize = Math.min(rightPanelWidth - 20, mapAreaHeight - 32);
        int mapX = rightPanelX + (rightPanelWidth - mapDrawSize) / 2;
        int mapY = mapAreaY + 18;
        
        if (mouseX >= mapX && mouseX <= mapX + mapDrawSize && mouseY >= mapY && mouseY <= mapY + mapDrawSize) {
            mapDragActive = true;
            mapDragStartX = mouseX;
            mapDragStartY = mouseY;
            mapDragInitialPanX = panX;
            mapDragInitialPanZ = panZ;
            mapDragButton = button;
            mapHasDragged = false;
            return true;
        }
        
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
        if (mapDragActive) {
            double totalDeltaX = event.x() - mapDragStartX;
            double totalDeltaY = event.y() - mapDragStartY;
            if (Math.abs(totalDeltaX) > 3 || Math.abs(totalDeltaY) > 3) {
                mapHasDragged = true;
            }
            if (mapHasDragged) {
                int rightPanelWidth = this.width / 2 - 20;
                int mapAreaY = 250;
                int mapAreaHeight = this.height - mapAreaY - 10;
                int mapDrawSize = Math.min(rightPanelWidth - 20, mapAreaHeight - 32);
                
                double zoom = ZOOM_LEVELS[zoomIndex];
                double blocksPerPixel = 1.0 / zoom;
                double screenToWorld = (256.0 * blocksPerPixel) / mapDrawSize;
                
                panX = mapDragInitialPanX - (int)(totalDeltaX * screenToWorld);
                panZ = mapDragInitialPanZ - (int)(totalDeltaY * screenToWorld);
                return true;
            }
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (mapDragActive) {
            mapDragActive = false;
            if (mapHasDragged) {
                updateMapTexture();
                return true;
            } else {
                handleMinimapClick(event.x(), event.y(), mapDragButton);
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    private void handleMinimapClick(double mouseX, double mouseY, int button) {
        int rightPanelX = this.width / 2 + 10;
        int rightPanelWidth = this.width / 2 - 20;
        int mapAreaY = 250;
        int mapAreaHeight = this.height - mapAreaY - 10;
        int mapDrawSize = Math.min(rightPanelWidth - 20, mapAreaHeight - 32);
        int mapX = rightPanelX + (rightPanelWidth - mapDrawSize) / 2;
        int mapY = mapAreaY + 18;
        
        if (this.minecraft != null && this.minecraft.player != null) {
            int px = this.minecraft.player.getBlockX();
            int pz = this.minecraft.player.getBlockZ();
            int mapWorldCenterX = px + panX;
            int mapWorldCenterZ = pz + panZ;
            
            double zoom = ZOOM_LEVELS[zoomIndex];
            double viewRadius = 128.0 / zoom;
            
            double normX = (mouseX - (mapX + mapDrawSize / 2.0)) / (mapDrawSize / 2.0);
            double normZ = (mouseY - (mapY + mapDrawSize / 2.0)) / (mapDrawSize / 2.0);
            int worldX = mapWorldCenterX + (int)(normX * viewRadius);
            int worldZ = mapWorldCenterZ + (int)(normZ * viewRadius);
            
            if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                Waypoint wp = new Waypoint("Map Location", worldX, this.minecraft.player.getBlockY(), worldZ, selectedColorHex, selectedIcon, true);
                WaypointManager.addWaypoint(wp);
                this.rebuildWidgets();
                playClickSound();
            } else {
                xBox.setValue(String.valueOf(worldX));
                yBox.setValue(String.valueOf(this.minecraft.player.getBlockY()));
                zBox.setValue(String.valueOf(worldZ));
                nameBox.setValue("Map Location");
                playClickSound();
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        
        if (editingMapLocation != null) {
            graphics.fill(0, 0, this.width, this.height, 0xD0000000);
            drawEditOverlay(graphics);
            return;
        }
        
        graphics.fill(0, 0, this.width, this.height, 0xD0000000);
        
        graphics.fill(0, 0, this.width, 30, 0xFF000000);
        graphics.fill(0, 30, this.width, 31, 0xFF555555);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.title"), 10, 10, 0xFFFFFFFF);
        
        if (this.minecraft.player != null) {
            boolean isGpsActive = WaypointManager.getActiveWaypoint() != null;
            int dotColor = isGpsActive ? 0xFF00FF00 : 0xFFFF0000;
            graphics.fill(this.width - 150, 12, this.width - 144, 18, dotColor);
            BlockPos pos = this.minecraft.player.blockPosition();
            graphics.text(this.font, String.format("Pos: %d, %d, %d", pos.getX(), pos.getY(), pos.getZ()), this.width - 130, 10, 0xFFAAAAAA);
        }
        
        int leftWidth = this.width / 2 - 10;
        int topListStartY = 50;
        int topListHeight = (this.height - 60) / 2 - 10;
        int bottomListStartY = topListStartY + topListHeight + 30;
        int bottomListHeight = (this.height - 60) / 2 - 20;
        
        RenderHelper.fillFlatRect(graphics, 10, topListStartY - 10, leftWidth - 10, topListHeight, 0xAA000000, 0xFF333333);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.active_targets"), 15, topListStartY - 8, 0xFF00FF00);
        
        RenderHelper.fillFlatRect(graphics, 10, bottomListStartY - 10, leftWidth - 10, bottomListHeight, 0xAA000000, 0xFF333333);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.map_locations"), 15, bottomListStartY - 8, 0xFF00FFFF);
        
        graphics.enableScissor(11, topListStartY + 3, leftWidth - 1, topListStartY - 10 + topListHeight - 1);
        List<Waypoint> standardWaypoints = WaypointManager.getWaypoints().stream().filter(w -> !w.isMapLocation()).collect(Collectors.toList());
        for (int i = 0; i < standardWaypoints.size(); i++) {
            Waypoint wp = standardWaypoints.get(i);
            int itemY = topListStartY + i * 35 - (int)scrollYTop;
            if (itemY > 0 && itemY < this.height) {
                Waypoint activeWp = WaypointManager.getActiveWaypoint();
                boolean isActive = activeWp != null && wp.getId().equals(activeWp.getId());
                int bgColor = isActive ? 0xAA113311 : 0x00000000;
                RenderHelper.fillFlatRect(graphics, 15, itemY, leftWidth - 25, 30, bgColor, isActive ? 0xFF338833 : 0xFF222222);
                try {
                    int col = java.awt.Color.decode(wp.getColorHex()).getRGB() | 0xFF000000;
                    graphics.fill(20, itemY + 10, 30, itemY + 20, col);
                } catch (Exception ignored) {}
                String displayIcon = wp.getIconName() != null ? wp.getIconName() : "•";
                graphics.text(this.font, displayIcon + " " + wp.getName(), 36, itemY + 6, 0xFFFFFFFF);
                graphics.text(this.font, wp.getX() + ", " + wp.getY() + ", " + wp.getZ(), 36, itemY + 16, 0xFFAAAAAA);
            }
        }
        graphics.disableScissor();
        
        graphics.enableScissor(11, bottomListStartY + 3, leftWidth - 1, bottomListStartY - 10 + bottomListHeight - 1);
        List<Waypoint> mapWaypoints = WaypointManager.getWaypoints().stream().filter(w -> w.isMapLocation()).collect(Collectors.toList());
        for (int i = 0; i < mapWaypoints.size(); i++) {
            Waypoint wp = mapWaypoints.get(i);
            int itemY = bottomListStartY + i * 35 - (int)scrollYBottom;
            if (itemY > 0 && itemY < this.height) {
                Waypoint activeWp = WaypointManager.getActiveWaypoint();
                boolean isActive = activeWp != null && wp.getId().equals(activeWp.getId());
                int bgColor = isActive ? 0xAA113311 : 0x00000000;
                RenderHelper.fillFlatRect(graphics, 15, itemY, leftWidth - 25, 30, bgColor, isActive ? 0xFF338833 : 0xFF222222);
                try {
                    int col = java.awt.Color.decode(wp.getColorHex()).getRGB() | 0xFF000000;
                    graphics.fill(20, itemY + 10, 30, itemY + 20, col);
                } catch (Exception ignored) {}
                String displayIcon = wp.getIconName() != null ? wp.getIconName() : "•";
                graphics.text(this.font, displayIcon + " " + wp.getName() + (wp.isEnabled() ? "" : " " + HelpBoxLang.str("helpbox.ui.nav.hidden")), 36, itemY + 6, wp.isEnabled() ? 0xFFFFFFFF : 0xFF888888);
                graphics.text(this.font, wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + " " + wp.getDescription(), 36, itemY + 16, 0xFFAAAAAA);
            }
        }
        graphics.disableScissor();
        
        int rightPanelX = this.width / 2 + 10;
        int rightPanelY = 50;
        
        RenderHelper.fillFlatRect(graphics, rightPanelX, rightPanelY, this.width / 2 - 20, 190, 0xAA000000, 0xFF333333);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.add_new"), rightPanelX + 10, rightPanelY + 10, 0xFFFFFFFF);
        
        for (int i = 0; i < COLORS.length; i++) {
            int cx = rightPanelX + 10 + (i * 22);
            int cy = rightPanelY + 120;
            int color = java.awt.Color.decode(COLORS[i]).getRGB() | 0xFF000000;
            graphics.fill(cx, cy, cx + 18, cy + 18, color);
            if (COLORS[i].equals(selectedColorHex)) graphics.fill(cx + 4, cy + 4, cx + 14, cy + 14, 0xFFFFFFFF);
        }
        for (int i = 0; i < ICONS.length; i++) {
            int cx = rightPanelX + 10 + (i * 22);
            int cy = rightPanelY + 140;
            int bgColor = ICONS[i].equals(selectedIcon) ? 0xFF666666 : 0xFF333333;
            graphics.fill(cx, cy, cx + 18, cy + 18, bgColor);
            graphics.text(this.font, ICONS[i], cx + 5, cy + 5, 0xFFFFFFFF);
        }
        
        drawMinimap(graphics, mouseX, mouseY);
    }
    
    private void drawMinimap(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int rightPanelX = this.width / 2 + 10;
        int rightPanelWidth = this.width / 2 - 20;
        
        int mapAreaY = 250;
        int mapAreaHeight = this.height - mapAreaY - 10;
        
        // Draw outer panel container
        RenderHelper.fillFlatRect(graphics, rightPanelX, mapAreaY, rightPanelWidth, mapAreaHeight, 0xCC0D1117, 0xFF30363D);
        
        // Header title
        graphics.text(this.font, "🗺 " + HelpBoxLang.str("helpbox.ui.nav.minimap", ZOOM_NAMES[zoomIndex]), rightPanelX + 8, mapAreaY + 5, 0xFF58A6FF);
        
        // Header buttons (Refresh, Zoom Out, Zoom Badge, Zoom In, Recenter)
        int btnRefreshX = rightPanelX + rightPanelWidth - 116;
        int btnZoomOutX = rightPanelX + rightPanelWidth - 96;
        int badgeZoomX  = rightPanelX + rightPanelWidth - 76;
        int btnZoomInX  = rightPanelX + rightPanelWidth - 44;
        int btnRecenterX= rightPanelX + rightPanelWidth - 24;
        int btnY = mapAreaY + 2;
        int btnH = 13;
        
        // Refresh button [ ⟳ ]
        boolean hovRefresh = isHovered(mouseX, mouseY, btnRefreshX, btnY, 16, btnH);
        RenderHelper.fillFlatRect(graphics, btnRefreshX, btnY, 16, btnH, hovRefresh ? 0xFF238636 : 0xFF21262D, hovRefresh ? 0xFF3FB950 : 0xFF30363D);
        graphics.centeredText(this.font, "⟳", btnRefreshX + 8, btnY + 2, 0xFFFFFFFF);
        
        // Zoom Out button [ - ]
        boolean hovZoomOut = isHovered(mouseX, mouseY, btnZoomOutX, btnY, 16, btnH);
        RenderHelper.fillFlatRect(graphics, btnZoomOutX, btnY, 16, btnH, hovZoomOut ? 0xFF388BFD : 0xFF21262D, hovZoomOut ? 0xFF58A6FF : 0xFF30363D);
        graphics.centeredText(this.font, "-", btnZoomOutX + 8, btnY + 2, zoomIndex > 0 ? 0xFFFFFFFF : 0xFF666666);
        
        // Zoom Level badge [ 1.0x ]
        RenderHelper.fillFlatRect(graphics, badgeZoomX, btnY, 28, btnH, 0xFF161B22, 0xFF30363D);
        graphics.centeredText(this.font, ZOOM_NAMES[zoomIndex], badgeZoomX + 14, btnY + 2, 0xFF58A6FF);
        
        // Zoom In button [ + ]
        boolean hovZoomIn = isHovered(mouseX, mouseY, btnZoomInX, btnY, 16, btnH);
        RenderHelper.fillFlatRect(graphics, btnZoomInX, btnY, 16, btnH, hovZoomIn ? 0xFF388BFD : 0xFF21262D, hovZoomIn ? 0xFF58A6FF : 0xFF30363D);
        graphics.centeredText(this.font, "+", btnZoomInX + 8, btnY + 2, zoomIndex < ZOOM_LEVELS.length - 1 ? 0xFFFFFFFF : 0xFF666666);
        
        // Recenter button [ ⌖ ]
        boolean hasPan = panX != 0 || panZ != 0;
        boolean hovRecenter = isHovered(mouseX, mouseY, btnRecenterX, btnY, 18, btnH);
        int recBg = hovRecenter ? 0xFFD29922 : (hasPan ? 0xFF3A3010 : 0xFF21262D);
        int recBorder = hovRecenter ? 0xFFF0883E : (hasPan ? 0xFFD29922 : 0xFF30363D);
        RenderHelper.fillFlatRect(graphics, btnRecenterX, btnY, 18, btnH, recBg, recBorder);
        graphics.centeredText(this.font, "⌖", btnRecenterX + 9, btnY + 2, hasPan ? 0xFFFFAA00 : 0xFF888888);
        
        // Map dimensions inside box
        int mapDrawSize = Math.min(rightPanelWidth - 20, mapAreaHeight - 32);
        int mapX = rightPanelX + (rightPanelWidth - mapDrawSize) / 2;
        int mapY = mapAreaY + 18;
        
        // Outer border
        graphics.fill(mapX - 1, mapY - 1, mapX + mapDrawSize + 1, mapY + mapDrawSize + 1, 0xFF388BFD);
        
        // Draw map texture with correct UV coordinates!
        if (mapTexture != null) {
            graphics.blit(MAP_TEXTURE_LOCATION, mapX, mapY, mapX + mapDrawSize, mapY + mapDrawSize, 0.0f, 1.0f, 0.0f, 1.0f);
        } else {
            graphics.fill(mapX, mapY, mapX + mapDrawSize, mapY + mapDrawSize, 0xFF111622);
            graphics.centeredText(this.font, HelpBoxLang.str("helpbox.ui.nav.loading_map"), mapX + mapDrawSize / 2, mapY + mapDrawSize / 2 - 4, 0xFF58A6FF);
        }
        
        // Updating indicator badge
        if (mapTextureUpdating) {
            RenderHelper.fillFlatRect(graphics, mapX + 4, mapY + 4, 62, 12, 0xBB000000, 0xFF58A6FF);
            graphics.text(this.font, "Updating...", mapX + 7, mapY + 6, 0xFF58A6FF);
        }
        
        // Center crosshair
        int centerX = mapX + mapDrawSize / 2;
        int centerY = mapY + mapDrawSize / 2;
        graphics.fill(centerX - 6, centerY, centerX + 7, centerY + 1, 0x55FFFFFF);
        graphics.fill(centerX, centerY - 6, centerX + 1, centerY + 7, 0x55FFFFFF);
        
        // Compass markers on borders (N, S, W, E)
        graphics.centeredText(this.font, "N", centerX, mapY + 2, 0xFFFF5555);
        graphics.centeredText(this.font, "S", centerX, mapY + mapDrawSize - 10, 0xFFAAAAAA);
        graphics.text(this.font, "W", mapX + 3, centerY - 4, 0xFFAAAAAA);
        graphics.text(this.font, "E", mapX + mapDrawSize - 9, centerY - 4, 0xFFAAAAAA);
        
        // Render Waypoints & Player
        if (this.minecraft.player != null) {
            int px = this.minecraft.player.getBlockX();
            int pz = this.minecraft.player.getBlockZ();
            int mapWorldCenterX = px + panX;
            int mapWorldCenterZ = pz + panZ;
            
            double zoom = ZOOM_LEVELS[zoomIndex];
            double viewRadius = 128.0 / zoom;
            
            // Waypoints
            for (Waypoint wp : WaypointManager.getWaypoints()) {
                if (!wp.isEnabled() && wp.isMapLocation()) continue;
                int dx = wp.getX() - mapWorldCenterX;
                int dz = wp.getZ() - mapWorldCenterZ;
                
                if (Math.abs(dx) <= viewRadius && Math.abs(dz) <= viewRadius) {
                    int drawX = mapX + mapDrawSize / 2 + (int)(dx / viewRadius * (mapDrawSize / 2.0));
                    int drawY = mapY + mapDrawSize / 2 + (int)(dz / viewRadius * (mapDrawSize / 2.0));
                    
                    try {
                        int col = java.awt.Color.decode(wp.getColorHex()).getRGB() | 0xFF000000;
                        graphics.fill(drawX - 2, drawY - 2, drawX + 3, drawY + 3, col);
                        String icon = wp.getIconName() != null ? wp.getIconName() : "";
                        graphics.text(this.font, icon, drawX + 4, drawY - 4, col);
                    } catch (Exception ignored) {}
                }
            }
            
            // Player marker
            int playerDx = -panX;
            int playerDz = -panZ;
            int playerDrawX = mapX + mapDrawSize / 2 + (int)(playerDx / viewRadius * (mapDrawSize / 2.0));
            int playerDrawY = mapY + mapDrawSize / 2 + (int)(playerDz / viewRadius * (mapDrawSize / 2.0));
            
            if (playerDrawX >= mapX + 3 && playerDrawX <= mapX + mapDrawSize - 3 &&
                playerDrawY >= mapY + 3 && playerDrawY <= mapY + mapDrawSize - 3) {
                
                // Position circle
                graphics.fill(playerDrawX - 3, playerDrawY - 3, playerDrawX + 4, playerDrawY + 4, 0xFF00FF88);
                graphics.fill(playerDrawX - 1, playerDrawY - 1, playerDrawX + 2, playerDrawY + 2, 0xFFFFFFFF);
                
                // Direction line
                float yaw = this.minecraft.player.getYRot();
                double rad = Math.toRadians(yaw - 90);
                int dirX = (int)(Math.cos(rad) * 7);
                int dirY = (int)(Math.sin(rad) * 7);
                graphics.fill(playerDrawX + dirX - 1, playerDrawY + dirY - 1, playerDrawX + dirX + 2, playerDrawY + dirY + 2, 0xFF00FFFF);
            } else {
                // Player off-screen indicator clamped to border
                int clampX = Math.max(mapX + 6, Math.min(mapX + mapDrawSize - 6, playerDrawX));
                int clampY = Math.max(mapY + 6, Math.min(mapY + mapDrawSize - 6, playerDrawY));
                graphics.fill(clampX - 3, clampY - 3, clampX + 4, clampY + 4, 0xFF00FF88);
            }
        }
        
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.minimap_help"), rightPanelX + 10, mapAreaY + mapAreaHeight - 12, 0xFF8B949E);
    }
    
    private void drawEditOverlay(GuiGraphicsExtractor graphics) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int w = 240;
        int h = 260;
        int startX = cx - w / 2;
        int startY = cy - h / 2;
        
        RenderHelper.fillFlatRect(graphics, startX, startY, w, h, 0xFF222222, 0xFF555555);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.edit_title"), startX + 10, startY + 10, 0xFFFFFFFF);
        
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.preview"), startX + 10, startY + 145, 0xFFAAAAAA);
        try {
            int prevCol = java.awt.Color.decode(editSelectedHex).getRGB() | 0xFF000000;
            graphics.fill(startX + 60, startY + 145, startX + 75, startY + 160, prevCol);
        } catch (Exception ignored) {}
        
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.icon"), startX + 10, startY + 165, 0xFFAAAAAA);
        for (int i = 0; i < ICONS.length; i++) {
            int icx = startX + 45 + (i * 22);
            int icy = startY + 165;
            int bgColor = ICONS[i].equals(editSelectedIcon) ? 0xFF666666 : 0xFF333333;
            graphics.fill(icx, icy, icx + 18, icy + 18, bgColor);
            graphics.text(this.font, ICONS[i], icx + 5, icy + 5, 0xFFFFFFFF);
        }
        
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.nav.sync_pos"), startX + 100, startY + 145, 0xFFAAAAAA);
        graphics.fill(startX + 160, startY + 145, startX + 170, startY + 155, editSyncEnabled ? 0xFF00FF00 : 0xFF555555);
    }
}
