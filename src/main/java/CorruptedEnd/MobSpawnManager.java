package CorruptedEnd;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.type.SculkShrieker;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class MobSpawnManager implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private BukkitRunnable spawnTask;

    public MobSpawnManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startSpawning() {
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnNaturalMobs();
            }
        };
        spawnTask.runTaskTimer(plugin, 100L, 100L); // Cada 5 segundos
    }

    public void stopSpawning() {
        if (spawnTask != null) {
            spawnTask.cancel();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) return;

        // Activar shriekers en chunks recién cargados
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            activateShriekersInChunk(event.getChunk());
        }, 10L);
    }

    private void activateShriekersInChunk(org.bukkit.Chunk chunk) {
        World world = chunk.getWorld();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getMaxHeight(); y++) {
                    org.bukkit.block.Block block = chunk.getBlock(x, y, z);

                    if (block.getType() == Material.SCULK_SHRIEKER) {
                        if (block.getBlockData() instanceof SculkShrieker) {
                            SculkShrieker shrieker = (SculkShrieker) block.getBlockData();
                            shrieker.setCanSummon(true); // Permitir que spawnee Wardens
                            block.setBlockData(shrieker);
                        }
                    }
                }
            }
        }
    }

    private void spawnNaturalMobs() {
        World corruptedWorld = Bukkit.getWorld(CorruptedEnd.WORLD_NAME);
        if (corruptedWorld == null || corruptedWorld.getPlayers().isEmpty()) return;

        for (Player player : corruptedWorld.getPlayers()) {
            if (random.nextInt(100) < 15) { // 15% probabilidad por intento
                spawnMobNearPlayer(player);
            }
        }
    }

    private void spawnMobNearPlayer(Player player) {
        Location playerLoc = player.getLocation();

        // Buscar ubicación de spawn en el aire (para mobs voladores)
        Location spawnLoc = findAirSpawnLocation(playerLoc);
        if (spawnLoc == null) return;

        // Seleccionar mob basado en probabilidades
        EntityType mobType = selectMobType();

        // Spawnear el mob
        spawnLoc.getWorld().spawnEntity(spawnLoc, mobType);

        plugin.getLogger().info("Spawneado " + mobType.name() + " cerca de " + player.getName());
    }

    private Location findAirSpawnLocation(Location playerLoc) {
        World world = playerLoc.getWorld();
        Random random = new Random();

        for (int attempts = 0; attempts < 10; attempts++) {
            // Buscar ubicación en un radio de 15-30 bloques
            int distance = 15 + random.nextInt(15);
            double angle = random.nextDouble() * Math.PI * 2;

            int x = playerLoc.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = playerLoc.getBlockZ() + (int) (Math.sin(angle) * distance);

            // Para mobs voladores, buscar espacio aéreo
            for (int y = playerLoc.getBlockY() + 10; y <= playerLoc.getBlockY() + 30; y++) {
                Location testLoc = new Location(world, x, y, z);

                // Verificar que hay espacio libre (3 bloques de altura)
                if (isAirSpace(testLoc, 3)) {
                    return testLoc;
                }
            }
        }

        return null;
    }

    private boolean isAirSpace(Location loc, int height) {
        for (int i = 0; i < height; i++) {
            if (loc.clone().add(0, i, 0).getBlock().getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private EntityType selectMobType() {
        int rand = random.nextInt(100);

        if (rand < 40) {
            return EntityType.VEX;
        } else if (rand < 70) {
            return EntityType.PHANTOM;
        } else {
            return EntityType.GHAST;
        }
    }

    public void shutdown() {
        stopSpawning();
    }
}