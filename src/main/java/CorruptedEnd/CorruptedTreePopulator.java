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
        // Intentar generar árboles múltiples veces
        for (int attempts = 0; attempts < 3; attempts++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = world.getMaxHeight() - 1;

            // Encontrar superficie sólida
            while (y > 0 && chunk.getBlock(x, y, z).getType() == Material.AIR) {
                --y;
            }

            if (y > 0 && y < 255 && y >= 90 && y < 150) {
                Location treeLocation = chunk.getBlock(x, y + 1, z).getLocation();
                BiomeType biomeType = determineBiomeType(treeLocation);

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

    private void generateCustomTreeStructure(World world, Location location, BiomeType biomeType, Random random) {
        // Usar la generación original como base
        world.generateTree(location, TreeType.CHORUS_PLANT, new BlockChangeDelegate() {
            @Override
            public boolean setBlockData(int x, int y, int z, @NotNull BlockData blockData) {
                Material replacement;
                if (blockData.getMaterial() == Material.CHORUS_FLOWER) {
                    replacement = getLeafMaterial(biomeType);
                } else if (blockData.getMaterial() == Material.CHORUS_PLANT) {
                    replacement = getTrunkMaterial(biomeType);
                } else {
                    return true;
                }
                world.getBlockAt(x, y, z).setType(replacement);

                // Añadir iluminación para Obsidian Peaks
                if (biomeType == BiomeType.OBSIDIAN_PEAKS && replacement == Material.GRAY_GLAZED_TERRACOTTA) {
                    Location lightLoc = new Location(world, x, y + 1, z);
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
            public @NotNull BlockData getBlockData(int x, int y, int z) {
                return world.getBlockAt(x, y, z).getBlockData();
            }

            @Override
            public int getHeight() {
                return 255;
            }

            @Override
            public boolean isEmpty(int x, int y, int z) {
                return world.getBlockAt(x, y, z).getType() == Material.AIR;
            }
        });

        // Para Obsidian Peaks, añadir ramificaciones adicionales
        if (biomeType == BiomeType.OBSIDIAN_PEAKS) {
            addExtraRamifications(world, location, random);
        }
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

    private void generateLeaves(World world, Location center, Material leafMaterial, BiomeType biomeType, Random random) {
        // Generar hojas en forma de cruz
        int[][] leafPositions = {
                {0, 0, 0}, {0, 1, 0}, // Centro y arriba
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, // Lados
                {1, 0, 1}, {-1, 0, -1}, {1, 0, -1}, {-1, 0, 1} // Diagonales
        };

        for (int[] pos : leafPositions) {
            if (random.nextDouble() < 0.8) { // 80% probabilidad por hoja
                Location leafLoc = center.clone().add(pos[0], pos[1], pos[2]);
                if (leafLoc.getBlock().getType() == Material.AIR) {
                    leafLoc.getBlock().setType(leafMaterial);

                    // Añadir iluminación para Obsidian Peaks
                    if (biomeType == BiomeType.OBSIDIAN_PEAKS && leafMaterial == Material.GRAY_GLAZED_TERRACOTTA) {
                        Location lightLoc = leafLoc.clone().add(0, 1, 0);
                        if (lightLoc.getBlock().getType() == Material.AIR) {
                            lightLoc.getBlock().setType(Material.LIGHT);
                            lightLoc.getBlock().setBlockData(
                                    Bukkit.createBlockData(Material.LIGHT, "[level=8]")
                            );
                        }
                    }
                }
            }
        }
    }

    private void addExtraRamifications(World world, Location base, Random random) {
        // Añadir ramificaciones adicionales más variadas para Obsidian Peaks
        int extraBranches = 3 + random.nextInt(4); // 3-6 ramas extra

        for (int i = 0; i < extraBranches; i++) {
            int branchY = 3 + random.nextInt(8); // Altura variable
            int branchLength = 3 + random.nextInt(5); // Longitud variable

            // Dirección completamente aleatoria
            double angle = random.nextDouble() * Math.PI * 2;
            int dx = (int) Math.round(Math.cos(angle));
            int dz = (int) Math.round(Math.sin(angle));

            Material trunkMaterial = getTrunkMaterial(BiomeType.OBSIDIAN_PEAKS);
            Material leafMaterial = getLeafMaterial(BiomeType.OBSIDIAN_PEAKS);

            // Generar rama
            for (int j = 1; j <= branchLength; j++) {
                Location branchLoc = base.clone().add(dx * j, branchY, dz * j);
                if (branchLoc.getBlock().getType() == Material.AIR) {
                    branchLoc.getBlock().setType(trunkMaterial);
                }

                // Hojas al final de la rama con iluminación
                if (j == branchLength && random.nextBoolean()) {
                    branchLoc.getBlock().setType(leafMaterial);

                    // Añadir iluminación
                    Location lightLoc = branchLoc.clone().add(0, 1, 0);
                    if (lightLoc.getBlock().getType() == Material.AIR) {
                        lightLoc.getBlock().setType(Material.LIGHT);
                        lightLoc.getBlock().setBlockData(
                                Bukkit.createBlockData(Material.LIGHT, "[level=8]")
                        );
                    }
                }
            }
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