package CorruptedEnd;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class PortalManager implements Listener {
    private final JavaPlugin plugin;
    private final CorruptedEnd corruptedEnd;
    private final Set<Location> hexagonalPortalBlocks = new HashSet<>();

    public PortalManager(JavaPlugin plugin, CorruptedEnd corruptedEnd) {
        this.plugin = plugin;
        this.corruptedEnd = corruptedEnd;
    }

    public void createReturnPortal() {
        Location center = new Location(corruptedEnd.getCorruptedWorld(), 0, 120, 0);
        createHexagonalPortalPlatform(center);
        activateHexagonalPortal(center);
    }

    public void createOverworldPortal(Location location) {
        // Encontrar superficie segura
        Location safeLocation = findSafeLocation(location);
        createHexagonalPortalPlatform(safeLocation);
        activateHexagonalPortal(safeLocation);
    }

    private void createHexagonalPortalPlatform(Location center) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Limpiar área
        clearArea(center, 8);

        // Crear plataforma hexagonal más grande que el portal
        Material[] terracottaColors = {
                Material.RED_TERRACOTTA, Material.BLUE_TERRACOTTA, Material.GREEN_TERRACOTTA,
                Material.YELLOW_TERRACOTTA, Material.PURPLE_TERRACOTTA, Material.ORANGE_TERRACOTTA,
                Material.LIGHT_BLUE_TERRACOTTA, Material.PINK_TERRACOTTA
        };

        int colorIndex = 0;
        for (int radius = 0; radius <= 8; radius++) { // Plataforma más grande
            for (double angle = 0; angle < 360; angle += 10) {
                double radians = Math.toRadians(angle);
                int x = centerX + (int) (radius * Math.cos(radians));
                int z = centerZ + (int) (radius * Math.sin(radians));

                // Crear plataforma de solo 1 bloque de altura
                if (isWithinHexagon(x - centerX, z - centerZ, 8)) {
                    Location blockLoc = new Location(world, x, centerY, z); // ✅ CORREGIDO: centerY en lugar de y
                    blockLoc.getBlock().setType(terracottaColors[colorIndex % terracottaColors.length]);
                }
                colorIndex++;
            }
        }

        // Crear estructura del portal rectangular
        createRectangularPortalFrame(center);

        // Añadir iluminación adicional alrededor de la plataforma
        addPlatformLighting(center);
    }

    private void createRectangularPortalFrame(Location center) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Crear marco rectangular de bedrock (7 de ancho, 12 de alto)
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 7; x++) {
                Location buildLoc = new Location(world, centerX + x - 3, centerY + y, centerZ);

                // Marco de bedrock (bordes)
                if (x == 0 || x == 6 || y == 0 || y == 11) {
                    buildLoc.getBlock().setType(Material.BEDROCK);
                }
            }
        }
    }

    private void activateHexagonalPortal(Location center) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Llenar el interior rectangular con END_GATEWAY
        for (int y = 1; y < 11; y++) {
            for (int x = 1; x < 6; x++) {
                Location portalLoc = new Location(world, centerX + x - 3, centerY + y, centerZ);
                portalLoc.getBlock().setType(Material.END_GATEWAY);
                portalLoc.getBlock().setMetadata("HexagonalPortal", new FixedMetadataValue(plugin, true));
                hexagonalPortalBlocks.add(portalLoc);
            }
        }

        // Efectos visuales y sonoros
        world.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 2.0f, 1.0f);
        world.spawnParticle(Particle.PORTAL, center.clone().add(0, 3, 0), 100, 2, 2, 2, 0.1);
        world.spawnParticle(Particle.END_ROD, center.clone().add(0, 3, 0), 50, 2, 2, 2, 0.05);
    }

    private boolean isWithinHexagon(int x, int z, int radius) {
        // Aproximación simple de hexágono usando distancia
        double distance = Math.sqrt(x * x + z * z);
        return distance <= radius;
    }

    private void createLine(World world, int x1, int y, int z1, int x2, int y2, int z2, Material material) {
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int x = x1;
        int z = z1;
        int n = 1 + dx + dz;
        int x_inc = (x2 > x1) ? 1 : -1;
        int z_inc = (z2 > z1) ? 1 : -1;
        int error = dx - dz;
        dx *= 2;
        dz *= 2;

        for (; n > 0; --n) {
            new Location(world, x, y, z).getBlock().setType(material);

            if (error > 0) {
                x += x_inc;
                error -= dz;
            } else {
                z += z_inc;
                error += dx;
            }
        }
    }

    private void addPlatformLighting(Location center) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Añadir glowstone alrededor de la plataforma
        for (double angle = 0; angle < 360; angle += 45) {
            double radians = Math.toRadians(angle);
            int x = centerX + (int) (10 * Math.cos(radians));
            int z = centerZ + (int) (10 * Math.sin(radians));

            Location lightLoc = new Location(world, x, centerY + 1, z);
            lightLoc.getBlock().setType(Material.GLOWSTONE);
        }
    }

    private void clearArea(Location center, int radius) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                for (int y = centerY - 2; y <= centerY + 8; y++) {
                    new Location(world, x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
    }

    private Location findSafeLocation(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        // Buscar superficie desde arriba
        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() != Material.AIR && block.getType() != Material.WATER) {
                return new Location(world, x, y + 1, z);
            }
        }

        // Si no encuentra superficie, usar altura por defecto
        return new Location(world, x, 100, z);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        Player player = event.getPlayer();
        Block block = event.getTo().getBlock();

        if (block.getType() == Material.END_GATEWAY &&
                block.hasMetadata("HexagonalPortal")) {

            if (player.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) {
                // Ir al overworld
                World overworld = Bukkit.getWorlds().get(0);
                Location spawnLoc = overworld.getSpawnLocation();

                // Buscar portal existente en el overworld o crear uno nuevo
                Location portalLoc = findNearestPortal(spawnLoc, 100);
                if (portalLoc == null) {
                    portalLoc = spawnLoc.clone().add(0, 10, 0);
                    createOverworldPortal(portalLoc);
                }

                player.teleport(portalLoc);
                player.sendMessage(ChatColor.GREEN + "Has regresado al Overworld");

            } else {
                // Ir al Corrupted End
                if (corruptedEnd.getCorruptedWorld() == null) {
                    corruptedEnd.createCorruptedWorld();
                }
                player.teleport(corruptedEnd.getCorruptedWorld().getSpawnLocation());
                player.sendMessage(ChatColor.DARK_PURPLE + "Has entrado al Corrupted End");
            }

            // Efectos de teleportación
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 20, 0.5, 1, 0.5);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        // Cancelar portales de End Gateway naturales en Corrupted End
        if (player.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) {
            if (event.getCause() == PlayerPortalEvent.TeleportCause.END_GATEWAY) {
                // Solo permitir nuestros portales hexagonales
                if (!event.getFrom().getBlock().hasMetadata("HexagonalPortal")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private Location findNearestPortal(Location center, int radius) {
        World world = center.getWorld();

        for (Location portalLoc : hexagonalPortalBlocks) {
            if (portalLoc.getWorld().equals(world) &&
                    portalLoc.distance(center) <= radius) {
                return portalLoc.clone().add(0, -1, 0);
            }
        }

        return null;
    }
}