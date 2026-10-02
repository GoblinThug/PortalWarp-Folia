package ru.goblin_thug.portalwarp.utils;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.Set;

public class FrameData {

    public enum Axis { X, Y, Z }

    private final World world;
    private final Set<Location> frameBlocks;
    private final Set<Location> interiorBlocks;
    private final int minX, maxX, minY, maxY, minZ, maxZ;
    private final Axis axis;

    public FrameData(World world, Set<Location> frameBlocks, Set<Location> interiorBlocks,
                     int minX, int maxX, int minY, int maxY, int minZ, int maxZ, Axis axis) {
        this.world = world;
        this.frameBlocks = frameBlocks;
        this.interiorBlocks = interiorBlocks;
        this.minX = minX; this.maxX = maxX;
        this.minY = minY; this.maxY = maxY;
        this.minZ = minZ; this.maxZ = maxZ;
        this.axis = axis;
    }

    public World getWorld() { return world; }
    public Set<Location> getFrameBlocks() { return frameBlocks; }
    public Set<Location> getInteriorBlocks() { return interiorBlocks; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public Axis getAxis() { return axis; }

    public Location getExitLocation() {
        double cx = (minX + maxX) / 2.0 + 0.5;
        double cy = (minY + maxY) / 2.0 + 0.5;
        double cz = (minZ + maxZ) / 2.0 + 0.5;
        return new Location(world, cx, cy, cz);
    }

    public String getSizeString() {
        return (maxX - minX + 1) + "x" + (maxY - minY + 1) + "x" + (maxZ - minZ + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrameData frameData = (FrameData) o;
        return minX == frameData.minX && maxX == frameData.maxX
            && minY == frameData.minY && maxY == frameData.maxY
            && minZ == frameData.minZ && maxZ == frameData.maxZ
            && axis == frameData.axis
            && (world != null && world.equals(frameData.world));
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, minX, maxX, minY, maxY, minZ, maxZ, axis);
    }
}
