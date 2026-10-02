package ru.goblin_thug.portalwarp.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.goblin_thug.portalwarp.PortalWarp;
import ru.goblin_thug.portalwarp.managers.Portal;
import ru.goblin_thug.portalwarp.managers.PortalManager;
import ru.goblin_thug.portalwarp.utils.FrameAnalyzer;
import ru.goblin_thug.portalwarp.utils.FrameData;

import java.util.UUID;

public class CompassListener implements Listener {

    private final PortalWarp plugin;

    public CompassListener(PortalWarp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand() == null ||
            player.getInventory().getItemInMainHand().getType() != Material.RECOVERY_COMPASS) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PortalManager manager = plugin.getPortalManager();
        Block clickedBlock = event.getClickedBlock();

        FrameData frame = FrameAnalyzer.analyze(clickedBlock, player);
        if (frame == null) {
            if (manager.hasPendingCreation(uuid) || manager.hasPendingConnectCreation(uuid)) {
                player.sendMessage("§c[PortalWarp] Не удалось распознать рамку! Проверьте целостность постройки.");
            }
            return;
        }

        event.setCancelled(true);

        // Проверка: координаты рамки уже заняты другим порталом
        if (manager.isFrameOccupied(frame)) {
            player.sendMessage("§c[PortalWarp] Эти координаты уже используются другим порталом!");
            return;
        }

        // === Межсерверный портал (1 клик) ===
        if (manager.hasPendingConnectCreation(uuid)) {
            String name = manager.getPendingName(uuid);
            String address = manager.getPendingConnectAddress(uuid);

            // Проверка: имя занято
            if (manager.getPortal(name) != null) {
                player.sendMessage("§c[PortalWarp] Портал с названием §e" + name + " §cуже существует!");
                manager.removePendingConnectCreation(uuid);
                return;
            }

            Portal portal = new Portal(name);
            portal.setFrame1(frame);
            portal.setServerAddress(address);

            manager.addPortal(portal);
            manager.removePendingConnectCreation(uuid);

            player.sendMessage("§a[PortalWarp] Межсерверный портал §e" + name + " §aсоздан!");
            player.sendMessage("§7Адрес: §b" + address);
            return;
        }

        // === Обычный портал (2 клика) ===
        if (!manager.hasPendingCreation(uuid)) return;

        // Шаг 1: первый клик
        if (manager.getPendingFrame1(uuid) == null) {
            manager.setPendingFrame1(uuid, frame);
            player.sendMessage("§a[PortalWarp] Первая рамка (ВЫХОД) сохранена.");
            player.sendMessage("§7Теперь кликните по §eВТОРОЙ §7рамке (ВХОД).");
            return;
        }

        // Шаг 2: второй клик
        FrameData firstFrame = manager.getPendingFrame1(uuid);
        String name = manager.getPendingName(uuid);

        // Проверка: та же рамка
        if (firstFrame.equals(frame)) {
            player.sendMessage("§c[PortalWarp] Это та же рамка! Нужна другая.");
            return;
        }

        // Проверка: имя занято
        if (manager.getPortal(name) != null) {
            player.sendMessage("§c[PortalWarp] Портал с названием §e" + name + " §cуже существует!");
            manager.removePendingCreation(uuid);
            return;
        }

        // Проверка: рамки в разных мирах
        if (!firstFrame.getWorld().equals(frame.getWorld())) {
            player.sendMessage("§c[PortalWarp] Рамки находятся в разных мирах!");
            player.sendMessage("§7Обе рамки должны быть в одном мире: §e" + firstFrame.getWorld().getName());
            manager.removePendingCreation(uuid);
            return;
        }

        Portal portal = new Portal(name);
        portal.setFrame1(firstFrame);
        portal.setFrame2(frame);

        manager.addPortal(portal);
        manager.removePendingCreation(uuid);

        player.sendMessage("§a[PortalWarp] Портал §e" + name + " §aсоздан!");
        player.sendMessage("§7Телепортация работает в обе стороны.");
    }
}
