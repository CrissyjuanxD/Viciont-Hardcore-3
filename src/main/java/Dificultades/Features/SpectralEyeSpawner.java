package Dificultades.Features;

import Dificultades.CustomMobs.SpectralEye;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SpectralEyeSpawner implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final SpectralEye spectralEye;

    // Configuración de spawn
    private static final double SPAWN_CHANCE = 0.02;
    private static final int MIN_Y = 30;
    private static final int MAX_Y = 200;
    private static final int MAX_LIGHT = 6;
    private static final int MIN_PLAYER_DISTANCE = 25;

    public SpectralEyeSpawner(JavaPlugin plugin, SpectralEye spectralEye) {
        this.plugin = plugin;
        this.spectralEye = spectralEye;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;

        if (random.nextDouble() > SPAWN_CHANCE) return;

        World world = event.getWorld();
        World.Environment env = world.getEnvironment();
        if (env != World.Environment.NORMAL &&
                env != World.Environment.NETHER &&
                env != World.Environment.THE_END) {
            return;
        }

        attemptSpawnInChunk(world, event.getChunk());
    }

    private void attemptSpawnInChunk(World world, Chunk chunk) {
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        for (int i = 0; i < 3; i++) {
            int x = baseX + random.nextInt(16);
            int z = baseZ + random.nextInt(16);
            int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y + 1);

            Location loc = new Location(world, x + 0.5, y, z + 0.5);

            if (isValidSpawnLocation(loc)) {
                spawnSpectralEye(loc);
                break;
            }
        }
    }

    private boolean isValidSpawnLocation(Location loc) {
        if (!loc.getBlock().getType().isAir()) return false;

        if (loc.getBlock().getLightFromSky() > MAX_LIGHT) return false;

        for (int i = 1; i <= 3; i++) {
            if (!loc.clone().add(0, i, 0).getBlock().getType().isAir()) {
                return false;
            }
        }

        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) < MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE) {
                return false;
            }
        }

        return true;
    }

    private void spawnSpectralEye(Location loc) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isValidSpawnLocation(loc)) {
                Phantom eye = (Phantom) loc.getWorld().spawnEntity(loc, EntityType.PHANTOM);
                spectralEye.transformSpawnSpectralEye(eye);
                eye.setHealth(1 + ThreadLocalRandom.current().nextInt(3));

                loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);
                loc.getWorld().spawnParticle(Particle.PORTAL, loc, 10, 0.5, 0.5, 0.5, 0.1);
            }
        }, 5L);
    }
}
