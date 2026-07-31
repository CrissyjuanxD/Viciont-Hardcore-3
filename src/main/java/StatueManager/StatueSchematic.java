package StatueManager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StatueSchematic implements Listener {

    private final JavaPlugin plugin;
    private final StatueManager statueManager;

    private static final NamespacedKey KEY_ID =
            new NamespacedKey("viciont", "statue_id");

    private final Set<UUID> registeredUUIDs = Collections.synchronizedSet(new HashSet<>());

    private final Queue<Chunk> pendingChunks = new ArrayDeque<>();

    private static final int CHUNKS_PER_TICK = 5;

    public StatueSchematic(JavaPlugin plugin, StatueManager statueManager) {
        this.plugin        = plugin;
        this.statueManager = statueManager;
        startBatchProcessor();
        startWorldEditScanner();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  ESCÁNER PERIÓDICO para WorldEdit / entidades pegadas sin evento
    // ────────────────────────────────────────────────────────────────────────
    private void startWorldEditScanner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof ArmorStand) {
                            UUID uid = entity.getUniqueId();
                            if (registeredUUIDs.contains(uid)) continue;

                            ArmorStand stand = (ArmorStand) entity;
                            if (stand.getPersistentDataContainer().has(KEY_ID, PersistentDataType.STRING)) {
                                restoreStatue(stand);
                                registeredUUIDs.add(uid);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  EntitySpawnEvent — captura copias por Clone/Summon/etc.
    // ────────────────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (e.getEntity() instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) e.getEntity();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (stand.isValid() && stand.getPersistentDataContainer().has(KEY_ID, PersistentDataType.STRING)) {
                    UUID uid = stand.getUniqueId();
                    if (!registeredUUIDs.contains(uid)) {
                        restoreStatue(stand);
                        registeredUUIDs.add(uid);
                    }
                }
            }, 1L);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SCAN INICIAL — fragmentado en chunks
    // ────────────────────────────────────────────────────────────────────────
    public void scanAllWorlds() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    pendingChunks.offer(chunk);
                }
            }
            plugin.getLogger().info("[StatueFixer] Chunks en cola para escanear: " + pendingChunks.size());
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    //  BATCH PROCESSOR
    // ────────────────────────────────────────────────────────────────────────
    private void startBatchProcessor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingChunks.isEmpty()) return;

                int processed = 0;
                while (!pendingChunks.isEmpty() && processed < CHUNKS_PER_TICK) {
                    Chunk chunk = pendingChunks.poll();
                    if (chunk == null || !chunk.isLoaded()) continue;
                    scanChunk(chunk);
                    processed++;
                }
            }
        }.runTaskTimer(plugin, 40L, 1L);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CHUNK LOAD EVENT
    // ────────────────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        pendingChunks.offer(e.getChunk());
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SCAN DE UN CHUNK
    // ────────────────────────────────────────────────────────────────────────
    private void scanChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof ArmorStand)) continue;

            UUID uid = entity.getUniqueId();
            if (registeredUUIDs.contains(uid)) continue;

            ArmorStand stand = (ArmorStand) entity;
            if (!stand.getPersistentDataContainer().has(KEY_ID, PersistentDataType.STRING)) continue;

            restoreStatue(stand);
            registeredUUIDs.add(uid);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  RESTAURAR ESTATUA
    //  registerStatue ya aplica: updateGlowingColor + updateStatueName + startParticleTask
    // ────────────────────────────────────────────────────────────────────────
    private void restoreStatue(ArmorStand stand) {
        StatueData data = new StatueData(stand);
        stand.setVisible(data.isVisible());
        statueManager.registerStatue(stand);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  LIMPIEZA
    // ────────────────────────────────────────────────────────────────────────
    public void onStatueRemoved(UUID uuid) {
        registeredUUIDs.remove(uuid);
    }

    public void cleanup() {
        pendingChunks.clear();
        registeredUUIDs.clear();
    }
}