package ru.goblin_thug.portalwarp.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.goblin_thug.portalwarp.PortalWarp;
import ru.goblin_thug.portalwarp.utils.FrameData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PortalManager {

    private final PortalWarp plugin;
    private final Map<String, Portal> portals = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingNames = new ConcurrentHashMap<>();
    private final Map<UUID, FrameData> pendingFrame1 = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingConnectAddresses = new ConcurrentHashMap<>();
    private final Set<UUID> teleportCooldown = ConcurrentHashMap.newKeySet();
    private final Map<String, List<ScheduledTask>> portalParticleTasks = new ConcurrentHashMap<>();

    public PortalManager(PortalWarp plugin) {
        this.plugin = plugin;
    }

    // === Настройки из конфига ===

    public int getTeleportCooldown() {
        return plugin.getConfig().getInt("teleport-cooldown", 20);
    }

    public int getMaxInteriorSize() {
        return plugin.getConfig().getInt("max-interior-size", 100);
    }

    private int getParticleInterval() {
        return plugin.getConfig().getInt("particles.interval", 3);
    }

    private int getParticleCount() {
        return plugin.getConfig().getInt("particles.count", 6);
    }

    private double getParticleSpread() {
        return plugin.getConfig().getDouble("particles.spread", 0.4);
    }

    private Particle.DustOptions getParticleDust() {
        int r = plugin.getConfig().getInt("particles.color.red", 60);
        int g = plugin.getConfig().getInt("particles.color.green", 140);
        int b = plugin.getConfig().getInt("particles.color.blue", 255);
        float size = (float) plugin.getConfig().getDouble("particles.size", 1.5);
        return new Particle.DustOptions(Color.fromRGB(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b))
        ), size);
    }

    // === Хранение порталов ===

    public Portal getPortal(String name) {
        return portals.get(name.toLowerCase());
    }

    public Collection<Portal> getAllPortals() {
        return portals.values();
    }

    public void addPortal(Portal portal) {
        portals.put(portal.getName().toLowerCase(), portal);
        startParticles(portal);
        savePortals();
    }

    public boolean deletePortal(String name) {
        String key = name.toLowerCase();
        Portal removed = portals.remove(key);
        if (removed == null) return false;

        List<ScheduledTask> tasks = portalParticleTasks.remove(key);
        if (tasks != null) {
            for (ScheduledTask t : tasks) t.cancel();
        }

        clearInteriorBlocks(removed.getFrame1());
        clearInteriorBlocks(removed.getFrame2());
        savePortals();
        return true;
    }

    // === Поиск ===

    public Portal findPortalAt(Location loc) {
        for (Portal portal : portals.values()) {
            if (isInFrameInterior(portal.getFrame1(), loc)) return portal;
            if (isInFrameInterior(portal.getFrame2(), loc)) return portal;
        }
        return null;
    }

    public Location getExitFor(Portal portal, Location currentLoc) {
        FrameData f1 = portal.getFrame1();
        FrameData f2 = portal.getFrame2();
        if (isInFrameInterior(f1, currentLoc)) return f2.getExitLocation();
        if (isInFrameInterior(f2, currentLoc)) return f1.getExitLocation();
        return null;
    }

    public boolean isInFrameInterior(FrameData frame, Location loc) {
        if (frame == null) return false;
        if (frame.getWorld() == null || !frame.getWorld().equals(loc.getWorld())) return false;
        for (Location interior : frame.getInteriorBlocks()) {
            if (interior.getBlockX() == loc.getBlockX()
                && interior.getBlockY() == loc.getBlockY()
                && interior.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    // === Проверка занятости координат рамки ===

    public boolean isFrameOccupied(FrameData newFrame) {
        for (Portal portal : portals.values()) {
            if (framesOverlap(portal.getFrame1(), newFrame)) return true;
            if (framesOverlap(portal.getFrame2(), newFrame)) return true;
        }
        return false;
    }

    private boolean framesOverlap(FrameData existing, FrameData candidate) {
        if (existing == null || candidate == null) return false;
        if (!existing.getWorld().equals(candidate.getWorld())) return false;
        return existing.getMinX() <= candidate.getMaxX() && existing.getMaxX() >= candidate.getMinX()
            && existing.getMinY() <= candidate.getMaxY() && existing.getMaxY() >= candidate.getMinY()
            && existing.getMinZ() <= candidate.getMaxZ() && existing.getMaxZ() >= candidate.getMinZ();
    }

    // === Частицы ===

    public void startParticles(Portal portal) {
        String name = portal.getName().toLowerCase();
        List<ScheduledTask> tasks = new ArrayList<>();

        if (portal.getFrame1() != null) {
            fillInteriorBlocks(portal.getFrame1());
            tasks.add(startParticleTask(portal.getFrame1()));
        }
        if (portal.getFrame2() != null) {
            fillInteriorBlocks(portal.getFrame2());
            tasks.add(startParticleTask(portal.getFrame2()));
        }

        portalParticleTasks.put(name, tasks);
    }

    private ScheduledTask startParticleTask(FrameData frame) {
        Location center = frame.getExitLocation();
        int interval = getParticleInterval();
        return plugin.getServer().getRegionScheduler().runAtFixedRate(
            plugin, center, t -> spawnParticles(frame), 5L, interval);
    }

    private void spawnParticles(FrameData frame) {
        World world = frame.getWorld();
        if (world == null) return;

        Particle.DustOptions dust = getParticleDust();
        double spread = getParticleSpread();
        int count = getParticleCount();

        long tick = System.currentTimeMillis() / 50;
        double phase = (tick * 0.08) % 1.0;

        List<Location> interiorList = new ArrayList<>(frame.getInteriorBlocks());

        int ambientCount = plugin.getConfig().getInt("portal.ambient-count", 2);
        Particle ambientType = Particle.ENCHANT;
        String ambientName = plugin.getConfig().getString("portal.ambient-particle", "ENCHANT");
        try {
            ambientType = Particle.valueOf(ambientName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[PortalWarp] Неизвестный тип частиц: " + ambientName + ", использую ENCHANT");
        }

        for (int i = 0; i < interiorList.size(); i++) {
            Location loc = interiorList.get(i);

            // --- Слой 1: плотная плёнка DUST ---
            for (int j = 0; j < count; j++) {
                double xOffset, yOffset, zOffset;

                if (frame.getAxis() == FrameData.Axis.X) {
                    xOffset = (Math.random() - 0.5) * 0.15;
                    yOffset = (Math.random() - 0.5) * spread;
                    zOffset = (Math.random() - 0.5) * spread;
                } else if (frame.getAxis() == FrameData.Axis.Y) {
                    xOffset = (Math.random() - 0.5) * spread;
                    yOffset = (Math.random() - 0.5) * 0.15;
                    zOffset = (Math.random() - 0.5) * spread;
                } else {
                    xOffset = (Math.random() - 0.5) * spread;
                    yOffset = (Math.random() - 0.5) * spread;
                    zOffset = (Math.random() - 0.5) * 0.15;
                }

                world.spawnParticle(
                    Particle.DUST,
                    loc.getX() + 0.5 + xOffset,
                    loc.getY() + 0.5 + yOffset,
                    loc.getZ() + 0.5 + zOffset,
                    1, 0, 0, 0, 0.0, dust
                );
            }

            // --- Слой 2: струящиеся частицы ---
            double flowOffset = (phase + i * 0.05) % 1.0;
            double flowY = 1.0 - flowOffset;

            if (frame.getAxis() == FrameData.Axis.Y) {
                world.spawnParticle(
                    Particle.DUST,
                    loc.getX() + 0.5 + (Math.random() - 0.5) * spread,
                    loc.getY() + 0.5,
                    loc.getZ() + 0.5 + (flowOffset - 0.5) * spread * 2,
                    1, 0, 0, 0, 0.0, dust
                );
            } else {
                world.spawnParticle(
                    Particle.DUST,
                    loc.getX() + 0.5 + (Math.random() - 0.5) * 0.2,
                    loc.getY() + flowY,
                    loc.getZ() + 0.5 + (Math.random() - 0.5) * 0.2,
                    1, 0, 0, 0, 0.0, dust
                );
            }

            // --- Слой 3: ambient-частицы ---
            if (ambientCount > 0 && i % 2 == 0) {
                world.spawnParticle(
                    ambientType,
                    loc.getX() + 0.5 + (Math.random() - 0.5) * spread,
                    loc.getY() + 0.3 + Math.random() * 0.5,
                    loc.getZ() + 0.5 + (Math.random() - 0.5) * spread,
                    ambientCount, 0.2, 0.3, 0.2, 0.01
                );
            }
        }
    }

    public void cancelAllParticleTasks() {
        for (List<ScheduledTask> tasks : portalParticleTasks.values()) {
            for (ScheduledTask t : tasks) t.cancel();
        }
        portalParticleTasks.clear();
    }

    // === Заполнение / очистка блоков ===

    private void fillInteriorBlocks(FrameData frame) {
        if (frame == null) return;
        Location center = frame.getExitLocation();

        String matName = plugin.getConfig().getString("portal.fill-material", "CYAN_STAINED_GLASS_PANE");
        Material fillMaterial;
        try {
            fillMaterial = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            fillMaterial = Material.CYAN_STAINED_GLASS_PANE;
        }
        final Material material = fillMaterial;

        plugin.getServer().getRegionScheduler().run(plugin, center, task -> {
            World world = frame.getWorld();
            if (world == null) return;
            int filled = 0;
            for (Location loc : frame.getInteriorBlocks()) {
                Block b = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                b.setType(material);
                filled++;
            }
            plugin.getLogger().info("[PortalWarp] Заполнено панелями: " + filled + " блоков");
        });
    }

    private void clearInteriorBlocks(FrameData frame) {
        if (frame == null) return;
        Location center = frame.getExitLocation();
        plugin.getServer().getRegionScheduler().run(plugin, center, task -> {
            World world = frame.getWorld();
            if (world == null) return;
            for (Location loc : frame.getInteriorBlocks()) {
                Block b = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                Material type = b.getType();
                if (type.name().endsWith("_STAINED_GLASS")
                    || type.name().endsWith("_STAINED_GLASS_PANE")
                    || type == Material.NETHER_PORTAL) {
                    b.setType(Material.AIR);
                }
            }
        });
    }

    // === Pending: обычные порталы ===

    public void setPendingCreation(UUID uuid, String portalName) {
        pendingNames.put(uuid, portalName.toLowerCase());
        pendingFrame1.remove(uuid);
        pendingConnectAddresses.remove(uuid);
    }

    public boolean hasPendingCreation(UUID uuid) {
        return pendingNames.containsKey(uuid) && !pendingConnectAddresses.containsKey(uuid);
    }

    public String getPendingName(UUID uuid) {
        return pendingNames.get(uuid);
    }

    public FrameData getPendingFrame1(UUID uuid) {
        return pendingFrame1.get(uuid);
    }

    public void setPendingFrame1(UUID uuid, FrameData frame) {
        pendingFrame1.put(uuid, frame);
    }

    public void removePendingCreation(UUID uuid) {
        pendingNames.remove(uuid);
        pendingFrame1.remove(uuid);
    }

    // === Pending: серверные порталы ===

    public void setPendingConnectCreation(UUID uuid, String portalName, String address) {
        pendingNames.put(uuid, portalName.toLowerCase());
        pendingConnectAddresses.put(uuid, address);
        pendingFrame1.remove(uuid);
    }

    public boolean hasPendingConnectCreation(UUID uuid) {
        return pendingConnectAddresses.containsKey(uuid);
    }

    public String getPendingConnectAddress(UUID uuid) {
        return pendingConnectAddresses.get(uuid);
    }

    public void removePendingConnectCreation(UUID uuid) {
        pendingNames.remove(uuid);
        pendingConnectAddresses.remove(uuid);
        pendingFrame1.remove(uuid);
    }

    // === Кулдаун ===

    public boolean isOnCooldown(UUID uuid) {
        return teleportCooldown.contains(uuid);
    }

    public void setCooldown(UUID uuid) {
        teleportCooldown.add(uuid);
    }

    public void removeCooldown(UUID uuid) {
        teleportCooldown.remove(uuid);
    }

    // === Сохранение / Загрузка ===

    public void savePortals() {
        FileConfiguration config = plugin.getConfig();
        config.set("portals", null);

        int i = 0;
        for (Portal portal : portals.values()) {
            String base = "portals." + i;
            config.set(base + ".name", portal.getName());
            saveFrame(config, base + ".frame1", portal.getFrame1());
            saveFrame(config, base + ".frame2", portal.getFrame2());
            config.set(base + ".serverAddress", portal.getServerAddress());
            i++;
        }
        plugin.saveConfig();
    }

    private void saveFrame(FileConfiguration config, String path, FrameData frame) {
        if (frame == null) return;
        config.set(path + ".world", frame.getWorld().getName());
        config.set(path + ".minX", frame.getMinX());
        config.set(path + ".maxX", frame.getMaxX());
        config.set(path + ".minY", frame.getMinY());
        config.set(path + ".maxY", frame.getMaxY());
        config.set(path + ".minZ", frame.getMinZ());
        config.set(path + ".maxZ", frame.getMaxZ());
        config.set(path + ".axis", frame.getAxis().name());

        List<String> interiorList = new ArrayList<>();
        for (Location loc : frame.getInteriorBlocks()) {
            interiorList.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
        config.set(path + ".interior", interiorList);

        List<String> frameList = new ArrayList<>();
        for (Location loc : frame.getFrameBlocks()) {
            frameList.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
        config.set(path + ".frameBlocks", frameList);
    }

    public void loadPortals() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("portals");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String base = "portals." + key;
            String name = config.getString(base + ".name");
            if (name == null) continue;

            Portal portal = new Portal(name);
            portal.setFrame1(loadFrame(config, base + ".frame1"));
            portal.setFrame2(loadFrame(config, base + ".frame2"));

            String serverAddress = config.getString(base + ".serverAddress");
            if (serverAddress != null) {
                portal.setServerAddress(serverAddress);
            }

            boolean hasFrame2 = portal.getFrame2() != null;
            boolean hasServer = portal.getServerAddress() != null;

            if (portal.getFrame1() != null && (hasFrame2 || hasServer)) {
                portals.put(name.toLowerCase(), portal);
                startParticles(portal);
            }
        }
    }

    private FrameData loadFrame(FileConfiguration config, String path) {
        String worldName = config.getString(path + ".world");
        if (worldName == null) return null;
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Мир '" + worldName + "' не найден при загрузке портала!");
            return null;
        }

        int minX = config.getInt(path + ".minX");
        int maxX = config.getInt(path + ".maxX");
        int minY = config.getInt(path + ".minY");
        int maxY = config.getInt(path + ".maxY");
        int minZ = config.getInt(path + ".minZ");
        int maxZ = config.getInt(path + ".maxZ");
        FrameData.Axis axis = FrameData.Axis.valueOf(config.getString(path + ".axis", "X"));

        Set<Location> interiorBlocks = new HashSet<>();
        for (String s : config.getStringList(path + ".interior")) {
            String[] p = s.split(",");
            interiorBlocks.add(new Location(world, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
        }

        Set<Location> frameBlocks = new HashSet<>();
        for (String s : config.getStringList(path + ".frameBlocks")) {
            String[] p = s.split(",");
            frameBlocks.add(new Location(world, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
        }

        return new FrameData(world, frameBlocks, interiorBlocks, minX, maxX, minY, maxY, minZ, maxZ, axis);
    }
}
