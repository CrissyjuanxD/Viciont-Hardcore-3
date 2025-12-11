package InfestedCaves;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InfestedPopulator extends BlockPopulator {
    private final JavaPlugin plugin;
    // Cache para asegurar que los chunks vecinos existen antes de decorar
    private static final Set<ChunkCoords> chunks = ConcurrentHashMap.newKeySet();
    private static final Set<ChunkCoords> unpopulatedChunks = ConcurrentHashMap.newKeySet();

    public InfestedPopulator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        ChunkCoords chunkCoordinates = new ChunkCoords(chunkX, chunkZ);

        if (!chunks.contains(chunkCoordinates)) {
            chunks.add(chunkCoordinates);
            unpopulatedChunks.add(chunkCoordinates);
        }

        // Verificar si todos los chunks vecinos están cargados
        for (ChunkCoords unpopulatedChunk : unpopulatedChunks.toArray(new ChunkCoords[0])) {
            if (areAllNeighborsLoaded(unpopulatedChunk)) {
                actuallyPopulate(world, random, world.getChunkAt(unpopulatedChunk.x, unpopulatedChunk.z));
                unpopulatedChunks.remove(unpopulatedChunk);
            }
        }
    }

    private boolean areAllNeighborsLoaded(ChunkCoords coords) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                if (!chunks.contains(coords.offset(x, z))) return false;
            }
        }
        return true;
    }

    private void actuallyPopulate(World world, Random random, Chunk chunk) {
        int attempts = 3;

        for (int i = 0; i < attempts; i++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);

            // Escaneo vertical rápido
            // Empezamos desde abajo (-50) hacia arriba (100) buscando suelo
            for (int y = -50; y < 100; y+=3) {
                // Verificar suelo válido
                if (isValidSoil(chunk.getBlock(x, y, z).getType()) &&
                        chunk.getBlock(x, y+1, z).getType() == Material.AIR) {

                    Location loc = chunk.getBlock(x, y + 1, z).getLocation();

                    // Intentamos generar.
                    generateCustomTree(world, loc, random);

                    // Queremos que siga el bucle 'for' para intentar poner los otros 13 árboles.
                    break; // Rompemos el escaneo de altura (Y) para pasar al siguiente intento (i)
                }
            }
        }
    }

    private boolean isValidSoil(Material mat) {
        return mat == Material.SCULK ||
                mat == Material.CRYING_OBSIDIAN ||
                mat == Material.SCULK_CATALYST ||
                mat == Material.COARSE_DIRT ||
                mat == Material.HONEYCOMB_BLOCK;
    }

    private void generateCustomTree(World world, Location location, Random random) {
        // Guardamos el suelo original
        Material originalSoil = location.clone().subtract(0, 1, 0).getBlock().getType();

        // Forzamos End Stone temporalmente para engañar al generador de Chorus
        location.clone().subtract(0, 1, 0).getBlock().setType(Material.END_STONE);

        boolean success = world.generateTree(location, TreeType.CHORUS_PLANT, new BlockChangeDelegate() {
            @Override
            public boolean setBlockData(int x, int y, int z, @NotNull BlockData blockData) {
                Material mat = blockData.getMaterial();

                // Reemplazo de materiales
                if (mat == Material.CHORUS_PLANT) {
                    world.getBlockAt(x, y, z).setType(Material.NETHER_BRICK_FENCE);
                } else if (mat == Material.CHORUS_FLOWER) {
                    world.getBlockAt(x, y, z).setType(Material.PEARLESCENT_FROGLIGHT);
                } else {
                    return true; // Ignorar aire u otros
                }
                return true;
            }

            @Override
            public @NotNull BlockData getBlockData(int x, int y, int z) {
                return world.getBlockAt(x, y, z).getBlockData();
            }

            @Override
            public int getHeight() { return 256; }

            @Override
            public boolean isEmpty(int x, int y, int z) {
                return world.getBlockAt(x, y, z).getType() == Material.AIR;
            }
        });

        // Restaurar el suelo original si no era aire ni endstone
        if (originalSoil != Material.AIR && originalSoil != Material.END_STONE) {
            location.clone().subtract(0, 1, 0).getBlock().setType(originalSoil);
        }
    }

    private static class ChunkCoords {
        public final int x;
        public final int z;

        public ChunkCoords(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public ChunkCoords offset(int dx, int dz) {
            return new ChunkCoords(x + dx, z + dz);
        }

        @Override
        public int hashCode() {
            return (x + z) * (x + z + 1) / 2 + x;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ChunkCoords other = (ChunkCoords) obj;
            return x == other.x && z == other.z;
        }
    }
}