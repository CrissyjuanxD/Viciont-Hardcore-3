package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobSoundManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Entity> customMobs = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAmbientSound = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastStepSound = new ConcurrentHashMap<>();
    private BukkitRunnable soundTask;

    // Configuración de sonidos
    private static final long AMBIENT_COOLDOWN = 4000; // 4 segundos entre sonidos ambientales
    private static final long STEP_COOLDOWN = 300; // 300ms entre sonidos de pasos
    private static final double MIN_MOVEMENT_DISTANCE = 0.5; // Distancia mínima para considerar movimiento

    public MobSoundManager(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startGlobalSoundTask();
    }

    public void addCustomMob(Entity mob) {
        if (mob == null || !mob.isValid()) return;

        UUID mobId = mob.getUniqueId();
        customMobs.put(mobId, mob);
        lastLocations.put(mobId, mob.getLocation().clone());

        plugin.getLogger().info("Mob custom agregado al sound manager: " + mob.getType() + " - " + mobId);
    }

    public void removeCustomMob(Entity mob) {
        if (mob == null) return;

        UUID mobId = mob.getUniqueId();
        customMobs.remove(mobId);
        lastLocations.remove(mobId);
        lastAmbientSound.remove(mobId);
        lastStepSound.remove(mobId);

        plugin.getLogger().info("Mob custom removido del sound manager: " + mob.getType() + " - " + mobId);
    }

    private void startGlobalSoundTask() {
        soundTask = new BukkitRunnable() {
            @Override
            public void run() {
                processCustomMobs();
            }
        };
        soundTask.runTaskTimer(plugin, 0L, 5L); // Ejecutar cada 5 ticks para mejor precisión
    }

    private void processCustomMobs() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Entity>> iterator = customMobs.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Entity> entry = iterator.next();
            UUID mobId = entry.getKey();
            Entity mob = entry.getValue();

            try {
                // Verificar si el mob sigue siendo válido
                if (mob == null || !mob.isValid() || mob.isDead()) {
                    iterator.remove();
                    lastLocations.remove(mobId);
                    lastAmbientSound.remove(mobId);
                    lastStepSound.remove(mobId);
                    continue;
                }

                // Procesar sonidos según el tipo de mob
                if (mob instanceof Zombie zombie) {
                    processZombieSounds(zombie, mobId, currentTime);
                } else if (mob instanceof Spider spider) {
                    processSpiderSounds(spider, mobId, currentTime);
                } else if (mob instanceof Creeper creeper) {
                    processCreeperSounds(creeper, mobId, currentTime);
                } else if (mob instanceof Skeleton skeleton) {
                    processSkeletonSounds(skeleton, mobId, currentTime);
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error procesando sonidos para mob " + mobId + ": " + e.getMessage());
                iterator.remove();
            }
        }
    }

    private void processZombieSounds(Zombie zombie, UUID mobId, long currentTime) {
        // Sonidos ambientales
        if (shouldPlayAmbientSound(mobId, currentTime)) {
            playSoundForNearbyPlayers(
                    zombie.getLocation(),
                    Sound.ENTITY_ZOMBIE_AMBIENT,
                    16,
                    1.0f,
                    0.6f
            );
            lastAmbientSound.put(mobId, currentTime);
        }

        // Sonidos de pasos
        if (hasMoved(zombie, mobId) && zombie.isOnGround() && shouldPlayStepSound(mobId, currentTime)) {
            playSoundForNearbyPlayers(
                    zombie.getLocation(),
                    Sound.ENTITY_ZOMBIE_STEP,
                    10,
                    0.8f,
                    0.6f
            );
            lastStepSound.put(mobId, currentTime);
        }
    }

    private void processSpiderSounds(Spider spider, UUID mobId, long currentTime) {
        // Sonidos ambientales
        if (shouldPlayAmbientSound(mobId, currentTime)) {
            playSoundForNearbyPlayers(
                    spider.getLocation(),
                    Sound.ENTITY_SPIDER_AMBIENT,
                    16,
                    1.0f,
                    0.6f
            );
            lastAmbientSound.put(mobId, currentTime);
        }

        // Sonidos de pasos
        if (hasMoved(spider, mobId) && spider.isOnGround() && shouldPlayStepSound(mobId, currentTime)) {
            playSoundForNearbyPlayers(
                    spider.getLocation(),
                    Sound.ENTITY_SPIDER_STEP,
                    10,
                    0.8f,
                    0.6f
            );
            lastStepSound.put(mobId, currentTime);
        }
    }

    private void processCreeperSounds(Creeper creeper, UUID mobId, long currentTime) {
        // Sonidos ambientales
        if (shouldPlayAmbientSound(mobId, currentTime)) {
            playSoundForNearbyPlayers(
                    creeper.getLocation(),
                    Sound.BLOCK_COPPER_BULB_STEP,
                    16,
                    1.0f,
                    0.6f
            );
            lastAmbientSound.put(mobId, currentTime);
        }

        // Sonidos de pasos (los creepers no tienen sonido de pasos vanilla, pero podemos usar uno genérico)
        if (hasMoved(creeper, mobId) && creeper.isOnGround() && shouldPlayStepSound(mobId, currentTime)) {
            playSoundForNearbyPlayers(
                    creeper.getLocation(),
                    Sound.BLOCK_GRASS_STEP,
                    8,
                    0.5f,
                    0.8f
            );
            lastStepSound.put(mobId, currentTime);
        }
    }

    private void processSkeletonSounds(Skeleton skeleton, UUID mobId, long currentTime) {
        // Sonidos ambientales
        if (shouldPlayAmbientSound(mobId, currentTime)) {
            playSoundForNearbyPlayers(
                    skeleton.getLocation(),
                    Sound.ENTITY_SKELETON_AMBIENT,
                    16,
                    1.0f,
                    0.6f
            );
            lastAmbientSound.put(mobId, currentTime);
        }

        // Sonidos de pasos
        if (hasMoved(skeleton, mobId) && skeleton.isOnGround() && shouldPlayStepSound(mobId, currentTime)) {
            playSoundForNearbyPlayers(
                    skeleton.getLocation(),
                    Sound.ENTITY_SKELETON_STEP,
                    10,
                    0.8f,
                    0.6f
            );
            lastStepSound.put(mobId, currentTime);
        }
    }

    private boolean shouldPlayAmbientSound(UUID mobId, long currentTime) {
        long lastTime = lastAmbientSound.getOrDefault(mobId, 0L);
        return (currentTime - lastTime) >= AMBIENT_COOLDOWN;
    }

    private boolean shouldPlayStepSound(UUID mobId, long currentTime) {
        long lastTime = lastStepSound.getOrDefault(mobId, 0L);
        return (currentTime - lastTime) >= STEP_COOLDOWN;
    }

    private boolean hasMoved(Entity entity, UUID mobId) {
        Location currentLoc = entity.getLocation();
        Location lastLoc = lastLocations.get(mobId);

        if (lastLoc == null) {
            lastLocations.put(mobId, currentLoc.clone());
            return false;
        }

        // Verificar si se movió lo suficiente
        double distanceSquared = currentLoc.distanceSquared(lastLoc);
        boolean moved = distanceSquared >= (MIN_MOVEMENT_DISTANCE * MIN_MOVEMENT_DISTANCE);

        if (moved) {
            lastLocations.put(mobId, currentLoc.clone());
        }

        return moved;
    }

    private void playSoundForNearbyPlayers(Location location, Sound sound, double radius, float volume, float pitch) {
        if (location.getWorld() == null) return;

        double radiusSquared = radius * radius;

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSquared) {
                try {
                    player.playSound(location, sound, SoundCategory.HOSTILE, volume, pitch);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error reproduciendo sonido para jugador " + player.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    // Eventos para limpiar automáticamente
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        removeCustomMob(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpiar mobs que ya no tienen jugadores cerca
        cleanupDistantMobs();
    }

    private void cleanupDistantMobs() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Entity>> iterator = customMobs.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Entity> entry = iterator.next();
            Entity mob = entry.getValue();

            if (mob == null || !mob.isValid() || mob.isDead()) {
                iterator.remove();
                continue;
            }

            // Verificar si hay jugadores cerca (radio de 64 bloques)
            boolean hasNearbyPlayers = false;
            for (Player player : mob.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(mob.getLocation()) <= 64 * 64) {
                    hasNearbyPlayers = true;
                    break;
                }
            }

            // Si no hay jugadores cerca, remover del tracking
            if (!hasNearbyPlayers) {
                iterator.remove();
                lastLocations.remove(entry.getKey());
                lastAmbientSound.remove(entry.getKey());
                lastStepSound.remove(entry.getKey());
            }
        }
    }

    // Método para obtener estadísticas de debug
    public void printDebugInfo() {
        plugin.getLogger().info("=== MobSoundManager Debug Info ===");
        plugin.getLogger().info("Mobs tracked: " + customMobs.size());

        Map<EntityType, Integer> mobCounts = new HashMap<>();
        for (Entity mob : customMobs.values()) {
            if (mob != null && mob.isValid()) {
                mobCounts.merge(mob.getType(), 1, Integer::sum);
            }
        }

        for (Map.Entry<EntityType, Integer> entry : mobCounts.entrySet()) {
            plugin.getLogger().info("- " + entry.getKey() + ": " + entry.getValue());
        }
        plugin.getLogger().info("================================");
    }

    // Método para limpiar todo al deshabilitar el plugin
    public void shutdown() {
        if (soundTask != null && !soundTask.isCancelled()) {
            soundTask.cancel();
        }

        customMobs.clear();
        lastLocations.clear();
        lastAmbientSound.clear();
        lastStepSound.clear();

        plugin.getLogger().info("MobSoundManager deshabilitado correctamente");
    }

    // Getters para debug
    public int getTrackedMobsCount() {
        return customMobs.size();
    }

    public Set<UUID> getTrackedMobIds() {
        return new HashSet<>(customMobs.keySet());
    }
}