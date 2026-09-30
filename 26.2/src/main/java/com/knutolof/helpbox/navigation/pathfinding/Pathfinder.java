/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package com.knutolof.helpbox.navigation.pathfinding;

import com.knutolof.helpbox.navigation.pathfinding.PathNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Pathfinder {
    private List<BlockPos> currentPath = new ArrayList<BlockPos>();
    private final AtomicBoolean isCalculating = new AtomicBoolean(false);

    public void calculatePathAsync(BlockPos start, BlockPos target) {
        if (this.isCalculating.get()) {
            return;
        }
        this.isCalculating.set(true);
        CompletableFuture.runAsync(() -> {
            try {
                ClientLevel level = Minecraft.getInstance().level;
                if (level != null) {
                    List<BlockPos> path = this.calculatePath(start, target, (Level)level);
                    if (path != null && !path.isEmpty()) {
                        path = this.smoothPath(path, (Level)level);
                    }
                    this.currentPath = path != null ? path : new ArrayList<BlockPos>();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.isCalculating.set(false);
            }
        });
    }

    public List<BlockPos> getCurrentPath() {
        return this.currentPath;
    }

    public void clearPath() {
        this.currentPath.clear();
    }

    public boolean isCalculating() {
        return this.isCalculating.get();
    }

    private List<BlockPos> calculatePath(BlockPos start, BlockPos target, Level level) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<PathNode>();
        HashSet<BlockPos> closedSet = new HashSet<BlockPos>();
        HashMap<BlockPos, PathNode> allNodes = new HashMap<BlockPos, PathNode>();
        PathNode startNode = new PathNode(start);
        startNode.gCost = 0.0;
        startNode.hCost = this.getHeuristic(start, target);
        openSet.add(startNode);
        allNodes.put(start, startNode);
        int iterations = 0;
        long startTime = System.currentTimeMillis();
        PathNode closestNode = startNode;
        double minHCost = startNode.hCost;
        while (!openSet.isEmpty()) {
            if (iterations++ > 5000 || iterations % 50 == 0 && System.currentTimeMillis() - startTime > 50L) {
                return this.reconstructPath(closestNode);
            }
            PathNode current = (PathNode)openSet.poll();
            if (current.pos.distManhattan((Vec3i)target) <= 1) {
                return this.reconstructPath(current);
            }
            closedSet.add(current.pos);
            for (BlockPos neighborPos : this.getNeighbors(current.pos, level)) {
                double movementCost;
                if (closedSet.contains(neighborPos)) continue;
                double d = movementCost = current.pos.getY() != neighborPos.getY() ? 1.5 : 1.0;
                if (level.getBlockState(neighborPos).liquid() || level.getBlockState(neighborPos.below()).liquid()) {
                    movementCost += 8.0;
                }
                double newCostToNeighbor = current.gCost + movementCost;
                PathNode neighborNode = (PathNode)allNodes.get(neighborPos);
                if (neighborNode == null) {
                    neighborNode = new PathNode(neighborPos);
                    allNodes.put(neighborPos, neighborNode);
                }
                if (!(newCostToNeighbor < neighborNode.gCost)) continue;
                neighborNode.gCost = newCostToNeighbor;
                neighborNode.hCost = this.getHeuristic(neighborPos, target);
                neighborNode.parent = current;
                if (openSet.contains(neighborNode)) continue;
                openSet.add(neighborNode);
                if (!(neighborNode.hCost < minHCost)) continue;
                minHCost = neighborNode.hCost;
                closestNode = neighborNode;
            }
        }
        return this.reconstructPath(closestNode);
    }

    private List<BlockPos> getNeighbors(BlockPos pos, Level level) {
        int[][] dirs;
        ArrayList<BlockPos> neighbors = new ArrayList<BlockPos>();
        for (int[] dir : dirs = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}}) {
            BlockPos n = pos.offset(dir[0], 0, dir[2]);
            if (this.isWalkable(n, level)) {
                neighbors.add(n);
                continue;
            }
            if (this.isWalkable(n.above(), level)) {
                if (!level.getBlockState(pos.above().above()).getCollisionShape((BlockGetter)level, pos.above().above()).isEmpty()) continue;
                neighbors.add(n.above());
                continue;
            }
            if (!this.isWalkable(n.below(), level) || !level.getBlockState(n.above()).getCollisionShape((BlockGetter)level, n.above()).isEmpty()) continue;
            neighbors.add(n.below());
        }
        return neighbors;
    }

    private boolean isWalkable(BlockPos pos, Level level) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());
        boolean groundOk = !ground.getCollisionShape((BlockGetter)level, pos.below()).isEmpty() || ground.liquid();
        boolean feetOk = feet.getCollisionShape((BlockGetter)level, pos).isEmpty() || feet.liquid();
        boolean headOk = head.getCollisionShape((BlockGetter)level, pos.above()).isEmpty() || head.liquid();
        return groundOk && feetOk && headOk;
    }

    private double getHeuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr((Vec3i)b));
    }

    private List<BlockPos> reconstructPath(PathNode endNode) {
        ArrayList<BlockPos> path = new ArrayList<BlockPos>();
        PathNode current = endNode;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private List<BlockPos> smoothPath(List<BlockPos> path, Level level) {
        if (path.size() <= 2) {
            return path;
        }
        ArrayList<BlockPos> smoothedPath = new ArrayList<BlockPos>();
        smoothedPath.add(path.get(0));
        int currentIndex = 0;
        while (currentIndex < path.size() - 1) {
            int furthestIndex = currentIndex + 1;
            for (int j = currentIndex + 2; j < path.size(); ++j) {
                if (!this.hasLineOfSight(path.get(currentIndex), path.get(j), level)) continue;
                furthestIndex = j;
            }
            smoothedPath.add(path.get(furthestIndex));
            currentIndex = furthestIndex;
        }
        return smoothedPath;
    }

    private boolean hasLineOfSight(BlockPos a, BlockPos b, Level level) {
        Vec3 end;
        Vec3 start = new Vec3((double)a.getX() + 0.5, (double)a.getY() + 0.5, (double)a.getZ() + 0.5);
        if (!this.isClear(start, end = new Vec3((double)b.getX() + 0.5, (double)b.getY() + 0.5, (double)b.getZ() + 0.5), level)) {
            return false;
        }
        Vec3 dir = end.subtract(start).normalize();
        Vec3 perp = new Vec3(-dir.z, 0.0, dir.x).scale(0.4);
        if (!this.isClear(start.add(perp), end.add(perp), level)) {
            return false;
        }
        return this.isClear(start.subtract(perp), end.subtract(perp), level);
    }

    private boolean isClear(Vec3 start, Vec3 end, Level level) {
        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)Minecraft.getInstance().player);
        BlockHitResult hit = level.clip(context);
        return hit.getType() == HitResult.Type.MISS;
    }
}
