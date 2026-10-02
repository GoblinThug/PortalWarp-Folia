package ru.goblin_thug.portalwarp.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import ru.goblin_thug.portalwarp.PortalWarp;
import ru.goblin_thug.portalwarp.managers.Portal;
import ru.goblin_thug.portalwarp.managers.PortalManager;
import ru.goblin_thug.portalwarp.utils.FrameData;

import java.util.UUID;

public class PortalListener implements Listener {

    private final PortalWarp plugin;

    public PortalListener(PortalWarp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location loc = event.getTo();
        PortalManager manager = plugin.getPortalManager();

        Portal portal = manager.findPortalAt(loc);
        if (portal == null) return;

        if (manager.isOnCooldown(uuid)) return;
        manager.setCooldown(uuid);

        // === Серверный портал ===
        if (portal.isCrossServer()) {
            String address = portal.getServerAddress();
            String host;
            int port;

            if (address.contains(":")) {
                String[] parts = address.split(":", 2);
                host = parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    port = 25565;
                }
            } else {
                host = address;
                port = 25565;
            }

            player.sendMessage("§a[PortalWarp] §7Подключение к серверу §b" + host + ":" + port + "§7...");
            player.setFallDistance(0f);
            player.setVelocity(new Vector(0, 0, 0));

            player.transfer(host, port);

            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> {
                manager.removeCooldown(uuid);
            }, manager.getTeleportCooldown());
            return;
        }

        // === Обычный портал ===
        FrameData dstFrame = manager.isInFrameInterior(portal.getFrame1(), loc)
                ? portal.getFrame2() : portal.getFrame1();

        Location exit = getSafeExit(dstFrame);
        if (exit == null) {
            manager.removeCooldown(uuid);
            return;
        }

        exit.setYaw(player.getLocation().getYaw());
        exit.setPitch(player.getLocation().getPitch());

        player.sendMessage("§a[PortalWarp] §7Телепортация на портал §e" + portal.getName() + "§7...");

        player.setFallDistance(0f);
        player.setVelocity(new Vector(0, 0, 0));

        player.teleportAsync(exit).thenAccept(success -> {
            if (success) {
                player.setFallDistance(0f);
                player.setVelocity(new Vector(0, 0, 0));
            }
        });

        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> {
            manager.removeCooldown(uuid);
        }, manager.getTeleportCooldown());
    }

    private Location getSafeExit(FrameData dstFrame) {
        if (dstFrame == null) return null;

        org.bukkit.World world = dstFrame.getWorld();
        if (world == null) return null;

        int floorY = dstFrame.getMinY();
        int cx = (dstFrame.getMinX() + dstFrame.getMaxX()) / 2;
        int cz = (dstFrame.getMinZ() + dstFrame.getMaxZ()) / 2;

        FrameData.Axis axis = dstFrame.getAxis();
        int offX = 0, offZ = 0;
        if (axis == FrameData.Axis.X) {
            offX = 1;
        } else if (axis == FrameData.Axis.Z) {
            offZ = 1;
        } else {
            offX = 1;
        }

        for (int i = 1; i <= 3; i++) {
            Location c1 = trySafeAt(world, cx + offX * i, floorY, cz + offZ * i);
            if (c1 != null) return c1;
            Location c2 = trySafeAt(world, cx - offX * i, floorY, cz - offZ * i);
            if (c2 != null) return c2;
        }

        for (int dy = 1; dy <= 3; dy++) {
            Location c1 = trySafeAt(world, cx + offX, floorY + dy, cz + offZ);
            if (c1 != null) return c1;
            Location c2 = trySafeAt(world, cx - offX, floorY + dy, cz - offZ);
            if (c2 != null) return c2;
        }

        return new Location(world, cx + 0.5, floorY, cz + 0.5);
    }

    private Location trySafeAt(org.bukkit.World world, int x, int y, int z) {
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);

        if (feet.getType().isAir() && head.getType().isAir()
            && !ground.getType().isAir() && ground.getType() != Material.LAVA) {
            return new Location(world, x + 0.5, y, z + 0.5);
        }
        return null;
    }
}
