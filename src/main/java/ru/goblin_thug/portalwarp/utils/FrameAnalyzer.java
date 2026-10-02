package ru.goblin_thug.portalwarp.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class FrameAnalyzer {

    private static final int MAX_INTERIOR = 128;
    private static final int MAX_FRAME_SIZE = 16;

    private enum Plane { XY, XZ, YZ }

    public static FrameData analyze(Block clicked, Player player) {
        World world = clicked.getWorld();
        int cx = clicked.getX();
        int cy = clicked.getY();
        int cz = clicked.getZ();

        for (Plane plane : Plane.values()) {
            FrameData result = tryPlane(cx, cy, cz, world, plane);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static FrameData tryPlane(int cx, int cy, int cz, World world, Plane plane) {
        int[][] dirs;
        FrameData.Axis axis;

        if (plane == Plane.XY) {
            dirs = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
            axis = FrameData.Axis.Z;
        } else if (plane == Plane.XZ) {
            dirs = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
            axis = FrameData.Axis.Y;
        } else {
            dirs = new int[][]{{0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            axis = FrameData.Axis.X;
        }

        // Если кликнутый блок — не воздух, ищем ближайший воздух в плоскости
        int startX = cx, startY = cy, startZ = cz;
        Block clickedBlock = world.getBlockAt(cx, cy, cz);

        if (!clickedBlock.getType().isAir()) {
            boolean found = false;
            outer:
            for (int[] d : dirs) {
                for (int i = 1; i <= 8; i++) {
                    int nx = cx + d[0] * i;
                    int ny = cy + d[1] * i;
                    int nz = cz + d[2] * i;
                    if (world.getBlockAt(nx, ny, nz).getType().isAir()) {
                        startX = nx;
                        startY = ny;
                        startZ = nz;
                        found = true;
                        break outer;
                    }
                }
            }
            if (!found) return null;
        }

        // BFS — заливаем внутренность рамки
        Set<Location> interior = new HashSet<>();
        Queue<int[]> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(new int[]{startX, startY, startZ});
        visited.add(startX + "," + startY + "," + startZ);

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0], y = cur[1], z = cur[2];

            if (!world.getBlockAt(x, y, z).getType().isAir()) continue;

            interior.add(new Location(world, x, y, z));
            if (interior.size() > MAX_INTERIOR) return null;

            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                int nz = z + d[2];
                String key = nx + "," + ny + "," + nz;
                if (!visited.contains(key)) {
                    visited.add(key);
                    queue.add(new int[]{nx, ny, nz});
                }
            }
        }

        if (interior.isEmpty()) return null;
        return buildFrame(interior, world, axis);
    }

    private static FrameData buildFrame(Set<Location> interior, World world, FrameData.Axis axis) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (Location loc : interior) {
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
        }

        int dx = maxX - minX + 1, dy = maxY - minY + 1, dz = maxZ - minZ + 1;

        // Толщина рамки по оси = 1
        if (axis == FrameData.Axis.X && dx != 1) return null;
        if (axis == FrameData.Axis.Y && dy != 1) return null;
        if (axis == FrameData.Axis.Z && dz != 1) return null;

        // Размер рамки
        int w, h;
        if (axis == FrameData.Axis.X) { w = dz; h = dy; }
        else if (axis == FrameData.Axis.Y) { w = dx; h = dz; }
        else { w = dx; h = dy; }

        if (w < 2 || h < 2) return null;
        if (w > MAX_FRAME_SIZE || h > MAX_FRAME_SIZE) return null;

        // Проверка: весь bbox заполнен воздухом (нет дыр внутри)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!interior.contains(new Location(world, x, y, z))) return null;
                }
            }
        }

        // Расширяем bbox на 1 по не-осевым координатам — это блоки самой рамки
        int fMinX = minX, fMaxX = maxX;
        int fMinY = minY, fMaxY = maxY;
        int fMinZ = minZ, fMaxZ = maxZ;
        if (axis != FrameData.Axis.X) { fMinX--; fMaxX++; }
        if (axis != FrameData.Axis.Y) { fMinY--; fMaxY++; }
        if (axis != FrameData.Axis.Z) { fMinZ--; fMaxZ++; }

        Set<Location> frameBlocks = new HashSet<>();
        for (int x = fMinX; x <= fMaxX; x++) {
            for (int y = fMinY; y <= fMaxY; y++) {
                for (int z = fMinZ; z <= fMaxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    if (interior.contains(loc)) continue;

                    boolean isCorner = isCornerPosition(x, y, z, minX, maxX, minY, maxY, minZ, maxZ, axis);

                    if (isCorner) {
                        // Углы необязательны — рамка без углов тоже валидна
                        if (!world.getBlockAt(x, y, z).getType().isAir()) {
                            frameBlocks.add(loc);
                        }
                        continue;
                    }

                    Material type = world.getBlockAt(x, y, z).getType();

                    // Блок рамки должен быть твёрдым (не воздух)
                    if (type.isAir()) return null;

                    frameBlocks.add(loc);
                }
            }
        }

        return new FrameData(world, frameBlocks, interior, fMinX, fMaxX, fMinY, fMaxY, fMinZ, fMaxZ, axis);
    }

    private static boolean isCornerPosition(int x, int y, int z,
                                             int minX, int maxX, int minY, int maxY,
                                             int minZ, int maxZ, FrameData.Axis axis) {
        if (axis == FrameData.Axis.X) {
            return (y < minY || y > maxY) && (z < minZ || z > maxZ);
        } else if (axis == FrameData.Axis.Y) {
            return (x < minX || x > maxX) && (z < minZ || z > maxZ);
        } else {
            return (x < minX || x > maxX) && (y < minY || y > maxY);
        }
    }
}
