package CorruptedEnd;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.type.SculkShrieker;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class MobSpawnManager implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public MobSpawnManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startSpawning() {
        // Ya no necesitamos un Task repetitivo. El generador maneja el spawn natural.
        // Mantenemos este método vacío por compatibilidad si lo llamas desde la clase principal.
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!event.getLocation().getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) return;

        // Solo modificar spawns naturales o de chunk generation
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL &&
                event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CHUNK_GEN) return;

        LivingEntity entity = event.getEntity();

        // Personalizar mobs naturales
        if (entity.getType() == EntityType.ENDERMAN) {
            // Ejemplo: Endermans corruptos más fuertes
            if (random.nextBoolean()) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.setCustomName("§5Corrupted Enderman");
                entity.setCustomNameVisible(false);
            }
        } else if (entity.getType() == EntityType.PHANTOM) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) return;
        if (!event.isNewChunk()) return; // Optimización: Solo procesar chunks nuevos

        // Activar shriekers (Lógica existente optimizada)
        plugin.getServer().getScheduler().runTask(plugin, () -> { // Correr sync pero en cola
            World world = event.getChunk().getWorld();
            int cx = event.getChunk().getX() * 16;
            int cz = event.getChunk().getZ() * 16;

            // Escaneo rápido simplificado (buscar solo en superficies probables)
            for (int x = 0; x < 16; x+=4) { // Saltos para optimizar búsqueda si es densa
                for (int z = 0; z < 16; z+=4) {
                    for (int y = 60; y < 150; y+=2) {
                        if (event.getChunk().getBlock(x, y, z).getType() == Material.SCULK_SHRIEKER) {
                            var block = event.getChunk().getBlock(x, y, z);
                            if (block.getBlockData() instanceof SculkShrieker shrieker) {
                                shrieker.setCanSummon(true);
                                block.setBlockData(shrieker);
                            }
                        }
                    }
                }
            }
        });
    }
}