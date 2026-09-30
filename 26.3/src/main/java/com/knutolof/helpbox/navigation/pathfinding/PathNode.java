package com.knutolof.helpbox.navigation.pathfinding;

import net.minecraft.core.BlockPos;

public class PathNode implements Comparable<PathNode> {
    public final BlockPos pos;
    public double gCost;
    public double hCost;
    public PathNode parent;
    
    public PathNode(BlockPos pos) {
        this.pos = pos;
        this.gCost = Double.MAX_VALUE;
        this.hCost = 0;
        this.parent = null;
    }
    
    public double getFCost() {
        return gCost + hCost;
    }
    
    @Override
    public int compareTo(PathNode other) {
        int compare = Double.compare(this.getFCost(), other.getFCost());
        if (compare == 0) {
            return Double.compare(this.hCost, other.hCost);
        }
        return compare;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PathNode)) return false;
        return pos.equals(((PathNode)obj).pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}
