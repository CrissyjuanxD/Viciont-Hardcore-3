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

public class InfestedGenerator extends ChunkGenerator {
    private final JavaPlugin plugin;
    // Límite del mundo
    private static final int WORLD_RADIUS = 3000;
    private static final int BORDER_SMOOTHING = 50;

    // Configuración Spawn Natural
    private static final int SPAWN_RADIUS = 70; // Radio plano
    private static final int SPAWN_TRANSITION = 40; // Bloques para suavizar la transición a cueva normal

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

                // Cálculos de distancia
                int distX = Math.abs(currentX);
                int distZ = Math.abs(currentZ);
                double distToSpawn = Math.sqrt(currentX * currentX + currentZ * currentZ);

                // --- GENERACIÓN DE PARED DE BEDROCK (LIMITE DEL MUNDO) ---
                if (distX >= WORLD_RADIUS || distZ >= WORLD_RADIUS) {
                    for (int y = -60; y <= 120; y++) {
                        chunk.setBlock(x, y, z, Material.BEDROCK);
                    }
                    continue;
                }

                // Generación de ruido 3D
                double[] densities = new double[183]; // -61 a 121

                // Calculamos la influencia del Spawn (0.0 = lejos, 1.0 = centro)
                double spawnInfluence = 0;
                if (distToSpawn < SPAWN_RADIUS + SPAWN_TRANSITION) {
                    if (distToSpawn <= SPAWN_RADIUS) {
                        spawnInfluence = 1.0;
                    } else {
                        spawnInfluence = 1.0 - ((distToSpawn - SPAWN_RADIUS) / SPAWN_TRANSITION);
                    }
                }

                for (int y = -61; y <= 121; y++) {
                    double noiseValue = noise.noise(currentX, y, currentZ, 0.5, 0.5);

                    // --- MODIFICACIÓN DE DENSIDAD ---
                    if (spawnInfluence > 0) {
                        // SOLO limpiamos hacia ARRIBA (aire) para que el templo no quede enterrado.
                        if (y >= -58) {
                            noiseValue += (spawnInfluence * 1.2);
                        }
                    }

                    densities[y + 61] = noiseValue;
                }

                for (int y = -60; y <= 120; y++) {
                    int index = y + 61;

                    // Bedrock borders
                    if (y == -60 || y == 120) {
                        chunk.setBlock(x, y, z, Material.BEDROCK);
                        continue;
                    }

                    double density = densities[index];

                    // --- GENERACIÓN DEL TERRENO ---
                    if (density > 0.2) {
                        // Es AIRE
                        chunk.setBlock(x, y, z, Material.AIR);

                        // Decoración normal (Piso y Techo de cuevas)
                        // Mantenemos esto restringido en el spawn SOLO para los sensores/shriekers
                        // que salen aleatorios, para que no molesten dentro del templo.
                        if (spawnInfluence < 0.5) {
                            if (y > -60 && densities[index - 1] <= 0.2) {
                                decorateFloor(chunk, x, y - 1, z, random);
                            }
                            if (y < 120 && densities[index + 1] <= 0.2) {
                                decorateCeiling(chunk, x, y + 1, z, random);
                            }
                        }

                    } else {
                        // Es SÓLIDO
                        if (chunk.getType(x, y, z) == Material.AIR) {
                            chunk.setBlock(x, y, z, Material.SCULK);

                            // Ores
                            if (spawnInfluence < 0.1 && random.nextInt(500) == 0) {
                                double chance = random.nextDouble();
                                int veinSize = (chance < 0.10) ? 3 : (chance < 0.35 ? 2 : 1);
                                generateVein(chunk, x, y, z, veinSize, random);
                            }
                        }
                    }

                    biomes.setBiome(x, y, z, Biome.DEEP_DARK);
                }

                // --- DECORACIÓN CAPA -59 (SUELO BASE) ---
                // MODIFICADO: Se ejecuta SIEMPRE, incluso en el spawn 0,0
                // para tapar la bedrock con decoración bonita.
                decorateFloorCape(chunk, x, -59, z, random);

                // Techo superior
                decorateCeiling(chunk, x, 119, z, random);

                // Suavizado borde mundo
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
            if (currentX >= 0 && currentX < 16 && currentZ >= 0 && currentZ < 16 && currentY > -60 && currentY < 120) {
                Material type = chunk.getType(currentX, currentY, currentZ);
                if (type != Material.AIR && type != Material.BEDROCK) {
                    chunk.setBlock(currentX, currentY, currentZ, Material.RED_GLAZED_TERRACOTTA);
                }
            }
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