package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MobSoundManager {

    private static MobSoundManager instance;
    private final JavaPlugin plugin;

    // Registro de configuraciones de sonido (Key -> Datos)
    private final Map<NamespacedKey, SoundData> soundRegistry = new HashMap<>();

    // --- CACHÉ DE RENDIMIENTO ---
    // Mapea UUID -> Configuración (Si es custom)
    private final Map<UUID, SoundData> mobCache = new HashMap<>();
    // Mapea UUID -> Boolean (Si está aquí, es un mob Vanilla y lo ignoramos rápido)
    private final Set<UUID> ignoredCache = new HashSet<>();

    // Cachés de estado (Sync)
    private final Map<UUID, Long> lastAmbientMap = new HashMap<>();
    private final Map<UUID, Long> lastStepMap = new HashMap<>();
    private final Map<UUID, Location> lastLocationMap = new HashMap<>();

    // Timestamp de la última vez que vimos al mob (para limpieza)
    private final Map<UUID, Long> lastSeenMap = new HashMap<>();

    // --- CONFIGURACIÓN DE RENDIMIENTO ---
    private static final long AMBIENT_INTERVAL_MS = 4000;
    private static final long STEP_INTERVAL_MS = 350;
    private static final double MIN_MOVE_DIST_SQ = 0.2 * 0.2;
    private static final long CLEANUP_THRESHOLD_MS = 10000;

    // Límite de seguridad: No procesar más de X mobs por tick para proteger TPS
    private static final int MAX_ENTITIES_PER_TICK = 300;

    public MobSoundManager(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        startSoundTask();
        startCleanupTask();
    }

    public static void register(NamespacedKey key, Sound ambient, Sound step, float pitch, float volume) {
        if (instance != null) {
            instance.soundRegistry.put(key, new SoundData(ambient, step, pitch, volume));
        }
    }

    private void startSoundTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (soundRegistry.isEmpty()) return;

                long now = System.currentTimeMillis();
                Set<UUID> processedInThisTick = new HashSet<>();
                int processedCount = 0; // Contador de seguridad

                for (Player p : Bukkit.getOnlinePlayers()) {

                    // Si ya superamos el límite de audiciones por tick, cortamos por seguridad.
                    // Esto evita lag en granjas masivas o eventos con cientos de mobs.
                    if (processedCount >= MAX_ENTITIES_PER_TICK) break;

                    // Radio 16 es suficiente para escuchar pasos/ambient
                    for (Entity e : p.getNearbyEntities(16, 16, 16)) {

                        if (!(e instanceof LivingEntity) || !e.isValid()) continue;

                        UUID id = e.getUniqueId();

                        // 1. DEDUPLICACIÓN DE JUGADORES
                        // Si el mob ya fue procesado en este tick (por otro jugador cercano), saltar.
                        if (!processedInThisTick.add(id)) continue;

                        // 2. CHECK RÁPIDO DE IGNORADOS
                        // Si ya sabemos que es un mob vanilla, lo marcamos como visto y seguimos.
                        if (ignoredCache.contains(id)) {
                            lastSeenMap.put(id, now);
                            continue;
                        }

                        // 3. CHECK DE CACHÉ POSITIVA
                        SoundData data = mobCache.get(id);

                        // 4. CHECK DE PERSISTENT DATA (Lento, solo se hace 1 vez por mob)
                        if (data == null) {
                            boolean isCustom = false;
                            for (Map.Entry<NamespacedKey, SoundData> entry : soundRegistry.entrySet()) {
                                if (e.getPersistentDataContainer().has(entry.getKey(), PersistentDataType.BYTE)) {
                                    data = entry.getValue();
                                    mobCache.put(id, data);
                                    isCustom = true;
                                    break;
                                }
                            }

                            // Si tras revisar todo no es custom, lo metemos a ignorados
                            if (!isCustom) {
                                ignoredCache.add(id);
                                lastSeenMap.put(id, now);
                                continue; // Saltamos al siguiente mob
                            }
                        }

                        // 5. PROCESAR SONIDO
                        if (data != null) {
                            processEntitySound(e, data, now);
                            lastSeenMap.put(id, now);
                            processedCount++; // Solo contamos mobs que realmente procesamos sonidos

                            if (processedCount >= MAX_ENTITIES_PER_TICK) break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 5L);
    }

    private void processEntitySound(Entity e, SoundData data, long now) {
        UUID id = e.getUniqueId();

        // Ambient (Probabilidad ~5% cada check si pasó el cooldown)
        if (data.ambient != null) {
            long lastAmbient = lastAmbientMap.getOrDefault(id, 0L);
            if (now - lastAmbient > AMBIENT_INTERVAL_MS && Math.random() < 0.05) {
                playSound(e.getLocation(), data.ambient, data.volume, data.pitch);
                lastAmbientMap.put(id, now);
            }
        }

        // Steps (Solo si se mueve en suelo)
        if (data.step != null && e.isOnGround()) {
            Location currentLoc = e.getLocation();
            Location lastLoc = lastLocationMap.get(id);

            if (lastLoc != null) {
                if (currentLoc.distanceSquared(lastLoc) > MIN_MOVE_DIST_SQ) {
                    long lastStep = lastStepMap.getOrDefault(id, 0L);
                    if (now - lastStep > STEP_INTERVAL_MS) {
                        playSound(currentLoc, data.step, data.volume * 0.8f, data.pitch);
                        lastStepMap.put(id, now);
                    }
                }
            }
            lastLocationMap.put(id, currentLoc);
        }
    }

    private void playSound(Location loc, Sound sound, float vol, float pitch) {
        if (loc.getWorld() != null) {
            loc.getWorld().playSound(loc, sound, SoundCategory.HOSTILE, vol, pitch);
        }
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Eliminar cualquier mob (custom o ignorado) que no hayamos visto en 10s
                lastSeenMap.entrySet().removeIf(entry -> (now - entry.getValue()) > CLEANUP_THRESHOLD_MS);

                Set<UUID> activeMobs = lastSeenMap.keySet();

                // Sincronizar todas las cachés
                mobCache.keySet().retainAll(activeMobs);
                ignoredCache.retainAll(activeMobs); // Limpiamos también la caché de ignorados
                lastAmbientMap.keySet().retainAll(activeMobs);
                lastStepMap.keySet().retainAll(activeMobs);
                lastLocationMap.keySet().retainAll(activeMobs);
            }
        }.runTaskTimer(plugin, 600L, 600L);
    }

    public void shutdown() {
        soundRegistry.clear();
        mobCache.clear();
        ignoredCache.clear();
        lastAmbientMap.clear();
        lastStepMap.clear();
        lastLocationMap.clear();
        lastSeenMap.clear();
        instance = null;
    }

    private static class SoundData {
        final Sound ambient;
        final Sound step;
        final float pitch;
        final float volume;

        public SoundData(Sound ambient, Sound step, float pitch, float volume) {
            this.ambient = ambient;
            this.step = step;
            this.pitch = pitch;
            this.volume = volume;
        }
    }
}