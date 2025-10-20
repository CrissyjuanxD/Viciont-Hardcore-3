package mobcap.spawn;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import mobcap.config.MobCapConfig;

import java.util.Random;

public class CustomSpawnManager implements Listener {
    private final JavaPlugin plugin;
    private final MobCapConfig config;
    private final Random random = new Random();

    public CustomSpawnManager(JavaPlugin plugin, MobCapConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!config.isCustomSpawnEnabled()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        EntityType entityType = entity.getType();

        // Solo aplicar a mobs hostiles configurados como custom
        SpawnMode spawnMode = config.getMobSpawnMode(entityType);
        if (spawnMode != SpawnMode.CUSTOM) {
            return;
        }

        // Permitir spawn durante el día para mobs configurados como custom
        World world = entity.getWorld();
        long time = world.getTime();
        boolean isDaytime = time >= 0 && time < 12300;

        if (isDaytime && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            // Verificar si hay jugadores cerca para spawn oval
            if (config.isOvalPatternEnabled() && config.isCloserToPlayerEnabled()) {
                Location spawnLocation = getOptimalSpawnLocation(entity.getLocation());
                if (spawnLocation != null && !spawnLocation.equals(entity.getLocation())) {
                    entity.teleport(spawnLocation);
                }
            }
        }
    }

    private Location getOptimalSpawnLocation(Location originalLocation) {
        Player nearestPlayer = getNearestPlayer(originalLocation);
        if (nearestPlayer == null) {
            return null;
        }

        Location playerLoc = nearestPlayer.getLocation();
        double radiusMultiplier = config.getRadiusMultiplier();

        // Crear patrón oval más cerca del jugador
        double maxDistance = 24 * radiusMultiplier; // Reducir distancia máxima
        double minDistance = 8 * radiusMultiplier;  // Distancia mínima para evitar spawn muy cerca

        for (int attempts = 0; attempts < 10; attempts++) {
            // Generar coordenadas en patrón oval
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

            // Patrón oval: más ancho en X, más estrecho en Z
            double x = playerLoc.getX() + Math.cos(angle) * distance;
            double z = playerLoc.getZ() + Math.sin(angle) * distance * 0.7; // Factor oval

            Location newLocation = new Location(
                    originalLocation.getWorld(),
                    x,
                    originalLocation.getWorld().getHighestBlockYAt((int) x, (int) z) + 1,
                    z
            );

            // Verificar que la ubicación sea válida
            if (isValidSpawnLocation(newLocation)) {
                return newLocation;
            }
        }

        return null;
    }

    private Player getNearestPlayer(Location location) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean isValidSpawnLocation(Location location) {
        // Verificar que no sea en agua, lava, o bloques sólidos
        return location.getBlock().isEmpty() &&
                location.clone().add(0, 1, 0).getBlock().isEmpty() &&
                !location.clone().subtract(0, 1, 0).getBlock().isEmpty();
    }
}