package CorruptedEnd;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CorruptedEndGenerator extends ChunkGenerator {
    private final JavaPlugin plugin;
    private CorruptedEndBiomeProvider biomeProvider;
    private static final int BASE_HEIGHT = 100;
    private final SplittableRandom random = new SplittableRandom();

    private SimplexOctaveGenerator regionGenerator;
    private SimplexOctaveGenerator islandShapeGenerator;
    private SimplexOctaveGenerator heightGenerator;
    private SimplexOctaveGenerator caveGenerator;
    private SimplexOctaveGenerator hillGenerator;

    public CorruptedEndGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initializeGenerators(long seed) {
        if (islandShapeGenerator != null) return;
        Random seedRandom = new Random(seed);

        regionGenerator = new SimplexOctaveGenerator(seedRandom, 2);
        regionGenerator.setScale(0.0015D);

        islandShapeGenerator = new SimplexOctaveGenerator(seedRandom, 8);
        islandShapeGenerator.setScale(0.012D);

        heightGenerator = new SimplexOctaveGenerator(seedRandom, 6);
        heightGenerator.setScale(0.01D);

        caveGenerator = new SimplexOctaveGenerator(seedRandom, 4);
        caveGenerator.setScale(0.05D);

        hillGenerator = new SimplexOctaveGenerator(seedRandom, 2);
        hillGenerator.setScale(0.008D);
    }

    @Override
    public ChunkData generateChunkData(World world, Random cRandom, int chunkX, int chunkZ, BiomeGrid biomes) {
        initializeGenerators(world.getSeed());
        ChunkData chunk = createChunkData(world);

        BiomeInfo centerInfo = getBiomeInfo(chunkX * 16 + 8, chunkZ * 16 + 8);
        /*setCustomBiome(world, biomes, centerInfo != null ? centerInfo.type : null);*/

        if (centerInfo == null && isDeepVoid(chunkX * 16 + 8, chunkZ * 16 + 8)) return chunk;

        for (int X = 0; X < 16; X++) {
            for (int Z = 0; Z < 16; Z++) {
                int globalX = chunkX * 16 + X;
                int globalZ = chunkZ * 16 + Z;

                BiomeInfo info = getBiomeInfo(globalX, globalZ);
                if (info == null) continue;

                double islandNoise = islandShapeGenerator.noise(globalX, globalZ, 0.5D, 0.5D);
                double edgeFactor = info.edgeFactor;
                double threshold = -0.2 + (1.0 - edgeFactor) * 0.8;

                if (islandNoise < threshold) continue;

                int baseY = BASE_HEIGHT;
                int thickness = 0;
                double heightVar = heightGenerator.noise(globalX, globalZ, 0.5D, 0.5D);
                double fadeMultiplier = Math.max(0.2, edgeFactor);

                switch (info.type) {
                    case CELESTIAL_FOREST:
                        baseY = BASE_HEIGHT - 15;
                        thickness = (int) (((islandNoise * 25) + 20) * fadeMultiplier);
                        baseY += (int) (heightVar * 15);
                        break;
                    case CRIMSON_WASTES:
                        thickness = (int) (((islandNoise * 15) + 15) * fadeMultiplier);
                        double hillNoise = hillGenerator.noise(globalX, globalZ, 0.5, 0.5);
                        baseY = BASE_HEIGHT + (int)(hillNoise * 18);
                        break;
                    case OBSIDIAN_PEAKS:
                        baseY = BASE_HEIGHT + 8;
                        thickness = (int) (((Math.abs(islandNoise) * 55) + 12) * fadeMultiplier);
                        baseY += (int) (Math.abs(heightVar) * 40);
                        break;
                    default:
                        thickness = (int) (22 * fadeMultiplier);
                        baseY += (int) (heightVar * 10);
                        break;
                }

                if (thickness <= 2) continue;

                generateIslandTerrain(chunk, X, Z, baseY, thickness, info.type, caveGenerator, globalX, globalZ);
                handleSurfaceFeatures(chunk, X, Z, baseY, thickness, info.type);
            }
        }
        return chunk;
    }

    @NotNull
    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        if (biomeProvider == null) {
            biomeProvider = new CorruptedEndBiomeProvider(this);
        }
        return biomeProvider;
    }

    public CorruptedEndBiomeProvider getBiomeProvider() {
        if (biomeProvider == null) {
            biomeProvider = new CorruptedEndBiomeProvider(this);
        }
        return biomeProvider;
    }

    public static class BiomeInfo {
        public BiomeType type;
        public double edgeFactor;
        public BiomeInfo(BiomeType type, double edgeFactor) { this.type = type; this.edgeFactor = edgeFactor; }
    }

    public BiomeInfo getBiomeInfo(int x, int z) {
        double noise = regionGenerator.noise(x, z, 0.5D, 0.5D);
        double fadeSize = 0.15;

        if (noise >= -1.0 && noise < -0.6) return new BiomeInfo(BiomeType.CELESTIAL_FOREST, calculateEdge(Math.min(noise + 1.0, -0.6 - noise), fadeSize));
        if (noise >= -0.5 && noise < -0.1) return new BiomeInfo(BiomeType.SCULK_PLAINS, calculateEdge(Math.min(noise + 0.5, -0.1 - noise), fadeSize));
        if (noise >= 0.1 && noise < 0.5) return new BiomeInfo(BiomeType.CRIMSON_WASTES, calculateEdge(Math.min(noise - 0.1, 0.5 - noise), fadeSize));
        if (noise >= 0.6 && noise <= 1.0) return new BiomeInfo(BiomeType.OBSIDIAN_PEAKS, calculateEdge(Math.min(noise - 0.6, 1.0 - noise), fadeSize));
        return null;
    }

    private double calculateEdge(double distance, double maxFade) {
        return distance >= maxFade ? 1.0 : distance / maxFade;
    }

    private boolean isDeepVoid(int x, int z) {
        double noise = regionGenerator.noise(x, z, 0.5D, 0.5D);
        return !((noise >= -1.0 && noise < -0.6) || (noise >= -0.5 && noise < -0.1) ||
                (noise >= 0.1 && noise < 0.5) || (noise >= 0.6 && noise <= 1.0));
    }

/*    private void setCustomBiome(World world, BiomeGrid biomes, BiomeType biomeType) {
        NamespacedKey key = (biomeType == null) ?
                NamespacedKey.minecraft("the_end") :
                new NamespacedKey("corrupted_end", biomeType.name().toLowerCase());

        Biome customBiome = null;
        try {
            // Se usa la clase completa de Paper para no romper los Imports de Java
            org.bukkit.Registry<Biome> biomeRegistry = io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.BIOME);
            customBiome = biomeRegistry.get(key);
        } catch (Throwable e) {
            customBiome = Registry.BIOME.get(key); // Fallback clásico
        }

        if (customBiome == null) {
            plugin.getLogger().warning("[CorruptedEnd] ¡Datapack no detectado para el bioma " + key.getKey() + "!");
            customBiome = Registry.BIOME.get(NamespacedKey.minecraft("the_end"));
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 4) {
                    biomes.setBiome(x, y, z, customBiome);
                }
            }
        }
    }*/

    private void handleSurfaceFeatures(ChunkData chunk, int x, int z, int baseY, int thickness, BiomeType biomeType) {
        if (thickness > 5 && random.nextInt(8) == 0) {
            int surfaceY = findSurfaceY(chunk, x, z, baseY, thickness);
            if (surfaceY > 0) generateSurfaceDecoration(chunk, x, z, surfaceY + 1, biomeType);
        }
    }

    private void generateIslandTerrain(ChunkData chunk, int x, int z, int baseY, int thickness,
                                       BiomeType biomeType, SimplexOctaveGenerator caveGenerator,
                                       int globalX, int globalZ) {
        Material primaryBlock = biomeType.getPrimaryBlock();
        Material secondaryBlock = biomeType.getSecondaryBlock();
        Material accentBlock = biomeType.getAccentBlock();

        for (int i = 0; i < thickness / 3; i++) {
            int y = baseY + i;
            if (y >= 0 && y < 319) {
                if (caveGenerator.noise(globalX, y, globalZ, 0.5D, 0.5D) > 0.6) continue;
                chunk.setBlock(x, y, z, getLayerMaterial(i, thickness / 3, primaryBlock, secondaryBlock, accentBlock));
            }
        }
        for (int i = 1; i <= thickness; i++) {
            int y = baseY - i;
            if (y >= 0 && y < 319) {
                if (caveGenerator.noise(globalX, y, globalZ, 0.5D, 0.5D) > 0.6) continue;
                chunk.setBlock(x, y, z, getLayerMaterial(i, thickness, primaryBlock, secondaryBlock, accentBlock));

                if (biomeType == BiomeType.OBSIDIAN_PEAKS && i > 5 && random.nextInt(400) == 0) {
                    generateOreCluster(chunk, x, y, z, Material.BLACK_GLAZED_TERRACOTTA, 6 + random.nextInt(6));
                }
            }
        }
    }

    private void generateOreCluster(ChunkData chunk, int startX, int startY, int startZ, Material mat, int size) {
        for(int i = 0; i < size; i++) {
            int dx = startX + random.nextInt(5) - 2;
            int dy = startY + random.nextInt(5) - 2;
            int dz = startZ + random.nextInt(5) - 2;
            if (dx >= 0 && dx < 16 && dz >= 0 && dz < 16 && dy >= 0 && dy < 319) {
                if (chunk.getType(dx, dy, dz) == Material.OBSIDIAN) {
                    chunk.setBlock(dx, dy, dz, mat);
                }
            }
        }
    }

    private Material getLayerMaterial(int layer, int maxLayers, Material primary, Material secondary, Material accent) {
        double ratio = (double) layer / maxLayers;
        double randomValue = random.nextDouble();
        if (ratio < 0.2 && randomValue < 0.3) return accent;
        else if (randomValue < 0.7) return primary;
        else return secondary;
    }

    private void generateSurfaceDecoration(ChunkData chunk, int x, int z, int y, BiomeType biomeType) {
        Material[] decorations = biomeType.getDecorationBlocks();
        Material decoration;

        if (random.nextInt(100) < 60) {
            decoration = random.nextBoolean() ? Material.SCULK_SENSOR : Material.SCULK_SHRIEKER;
        } else {
            decoration = decorations[random.nextInt(decorations.length)];
        }

        if (y >= 0 && y < 319) {
            if (decoration == Material.SCULK_SHRIEKER) {
                chunk.setBlock(x, y, z, Bukkit.createBlockData(Material.SCULK_SHRIEKER, "[can_summon=true]"));
                if (random.nextInt(3) == 0) chunk.setBlock(x, y - 1, z, Material.SCULK_CATALYST);
            } else {
                chunk.setBlock(x, y, z, decoration);
            }
        }
    }

    private int findSurfaceY(ChunkData chunk, int x, int z, int baseY, int thickness) {
        for (int y = Math.min(319, baseY + thickness + 10); y >= Math.max(0, baseY - thickness - 10); y--) {
            if (chunk.getType(x, y, z) != Material.AIR && chunk.getType(x, y, z) != Material.POWDER_SNOW) return y;
        }
        return -1;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    @NotNull
    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.singletonList(new CorruptedTreePopulator());
    }
}