package CorruptedEnd;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CorruptedTreePopulator extends BlockPopulator {
    private static Set<ChunkCoords> chunks = ConcurrentHashMap.newKeySet();
    private static Set<ChunkCoords> unpopulatedChunks = ConcurrentHashMap.newKeySet();
    private final JavaPlugin plugin;

    public CorruptedTreePopulator(JavaPlugin plugin) {
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
        return chunks.contains(coords.offset(-1, -1)) &&
                chunks.contains(coords.offset(-1, 0)) &&
                chunks.contains(coords.offset(-1, 1)) &&
                chunks.contains(coords.offset(0, -1)) &&
                chunks.contains(coords.offset(0, 1)) &&
                chunks.contains(coords.offset(1, -1)) &&
                chunks.contains(coords.offset(1, 0)) &&
                chunks.contains(coords.offset(1, 1));
    }

    private void actuallyPopulate(World world, Random random, Chunk chunk) {

        int intentos = 5;

        for (int i = 0; i < intentos; i++) {

            if (random.nextInt(100) < 40) continue;

            int x = random.nextInt(16);
            int z = random.nextInt(16);

            // Optimización: Empezar a buscar desde una altura razonable en lugar del techo del mundo
            int y = 250;

            // Buscar suelo sólido
            while (y > 0 && chunk.getBlock(x, y, z).getType() == Material.AIR) {
                --y;
            }

            // Rango de altura (Ajustado a 210 como pediste antes)
            if (y > 0 && y < 255 && y >= 90 && y < 210) {

                Location treeLocation = chunk.getBlock(x, y + 1, z).getLocation();
                BiomeType biomeType = determineBiomeType(treeLocation);

                // Si el bioma es válido (tiene suelo correcto), generamos
                if (biomeType != null) {
                    generateOriginalTree(world, treeLocation, biomeType, random);
                }
            }
        }
    }

    private BiomeType determineBiomeType(Location location) {
        // Determinar el bioma basado en el bloque base
        Material groundBlock = location.clone().subtract(0, 1, 0).getBlock().getType();

        if (groundBlock == Material.SCULK || groundBlock == Material.BLUE_TERRACOTTA) {
            // Verificar si hay blue terracotta cerca para confirmar Celestial Forest
            boolean hasBlueTerracotta = false;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (location.clone().add(dx, -1, dz).getBlock().getType() == Material.BLUE_TERRACOTTA) {
                        hasBlueTerracotta = true;
                        break;
                    }
                }
            }
            return hasBlueTerracotta ? BiomeType.CELESTIAL_FOREST : BiomeType.SCULK_PLAINS;
        } else if (groundBlock == Material.OBSIDIAN) {
            return BiomeType.OBSIDIAN_PEAKS;
        } else if (groundBlock == Material.CRIMSON_HYPHAE) {
            return BiomeType.CRIMSON_WASTES;
        }

        return BiomeType.SCULK_PLAINS;
    }

    private void generateOriginalTree(World world, Location location, BiomeType biomeType, Random random) {
        // Usar la generación original como base para todos los biomas
        world.generateTree(location, TreeType.CHORUS_PLANT, new BlockChangeDelegate() {
            @Override
            public boolean setBlockData(int i, int i1, int i2, @NotNull BlockData blockData) {
                Material replacement;
                if (blockData.getMaterial() == Material.CHORUS_FLOWER) {
                    replacement = getLeafMaterial(biomeType);
                } else if (blockData.getMaterial() == Material.CHORUS_PLANT) {
                    replacement = getTrunkMaterial(biomeType);
                } else {
                    return true;
                }
                world.getBlockAt(i, i1, i2).setType(replacement);

                // Añadir iluminación para Obsidian Peaks
                if (biomeType == BiomeType.OBSIDIAN_PEAKS && replacement == Material.GRAY_GLAZED_TERRACOTTA) {
                    Location lightLoc = new Location(world, i, i1 + 1, i2);
                    if (lightLoc.getBlock().getType() == Material.AIR) {
                        lightLoc.getBlock().setType(Material.LIGHT);
                        lightLoc.getBlock().setBlockData(
                                Bukkit.createBlockData(Material.LIGHT, "[level=8]")
                        );
                    }
                }

                return true;
            }

            @Override
            public @NotNull BlockData getBlockData(int i, int i1, int i2) {
                return world.getBlockAt(i, i1, i2).getBlockData();
            }

            @Override
            public int getHeight() {
                return 255;
            }

            @Override
            public boolean isEmpty(int i, int i1, int i2) {
                return world.getBlockAt(i, i1, i2).getType() == Material.AIR;
            }
        });

    }

    private Material getTrunkMaterial(BiomeType biomeType) {
        switch (biomeType) {
            case CELESTIAL_FOREST:
                return Material.PRISMARINE_WALL;
            case OBSIDIAN_PEAKS:
                return Material.NETHER_BRICK_WALL;
            case CRIMSON_WASTES:
                return Material.RED_NETHER_BRICK_WALL;
            default: // SCULK_PLAINS
                return Material.BLACKSTONE_WALL;
        }
    }

    private Material getLeafMaterial(BiomeType biomeType) {
        switch (biomeType) {
            case CELESTIAL_FOREST:
                return Material.VERDANT_FROGLIGHT;
            case OBSIDIAN_PEAKS:
                return Material.GRAY_GLAZED_TERRACOTTA;
            case CRIMSON_WASTES:
                return Material.SHROOMLIGHT;
            default: // SCULK_PLAINS
                return Material.SEA_LANTERN;
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