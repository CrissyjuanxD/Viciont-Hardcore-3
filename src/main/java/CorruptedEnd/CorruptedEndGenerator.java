package CorruptedEnd;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CorruptedEndGenerator extends ChunkGenerator {
    private final JavaPlugin plugin;
    private static final int BASE_HEIGHT = 100;
    private final SplittableRandom random = new SplittableRandom();

    public CorruptedEndGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ChunkData generateChunkData(World world, Random cRandom, int chunkX, int chunkZ, BiomeGrid biomes) {
        // Generadores de ruido mejorados
        SimplexOctaveGenerator islandGenerator = new SimplexOctaveGenerator(new Random(world.getSeed()), 8);
        islandGenerator.setScale(0.02D);

        SimplexOctaveGenerator biomeGenerator = new SimplexOctaveGenerator(new Random(world.getSeed() + 1), 4);
        biomeGenerator.setScale(0.003D);

        SimplexOctaveGenerator heightVariationGenerator = new SimplexOctaveGenerator(new Random(world.getSeed() + 2), 6);
        heightVariationGenerator.setScale(0.01D);

        SimplexOctaveGenerator caveGenerator = new SimplexOctaveGenerator(new Random(world.getSeed() + 3), 4);
        caveGenerator.setScale(0.05D);

        ChunkData chunk = createChunkData(world);

        for (int X = 0; X < 16; X++) {
            for (int Z = 0; Z < 16; Z++) {
                int globalX = chunkX * 16 + X;
                int globalZ = chunkZ * 16 + Z;

                // Ruido para la forma de la isla
                double islandNoise = islandGenerator.noise(globalX, globalZ, 0.5D, 0.5D);
                int baseThickness = (int) (islandNoise * 15);

                if (baseThickness <= 0) continue;

                // Determinar tipo de bioma
                double biomeValue = biomeGenerator.noise(globalX, globalZ, 0.5D, 0.5D);
                BiomeType biomeType = getBiomeType(biomeValue, globalX, globalZ);

                // Variación de altura según el bioma
                double heightVariation = heightVariationGenerator.noise(globalX, globalZ, 0.5D, 0.5D);
                int heightOffset = getHeightOffset(biomeType, heightVariation);

                int baseY = BASE_HEIGHT + heightOffset;

                // Generar la isla con diferentes alturas
                generateIslandTerrain(chunk, X, Z, baseY, baseThickness, biomeType, caveGenerator, globalX, globalZ);

                // Decoración superficial
                if (baseThickness > 8 && random.nextInt(8) == 0) { // Más decoración
                    // Encontrar la superficie sólida más alta para colocar decoración
                    int surfaceY = findSurfaceY(chunk, X, Z, baseY, baseThickness);
                    if (surfaceY > 0) {
                        generateSurfaceDecoration(chunk, X, Z, surfaceY + 1, biomeType);
                    }
                }

                // Generar lagos para Celestial Forest (solo en el centro de islas grandes)
                if (biomeType == BiomeType.CELESTIAL_FOREST && baseThickness > 12 && random.nextInt(80) == 0) {
                    int surfaceY = findSurfaceY(chunk, X, Z, baseY, baseThickness);
                    if (surfaceY > 0 && isValidLakeLocation(chunk, X, Z, surfaceY)) {
                        generateLake(chunk, X, Z, surfaceY);
                    }
                }
            }
        }

        return chunk;
    }

    private BiomeType getBiomeType(double biomeValue, int globalX, int globalZ) {
        // Distribución de biomas más compleja y natural
        double distance = Math.sqrt(globalX * globalX + globalZ * globalZ);
        double angle = Math.atan2(globalZ, globalX);

        // Modifica el valor del bioma basado en la distancia y el ángulo
        double modifiedBiomeValue = biomeValue + Math.sin(angle * 2) * 0.2 + (distance % 1000) * 0.001;

        if (modifiedBiomeValue < -0.3) {
            return BiomeType.SCULK_PLAINS;
        } else if (modifiedBiomeValue < 0.0) {
            return BiomeType.CRIMSON_WASTES;
        } else if (modifiedBiomeValue < 0.4) {
            return BiomeType.CELESTIAL_FOREST;
        } else {
            return BiomeType.OBSIDIAN_PEAKS;
        }
    }

    private int getHeightOffset(BiomeType biomeType, double heightVariation) {
        switch (biomeType) {
            case CELESTIAL_FOREST:
                // Variaciones de terreno de 10 bloques arriba y abajo
                return (int) (heightVariation * 10);
            case OBSIDIAN_PEAKS:
                // Montañas más grandes y elevadas (solo hacia arriba)
                return (int) (Math.abs(heightVariation) * 25) + 10;
            case CRIMSON_WASTES:
                return (int) (heightVariation * 10);
            default:
                return (int) (heightVariation * 5);
        }
    }

    private void generateIslandTerrain(ChunkData chunk, int x, int z, int baseY, int thickness,
                                       BiomeType biomeType, SimplexOctaveGenerator caveGenerator,
                                       int globalX, int globalZ) {
        Material primaryBlock = biomeType.getPrimaryBlock();
        Material secondaryBlock = biomeType.getSecondaryBlock();
        Material accentBlock = biomeType.getAccentBlock();

        // Ajustar tamaño según el bioma
        int adjustedThickness = thickness;
        if (biomeType == BiomeType.CELESTIAL_FOREST) {
            // Islas más grandes que los biomas originales
            adjustedThickness = (int) (thickness * 1.3);
        } else if (biomeType == BiomeType.OBSIDIAN_PEAKS) {
            // Montañas más grandes y completas
            adjustedThickness = (int) (thickness * 1.5);
        }

        // Generar capas hacia arriba
        for (int i = 0; i < adjustedThickness / 3; i++) {
            int y = baseY + i;
            if (y >= 0 && y < 256) {
                // Verificar si debe haber una cueva
                double caveNoise = caveGenerator.noise(globalX, y, globalZ, 0.5D, 0.5D);
                if (caveNoise > 0.6) continue; // Crear cueva

                Material blockToPlace = getLayerMaterial(i, adjustedThickness / 3, primaryBlock, secondaryBlock, accentBlock);
                chunk.setBlock(x, y, z, blockToPlace);
            }
        }

        // Generar capas hacia abajo
        for (int i = 1; i <= adjustedThickness; i++) {
            int y = baseY - i;
            if (y >= 0 && y < 256) {
                // Verificar si debe haber una cueva
                double caveNoise = caveGenerator.noise(globalX, y, globalZ, 0.5D, 0.5D);
                if (caveNoise > 0.6) continue; // Crear cueva

                Material blockToPlace = getLayerMaterial(i, adjustedThickness, primaryBlock, secondaryBlock, accentBlock);
                chunk.setBlock(x, y, z, blockToPlace);
            }
        }
    }

    private Material getLayerMaterial(int layer, int maxLayers, Material primary, Material secondary, Material accent) {
        double ratio = (double) layer / maxLayers;
        double randomValue = random.nextDouble();

        if (ratio < 0.2 && randomValue < 0.3) {
            return accent;
        } else if (randomValue < 0.7) {
            return primary;
        } else {
            return secondary;
        }
    }

    private void generateSurfaceDecoration(ChunkData chunk, int x, int z, int y, BiomeType biomeType) {
        Material[] decorations = biomeType.getDecorationBlocks();

        // Aumentar probabilidad de sculk sensors y shriekers
        Material decoration;
        if (random.nextInt(100) < 60) { // 60% probabilidad de sculk sensor/shrieker
            decoration = random.nextBoolean() ? Material.SCULK_SENSOR : Material.SCULK_SHRIEKER;
        } else {
            decoration = decorations[random.nextInt(decorations.length)];
        }

        if (y >= 0 && y < 255) {
            chunk.setBlock(x, y, z, decoration);

            // Configurar shriekers como activos
            if (decoration == Material.SCULK_SHRIEKER) {
                // Esto se manejará en el evento de chunk load para configurar el blockdata
            }

            // Añadir bloques adicionales para algunos casos
            if (decoration == Material.SCULK_SHRIEKER && random.nextInt(3) == 0) {
                chunk.setBlock(x, y - 1, z, Material.SCULK_CATALYST);
            }

            // Sculk veins alrededor de sensors y shriekers
            if (random.nextInt(4) == 0 && (decoration == Material.SCULK_SENSOR || decoration == Material.SCULK_SHRIEKER)) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if ((dx != 0 || dz != 0) && random.nextBoolean() &&
                                x + dx >= 0 && x + dx < 16 && z + dz >= 0 && z + dz < 16) {
                            // Solo colocar si hay superficie sólida
                            if (chunk.getType(x + dx, y - 1, z + dz) != Material.AIR) {
                                chunk.setBlock(x + dx, y, z + dz, Material.SCULK_VEIN);
                            }
                        }
                    }
                }
            }
        }
    }

    private int findSurfaceY(ChunkData chunk, int x, int z, int baseY, int thickness) {
        // Buscar la superficie sólida más alta en esta columna
        for (int y = baseY + thickness; y >= baseY - thickness; y--) {
            if (y >= 0 && y < 256 && chunk.getType(x, y, z) != Material.AIR) {
                return y;
            }
        }
        return -1; // No se encontró superficie
    }

    private boolean isValidLakeLocation(ChunkData chunk, int centerX, int centerZ, int surfaceY) {
        // Verificar que hay suficiente terreno sólido alrededor para crear un lago
        int solidBlocks = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                    if (chunk.getType(x, surfaceY - 1, z) != Material.AIR) {
                        solidBlocks++;
                    }
                }
            }
        }
        return solidBlocks >= 35; // Al menos 35 de 49 bloques deben ser sólidos
    }

    private void generateLake(ChunkData chunk, int centerX, int centerZ, int surfaceY) {
        int radius = 2 + random.nextInt(2); // Lagos más pequeños (2-3 de radio)

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;

                if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance <= radius && chunk.getType(x, surfaceY - 1, z) != Material.AIR) {
                        // Crear depresión en el terreno y llenar con agua
                        chunk.setBlock(x, surfaceY - 1, z, Material.WATER);
                        // Asegurar que hay terreno debajo del agua
                        if (chunk.getType(x, surfaceY - 2, z) == Material.AIR) {
                            chunk.setBlock(x, surfaceY - 2, z, Material.BLUE_TERRACOTTA);
                        }
                    }
                }
            }
        }
    }

    @NotNull
    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.singletonList(new CorruptedTreePopulator(plugin));
    }
}