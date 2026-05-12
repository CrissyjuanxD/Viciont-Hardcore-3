package CorruptedEnd;

import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class PortalManager implements Listener {
    private final JavaPlugin plugin;
    private final CorruptedEnd corruptedEnd;
    private final NamespacedKey PORTAL_KEY;

    public PortalManager(JavaPlugin plugin, CorruptedEnd corruptedEnd) {
        this.plugin = plugin;
        this.corruptedEnd = corruptedEnd;
        this.PORTAL_KEY = new NamespacedKey(plugin, "corrupted_portal_type");
    }

    public void createReturnPortal() {
        if (corruptedEnd.getCorruptedWorld() == null) return;
        Location center = new Location(corruptedEnd.getCorruptedWorld(), 0, 120, 0);
        createRectangularPortal(center, false); // false = hacia overworld
    }

    public void createOverworldPortal(Location location) {
        Location safeLocation = findSafeLocation(location);
        createRectangularPortal(safeLocation, true); // true = hacia corrupted end
    }

    private void createRectangularPortal(Location center, boolean toCorruptedEnd) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Limpiar área y crear plataforma base
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                world.getBlockAt(cx + x, cy - 1, cz + z).setType(Material.OBSIDIAN); // Suelo base
                for (int y = 0; y <= 11; y++) {
                    world.getBlockAt(cx + x, cy + y, cz + z).setType(Material.AIR);
                }
            }
        }

        // 2. Crear Marco Rectangular (7 ancho x 12 alto exterior)
        // El hueco interior será de 5 ancho x 10 alto
        for (int y = 0; y < 12; y++) {
            for (int x = -3; x <= 3; x++) {
                boolean isEdge = (x == -3 || x == 3 || y == 0 || y == 11);
                if (isEdge) {
                    world.getBlockAt(cx + x, cy + y, cz).setType(Material.BEDROCK);
                }
            }
        }

        // --- CORRECCIÓN DE POSICIONAMIENTO DEL DISPLAY ---

        // El centro geométrico exacto del hueco (Hueco va de Y=1 a Y=10 -> Centro = 6.0)
        // +0.5 en X y Z para centrarlo en el bloque central
        Location displayLoc = new Location(world, cx + 0.5, cy + 6.0, cz + 0.5);

        // Eliminar portales viejos cercanos para evitar solapamiento
        removePortalsNearby(displayLoc);

        BlockDisplay display = (BlockDisplay) world.spawnEntity(displayLoc, EntityType.BLOCK_DISPLAY);

        // Configurar bloque visual
        display.setBlock(Bukkit.createBlockData(Material.BLUE_GLAZED_TERRACOTTA));

        // --- TRANSFORMACIÓN CORREGIDA ---
        // Escala: 5 ancho, 10 alto.
        // Traslación: Movemos -2.5 (mitad de ancho) y -5.0 (mitad de alto) para que
        // la entidad quede exactamente en el centro del rectángulo visual.
        Transformation transformation = new Transformation(
                new Vector3f(-2.5f, -5.0f, 0f),   // <--- AQUÍ ESTÁ EL FIX (Centrado)
                new AxisAngle4f(0, 0, 0, 0),      // Sin rotación
                new Vector3f(5f, 10f, 0.1f),      // Escala: 5x10 bloques
                new AxisAngle4f(0, 0, 0, 0)
        );

        display.setTransformation(transformation);
        display.setGlowing(true);
        display.setGlowColorOverride(Color.AQUA);

        // Guardar datos persistentes
        String type = toCorruptedEnd ? "TO_CORRUPTED" : "TO_OVERWORLD";
        display.getPersistentDataContainer().set(PORTAL_KEY, PersistentDataType.STRING, type);

        // Efectos
        world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
    }

    private void removePortalsNearby(Location loc) {
        // Aumentamos un poco el rango vertical por si acaso
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 6, 12, 6)) {
            if (e instanceof BlockDisplay && e.getPersistentDataContainer().has(PORTAL_KEY, PersistentDataType.STRING)) {
                e.remove();
            }
        }
    }

    private Location findSafeLocation(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        // Buscar superficie sólida
        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
            if (world.getBlockAt(x, y, z).getType().isSolid()) {
                return new Location(world, x, y + 1, z);
            }
        }
        return new Location(world, x, 100, z);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        Location pLoc = player.getLocation();

        // Buscar BlockDisplay muy cerca
        for (Entity entity : pLoc.getWorld().getNearbyEntities(pLoc, 0.8, 2, 0.8)) {
            if (entity instanceof BlockDisplay) {
                String type = entity.getPersistentDataContainer().get(PORTAL_KEY, PersistentDataType.STRING);

                if (type != null) {
                    handleTeleport(player, type.equals("TO_CORRUPTED"));
                    return;
                }
            }
        }
    }

    private void handleTeleport(Player player, boolean toCorruptedEnd) {
        if (player.hasMetadata("Teleporting")) return;
        player.setMetadata("Teleporting", new FixedMetadataValue(plugin, true));

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        player.spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 20);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.removeMetadata("Teleporting", plugin);
            Location target;

            if (toCorruptedEnd) {
                if (corruptedEnd.getCorruptedWorld() == null) corruptedEnd.createCorruptedWorld();
                World cWorld = corruptedEnd.getCorruptedWorld();
                // Aparecer fuera del portal para no buclear
                target = new Location(cWorld, 0.5, 121, 4.5);
                target.getBlock().getRelative(0, -1, 0).setType(Material.OBSIDIAN);
                player.sendMessage(ChatColor.DARK_PURPLE + "Bienvenido al Corrupted End.");
            } else {
                World overworld = Bukkit.getWorlds().get(0);
                target = player.getBedSpawnLocation();
                if (target == null) target = overworld.getSpawnLocation();

                // Evitar caer en bucle si el spawn está cerca del portal
                if (isNearPortal(target)) {
                    target.add(3, 0, 3);
                }
                player.sendMessage(ChatColor.GREEN + "Regresando al Overworld.");
            }
            player.teleport(target);
        }, 10L);
    }

    private boolean isNearPortal(Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof BlockDisplay && e.getPersistentDataContainer().has(PORTAL_KEY, PersistentDataType.STRING)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getPlayer().getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) {
            if (event.getCause() == PlayerPortalEvent.TeleportCause.END_GATEWAY ||
                    event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL) {
                event.setCancelled(true);
            }
        }
    }
}