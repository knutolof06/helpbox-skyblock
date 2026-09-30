/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.BlockGetter
 */
package com.knutolof.helpbox.navigation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.knutolof.helpbox.HelpBoxMod;
import com.knutolof.helpbox.navigation.model.NavigationConfig;
import com.knutolof.helpbox.navigation.model.Waypoint;
import com.knutolof.helpbox.navigation.pathfinding.Pathfinder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;

public class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "helpbox_waypoints.json");
    private static NavigationConfig config = new NavigationConfig();
    private static final Pathfinder pathfinder = new Pathfinder();
    private static int tickCounter = 0;
    private static boolean useAStar = true;
    private static BlockPos lastCalculatedPlayerPos = null;
    private static String lastCalculatedTargetId = null;

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE);){
                config = (NavigationConfig)GSON.fromJson((Reader)reader, NavigationConfig.class);
                if (config == null) {
                    config = new NavigationConfig();
                }
            }
            catch (IOException e) {
                HelpBoxMod.LOGGER.error("[HelpBox] Failed to load waypoints", (Throwable)e);
            }
        } else {
            WaypointManager.save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE);){
            GSON.toJson((Object)config, (Appendable)writer);
        }
        catch (IOException e) {
            HelpBoxMod.LOGGER.error("[HelpBox] Failed to save waypoints", (Throwable)e);
        }
    }

    public static List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(WaypointManager.config.waypoints);
    }

    public static void addWaypoint(Waypoint waypoint) {
        WaypointManager.config.waypoints.add(waypoint);
        WaypointManager.save();
    }

    public static void updateWaypoint(Waypoint waypoint) {
        WaypointManager.save();
    }

    public static void removeWaypoint(String id) {
        WaypointManager.config.waypoints.removeIf(w -> w.getId().equals(id));
        if (id.equals(WaypointManager.config.activeWaypointId)) {
            WaypointManager.config.activeWaypointId = null;
        }
        WaypointManager.save();
    }

    public static Waypoint getActiveWaypoint() {
        if (WaypointManager.config.activeWaypointId == null) {
            return null;
        }
        return WaypointManager.config.waypoints.stream().filter(w -> w.getId().equals(WaypointManager.config.activeWaypointId)).findFirst().orElse(null);
    }

    public static void setActiveWaypoint(String id) {
        WaypointManager.config.activeWaypointId = id;
        WaypointManager.save();
        WaypointManager.invalidateCache();
    }

    public static void clearActiveWaypoint() {
        WaypointManager.config.activeWaypointId = null;
        pathfinder.clearPath();
        WaypointManager.invalidateCache();
        WaypointManager.save();
    }

    public static Pathfinder getPathfinder() {
        return pathfinder;
    }

    public static void invalidateCache() {
        lastCalculatedPlayerPos = null;
        lastCalculatedTargetId = null;
        pathfinder.clearPath();
    }

    public static void tick() {
        boolean playerDeviated;
        Waypoint active = WaypointManager.getActiveWaypoint();
        if (active == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        BlockPos playerPos = client.player.blockPosition();
        BlockPos targetPos = new BlockPos(active.getX(), active.getY(), active.getZ());
        BlockPos startPos = playerPos;
        ClientLevel level = client.level;
        if (level != null) {
            for (int maxDrop = 320; maxDrop > 0 && startPos.getY() > -64 && level.getBlockState(startPos.below()).getCollisionShape((BlockGetter)level, startPos.below()).isEmpty() && !level.getBlockState(startPos.below()).liquid(); --maxDrop) {
                startPos = startPos.below();
            }
        }
        boolean targetChanged = lastCalculatedTargetId == null || !lastCalculatedTargetId.equals(active.getId());
        boolean bl = playerDeviated = lastCalculatedPlayerPos == null || playerPos.distSqr((Vec3i)lastCalculatedPlayerPos) > 9.0;
        if (++tickCounter >= 10) {
            tickCounter = 0;
            if ((targetChanged || playerDeviated) && !pathfinder.isCalculating()) {
                lastCalculatedPlayerPos = playerPos;
                lastCalculatedTargetId = active.getId();
                pathfinder.calculatePathAsync(startPos, targetPos);
            }
        }
    }

    public static boolean isAutoClearEnabled() {
        return WaypointManager.config.autoClearWaypoint;
    }

    public static void setAutoClearEnabled(boolean enabled) {
        WaypointManager.config.autoClearWaypoint = enabled;
        WaypointManager.save();
    }

    public static float getEspLineOpacity() {
        return WaypointManager.config.espLineOpacity;
    }

    public static void setEspLineOpacity(float opacity) {
        WaypointManager.config.espLineOpacity = opacity;
        WaypointManager.save();
    }
}
