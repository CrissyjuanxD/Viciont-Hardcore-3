package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

public class MobSoundManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Entity> customMobs = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private int taskId;

    public MobSoundManager(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin); // Registrar como listener
        startGlobalSoundTask();
    }

    public void addCustomMob(Entity mob) {
        customMobs.put(mob.getUniqueId(), mob);
        mob.setMetadata("custom_mob", new FixedMetadataValue(plugin, true)); // Marcar como custom
    }

    public void removeCustomMob(Entity mob) {
        customMobs.remove(mob.getUniqueId());
        lastLocations.remove(mob.getUniqueId());
    }

    private void startGlobalSoundTask() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Entity mob : new ArrayList<>(customMobs.values())) {
                try {
                    if (mob == null || !mob.isValid() || mob.isDead()) {
                        customMobs.remove(mob.getUniqueId());
                        continue;
                    }

                    if (mob instanceof Zombie && mob.hasMetadata("corrupted_zombie")) {
                        playCorruptedZombieSounds((Zombie) mob);
                    }
                    // Aquí puedes agregar más condiciones para otros tipos de mobs
                    if (mob instanceof Spider && mob.hasMetadata("corruptedspider")) {
                        playCorruptedSpiderSounds((Spider) mob);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error procesando mob: " + e.getMessage());
                }
            }
        }, 0L, 20L).getTaskId();
    }

    private void playCorruptedZombieSounds(Zombie zombie) {
        // Sonido ambiental (40% de probabilidad)
        if (new Random().nextInt(100) < 40) {
            playSoundForNearbyPlayers(
                    zombie.getLocation(),
                    Sound.ENTITY_ZOMBIE_AMBIENT,
                    16, // Radio
                    1.0f, // Volumen
                    0.6f // Pitch
            );
        }

        // Sonido de pasos (si se mueve)
        if (zombie.isOnGround() && hasMoved(zombie)) {
            playSoundForNearbyPlayers(
                    zombie.getLocation(),
                    Sound.ENTITY_ZOMBIE_STEP,
                    10, // Radio más pequeño para pasos
                    1.0f, // Volumen más bajo
                    0.6f // Pitch
            );
        }
    }

    private void playCorruptedSpiderSounds(Spider spider) {
        // Sonido ambiental (30% de probabilidad)
        if (new Random().nextInt(100) < 30) {
            playSoundForNearbyPlayers(
                    spider.getLocation(),
                    Sound.ENTITY_SPIDER_AMBIENT,
                    16, // Radio
                    1.0f, // Volumen
                    0.6f // Pitch
            );
        }

        // Sonido de pasos (si se mueve)
        if (spider.isOnGround() && hasMoved(spider)) {
            playSoundForNearbyPlayers(
                    spider.getLocation(),
                    Sound.ENTITY_SPIDER_STEP,
                    10, // Radio más pequeño para pasos
                    1.0f, // Volumen más bajo
                    0.6f // Pitch
            );
        }
    }

/*    private boolean hasMoved(Zombie zombie) {
        Location currentLoc = zombie.getLocation();
        Location lastLoc = lastLocations.get(zombie.getUniqueId());
        lastLocations.put(zombie.getUniqueId(), currentLoc.clone());

        if (lastLoc == null) return false;

        // Usar distancia en lugar de comparación exacta
        return currentLoc.distanceSquared(lastLoc) > 0.01;
    }*/

    private boolean hasMoved(Entity entity) {
        Location currentLoc = entity.getLocation();
        Location lastLoc = lastLocations.get(entity.getUniqueId());
        lastLocations.put(entity.getUniqueId(), currentLoc.clone());

        if (lastLoc == null) return false;

        // Usar distancia en lugar de comparación exacta
        return currentLoc.distanceSquared(lastLoc) > 0.01;
    }

    private void playSoundForNearbyPlayers(Location location, Sound sound, double radius, float volume, float pitch) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radius * radius) {
                player.playSound(location, sound, SoundCategory.HOSTILE, volume, pitch);
            }
        }
    }

    public void stop() {
        Bukkit.getScheduler().cancelTask(taskId);
    }
}