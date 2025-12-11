package InfestedCaves;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Random;
import java.util.List;
import org.bukkit.generator.BlockPopulator;
import java.util.Collections;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.PointedDripstone;

public class InfestedGenerator extends ChunkGenerator {
    private final JavaPlugin plugin;
    // Límite del mundo (radio desde el centro)
    private static final int WORLD_RADIUS = 3000; // 3000x3000 total size
    private static final int BORDER_SMOOTHING = 50; // Distancia para suavizar la pared

    public InfestedGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        ChunkData chunk = createChunkData(world);
        SimplexOctaveGenerator noise = new SimplexOctaveGenerator(new Random(world.getSeed()), 8);
        noise.setScale(0.01);

        int realX = chunkX * 16;
        int realZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int currentX = realX + x;
                int currentZ = realZ + z;

                // Distancia al borde más cercano
                int distX = Math.abs(currentX);
                int distZ = Math.abs(currentZ);

                // --- GENERACIÓN DE PARED DE BEDROCK (LIMITE DEL MUNDO) ---
                if (distX >= WORLD_RADIUS || distZ >= WORLD_RADIUS) {
                    for (int y = -60; y <= 120; y++) {
                        chunk.setBlock(x, y, z, Material.BEDROCK);
                    }
                    continue; // No generamos nada más aquí
                }

                // Generación de ruido 3D
                double[] densities = new double[183]; // -61 a 121

                for (int y = -61; y <= 121; y++) {
                    densities[y + 61] = noise.noise(currentX, y, currentZ, 0.5, 0.5);
                }

                for (int y = -60; y <= 120; y++) {
                    int index = y + 61;
                    double density = densities[index];

                    // Bedrock borders (suelo y techo del mundo)
                    if (y == -60 || y == 120) {
                        chunk.setBlock(x, y, z, Material.BEDROCK);
                        continue;
                    }

                    // --- GENERACIÓN DEL TERRENO ---
                    if (density > 0.2) {
                        // Es AIRE (Cueva)
                        chunk.setBlock(x, y, z, Material.AIR);

                        // Decoración de Suelo
                        if (y > -60 && densities[index - 1] <= 0.2) {
                            decorateFloor(chunk, x, y - 1, z, random);
                        }

                        // Decoración de Techo
                        if (y < 120 && densities[index + 1] <= 0.2) {
                            decorateCeiling(chunk, x, y + 1, z, random);
                        }

                    } else {
                        // Es SÓLIDO (Pared)
                        if (chunk.getType(x, y, z) == Material.AIR) {
                            chunk.setBlock(x, y, z, Material.SCULK);

                            // Ores raros dentro de las paredes
                            if (random.nextInt(500) == 0) { // Probabilidad de aparición del grupo

                                // Determinar tamaño del grupo (1, 2 o 3)
                                double chance = random.nextDouble();
                                int veinSize;

                                if (chance < 0.10) {
                                    veinSize = 3; // 10% probabilidad de 3 unidos
                                } else if (chance < 0.35) {
                                    veinSize = 2; // 25% probabilidad de 2 unidos (0.10 + 0.25)
                                } else {
                                    veinSize = 1; // 65% probabilidad de 1 solo
                                }

                                generateVein(chunk, x, y, z, veinSize, random);
                            }
                        }
                    }

                    biomes.setBiome(x, y, z, Biome.DEEP_DARK);
                }

                decorateFloorCape(chunk, x, -59, z, random);

                decorateCeiling(chunk, x, 119, z, random);
                // Suavizado lateral
                if (distX > WORLD_RADIUS - BORDER_SMOOTHING || distZ > WORLD_RADIUS - BORDER_SMOOTHING) {
                    smoothBorder(chunk, x, z, distX, distZ);
                }
            }
        }
        return chunk;
    }

    private void generateVein(ChunkData chunk, int startX, int startY, int startZ, int size, Random random) {
        int currentX = startX;
        int currentY = startY;
        int currentZ = startZ;

        for (int i = 0; i < size; i++) {
            // Verificar límites del chunk (0-15) y altura (-60 a 120) para no dar error
            if (currentX >= 0 && currentX < 16 && currentZ >= 0 && currentZ < 16 && currentY > -60 && currentY < 120) {

                // Solo colocar si no es aire (para no flotar en cuevas) y no es bedrock
                Material type = chunk.getType(currentX, currentY, currentZ);
                if (type != Material.AIR && type != Material.BEDROCK) {
                    chunk.setBlock(currentX, currentY, currentZ, Material.RED_GLAZED_TERRACOTTA);
                }
            }

            // Moverse aleatoriamente a un bloque adyacente para el siguiente ore del grupo
            int dir = random.nextInt(6);
            switch (dir) {
                case 0: currentX++; break;
                case 1: currentX--; break;
                case 2: currentY++; break;
                case 3: currentY--; break;
                case 4: currentZ++; break;
                case 5: currentZ--; break;
            }
        }
    }

    private void smoothBorder(ChunkData chunk, int x, int z, int distX, int distZ) {
        // Cuanto más cerca del borde, más probabilidad de cerrar la cueva con sculk
        double progressX = (double)(distX - (WORLD_RADIUS - BORDER_SMOOTHING)) / BORDER_SMOOTHING;
        double progressZ = (double)(distZ - (WORLD_RADIUS - BORDER_SMOOTHING)) / BORDER_SMOOTHING;
        double progress = Math.max(progressX, progressZ);

        if (progress > 0.8) {
            for (int y = -60; y <= 120; y++) {
                if (chunk.getType(x, y, z) == Material.AIR) {
                    chunk.setBlock(x, y, z, Material.SCULK);
                }
            }
        }
    }

    private void decorateFloor(ChunkData chunk, int x, int y, int z, Random random) {
        if (random.nextInt(1000) < 13) {
            if (random.nextBoolean()) {
                chunk.setBlock(x, y + 1, z, Material.SCULK_SHRIEKER);
            } else {
                chunk.setBlock(x, y + 1, z, Material.SCULK_SENSOR);
            }
            chunk.setBlock(x, y, z, Material.SCULK);
        } else {
            // Normal Floor Decoration
            double chance = random.nextDouble();
            if (chance < 0.01) {
                chunk.setBlock(x, y, z, Material.SCULK_CATALYST);
            } else if (chance < 0.12) {
                chunk.setBlock(x, y, z, Material.BROWN_GLAZED_TERRACOTTA);
            } else if (chance < 0.14) {
                chunk.setBlock(x, y, z, Material.HONEYCOMB_BLOCK);
            } else {
                chunk.setBlock(x, y, z, Material.SCULK);
            }
        }
    }

    private void decorateFloorCape(ChunkData chunk, int x, int y, int z, Random random) {
        double chance = random.nextDouble();
        if (chance < 0.01) {
            chunk.setBlock(x, y, z, Material.SCULK_CATALYST);
        } else if (chance < 0.12) {
            chunk.setBlock(x, y, z, Material.BROWN_GLAZED_TERRACOTTA);
        } else if (chance < 0.14) {
            chunk.setBlock(x, y, z, Material.HONEYCOMB_BLOCK);
        } else {
            chunk.setBlock(x, y, z, Material.SCULK);
        }
    }

    private void decorateCeiling(ChunkData chunk, int x, int y, int z, Random random) {
        if (random.nextInt(100) < 5) {
            chunk.setBlock(x, y, z, Material.SCULK);

            if (random.nextBoolean()) {
                AmethystCluster amethyst1 = (AmethystCluster) Material.MEDIUM_AMETHYST_BUD.createBlockData();
                amethyst1.setFacing(BlockFace.DOWN);
                chunk.setBlock(x, y - 1, z, amethyst1);
            } else {
                AmethystCluster amethyst = (AmethystCluster) Material.LARGE_AMETHYST_BUD.createBlockData();
                amethyst.setFacing(BlockFace.DOWN);
                chunk.setBlock(x, y - 1, z, amethyst);
            }
        } else {
            double chance = random.nextDouble();
            if (chance < 0.015) {
                chunk.setBlock(x, y, z, Material.CRYING_OBSIDIAN);
            } else if (chance < 0.10) {
                chunk.setBlock(x, y, z, Material.BROWN_GLAZED_TERRACOTTA);
            } else {
                chunk.setBlock(x, y, z, Material.SCULK);
            }
        }
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.singletonList(new InfestedPopulator(plugin));
    }
}